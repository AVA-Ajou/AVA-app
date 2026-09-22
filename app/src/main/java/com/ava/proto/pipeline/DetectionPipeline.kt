package com.ava.proto.pipeline

import android.util.Log
import com.ava.proto.capture.CapturedEvent
import com.ava.proto.classification.ClassificationClient
import com.ava.proto.classification.ClassificationVerdict
import com.ava.proto.data.EventDao
import com.ava.proto.data.EventEntity
import com.ava.proto.data.EventStatus
import com.ava.proto.data.RiskSignal
import com.ava.proto.session.SessionEngine
import com.ava.proto.session.WINDOW_MILLIS
import com.ava.proto.capture.Channel

private const val TAG = "DetectionPipeline"

/**
 * 캡처 -> 분류 -> 세션 융합을 잇는 유일한 조립 지점.
 *
 * 캡처 계층(NotificationCaptureService, RecordingScanWorker)은 이 클래스만 알면 되고,
 * 분류를 누가 하는지(모델 서버인지 키워드 대역인지)는 몰라도 된다. 반대로 분류·세션
 * 로직도 캡처 방식(알림 리스너인지 SAF 폴더 스캔인지)을 몰라도 된다 — 각 계층이 서로를
 * 모르게 유지하려고 이 조립 지점을 따로 뒀다.
 */
class DetectionPipeline(
    private val classificationClient: ClassificationClient,
    private val sessionEngine: SessionEngine,
    private val eventDao: EventDao,
) {
    suspend fun process(captured: CapturedEvent): EventEntity {
        val (status, verdict) = resolveVerdict(captured)

        // 같은 파일을 다시 분석하는 경우 기존 행을 이어받는다. 새 행을 넣으면 재분석할 때마다
        // 이벤트가 쌓여 탐지율 같은 숫자가 부풀려진다. 세션은 그대로 두고 판정만 갱신한다.
        val existing = eventDao.findBySource(captured.channel, captured.sourceLabel)

        val event = EventEntity(
            id = existing?.id ?: 0,
            sessionId = existing?.sessionId,
            channel = captured.channel,
            capturedAt = captured.capturedAt,
            sourceLabel = captured.sourceLabel,
            text = captured.text,
            status = status,
            riskSignal = verdict.riskSignal,
            matchedPhrase = verdict.matchedPhrase,
            audioUri = captured.audioUri,
            counterpart = captured.counterpart,
            // 모델이 준 값은 그대로 싣는다. 등급으로 접기 전 원본이라 화면에 보여줄 수 있다.
            risk = verdict.risk,
            stage = verdict.stage,
            stageLabel = verdict.stageLabel,
        )

        val saved = sessionEngine.ingest(event)
        rescoreWithContext(saved)
        return saved
    }

    /**
     * 세션 재판정 — 같은 창의 다른 채널 조각이 있으면 이어 붙여 한 번 더 묻는다.
     *
     * 조각별 판정(위)은 그대로다. 이 단계는 그 위에 얹는 두 번째 질문이고, 실패해도 조각 판정과
     * 세션은 이미 저장돼 있으므로 조용히 넘어간다. 결합 텍스트의 형식(`[문자] …\n[통화] …`)은
     * 문자 어댑터 학습셋의 결합 표본과 같다 — `Voice-Detection/src/build_sms_set.py`. 카카오톡도
     * `[문자]`로 적는다. 학습셋에 그 표기만 있다.
     *
     * 문자 어댑터(`task=sms`)로 묻는 이유는 결합 입력을 학습한 쪽이 그쪽이기 때문이다.
     * [Channel.SMS]를 넘기는 것이 곧 그 어댑터를 고르는 것이다.
     */
    private suspend fun rescoreWithContext(event: EventEntity) {
        val text = event.text ?: return
        val others = eventDao.findOtherChannelsBetween(
            event.channel, event.capturedAt - WINDOW_MILLIS, event.capturedAt + WINDOW_MILLIS,
        ).filter { it.id != event.id }
        if (others.isEmpty()) return

        val pieces = (others + event).sortedBy { it.capturedAt }
        val joined = pieces.joinToString("\n") { piece ->
            val tag = if (piece.channel == Channel.CALL) "통화" else "문자"
            "[$tag] ${piece.text}"
        }
        val verdict = try {
            classificationClient.classify(joined, Channel.SMS)
        } catch (e: Exception) {
            Log.e(TAG, "세션 재판정 실패 — 조각 판정은 그대로 둔다", e)
            return
        }
        val fusedRisk = verdict.risk ?: when (verdict.riskSignal) {
            // 키워드 대역은 위험도를 주지 않는다. 등급만으로 문턱 위·아래를 정한다.
            RiskSignal.HIGH, RiskSignal.HIGH_UNBACKED -> 100.0
            else -> 0.0
        }
        val fused = sessionEngine.fuse(pieces, fusedRisk)
        Log.i(TAG, "세션 재판정 ${pieces.size}조각 → ${"%.1f".format(fusedRisk)} ${if (fused) "격상" else "유지"}")
    }

    private suspend fun resolveVerdict(captured: CapturedEvent): Pair<EventStatus, ClassificationVerdict> {
        val text = captured.text
            // 분석할 텍스트가 없으면 분류를 시도하지 않고 그대로 기록만 한다.
            ?: return EventStatus.PENDING_TRANSCRIPTION to ClassificationVerdict(RiskSignal.NONE, null)

        // 텍스트가 있는 이벤트는 내용과 무관하게 예외 없이 분류를 거친다 — 게이트 없음.
        return try {
            EventStatus.ANALYZED to classificationClient.classify(text, captured.channel)
        } catch (e: Exception) {
            // 분류 실패를 RiskSignal.NONE으로만 남기면 "무해하다고 확인됨"과 구분이 안 된다.
            // CLASSIFICATION_FAILED로 명시해서, 게이트 시절의 "NONE = 분석 안 됨" 모호함이
            // 되돌아오지 않게 한다. 이벤트 자체는 여기서 유실시키지 않고 기록은 남긴다.
            Log.e(TAG, "분류 실패, CLASSIFICATION_FAILED로 기록", e)
            EventStatus.CLASSIFICATION_FAILED to ClassificationVerdict(RiskSignal.NONE, null)
        }
    }
}

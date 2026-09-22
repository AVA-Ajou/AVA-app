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
        //
        // **통화에만 해당한다.** 통화의 sourceLabel 은 파일명이라 "같은 것"을 뜻하지만, 알림의
        // sourceLabel 은 패키지명이라 카카오톡 메시지가 전부 같은 값이다. 채널을 가리지 않고
        // 갱신하던 때는 새 문자가 올 때마다 직전 문자 행을 덮어쓰고 그 sessionId 까지 물려받아,
        // 정상 문자가 직전 피싱 문자의 사기 세션에 앉았다 — 순환 테스트에서 정상이 `다채널`로
        // 보인 원인이 이것이었다. 기록에도 카톡이 늘 1건만 남았다.
        val existing = if (captured.channel == Channel.CALL) {
            eventDao.findBySource(captured.channel, captured.sourceLabel)
        } else null

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
     * 세션 재판정 — **혼자서는 문턱을 못 넘은 조각들만** 이어 붙여 한 번 더 묻는다.
     *
     * 처음에는 같은 창의 다른 채널 조각을 전부 이어 붙였다. 그러자 순환 테스트에서 정상 문자가
     * 앞서 온 피싱 카톡과 결합돼 높은 점수를 받고 그 사기 세션에 끌려 들어갔다 — 결합 점수가
     * 높았던 이유는 정상 문자가 아니라 카톡이었는데, 그것을 "합쳐 보니 사기"로 읽은 것이다.
     *
     * 그래서 조건을 셋으로 좁힌다.
     *   1. 이번 조각이 단독으로 이미 경보·경보우려면 재판정하지 않는다. 그 격상은 [SessionEngine.ingest]
     *      가 한다.
     *   2. 상대 조각도 단독으로 경보·경보우려가 아닌 것만 고른다. 경보 조각과 이으면 결합 점수는
     *      그 조각의 점수를 되풀이할 뿐이라 새 정보가 없다.
     *   3. 결합 점수가 경보 문턱(80)을 넘고, 조각 최댓값보다 [FUSE_GAIN] 이상 높아야 한다.
     *      조각 하나가 이미 70 인데 결합이 85 면 그 15 는 결합의 공이 아니다.
     *
     * 이 조건에서 남는 것은 "각각은 예보 이하인데 이어 보면 경보"뿐이다. 그것이 이 단계가
     * 존재하는 유일한 이유다. 결합 텍스트 형식(`[문자] …\n[통화] …`)은 문자 어댑터 학습셋의
     * 결합 표본과 같다 — `Voice-Detection/src/build_sms_set.py`. 카카오톡도 `[문자]`로 적는다.
     */
    private suspend fun rescoreWithContext(event: EventEntity) {
        val text = event.text ?: return
        // 판정에 실패한 조각은 재료가 아니다 — 모델이 못 본 것을 결합에서 다시 보게 할 이유가 없다.
        if (event.status != EventStatus.ANALYZED || event.riskSignal.isConfident()) return
        val others = eventDao.findOtherChannelsBetween(
            event.channel, event.capturedAt - WINDOW_MILLIS, event.capturedAt + WINDOW_MILLIS,
        ).filter { it.id != event.id && !it.riskSignal.isConfident() && it.status == EventStatus.ANALYZED }
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
        // 키워드 대역은 위험도를 주지 않는다. 결합 판정은 모델이 있을 때만 뜻이 있으므로 건너뛴다.
        val fusedRisk = verdict.risk ?: return
        val best = pieces.maxOf { it.risk ?: 0.0 }
        val fused = fusedRisk >= FUSE_THRESHOLD && fusedRisk - best >= FUSE_GAIN &&
            sessionEngine.fuse(pieces, fusedRisk)
        Log.i(TAG, "세션 재판정 ${pieces.size}조각 (최대 ${"%.1f".format(best)}) → " +
            "${"%.1f".format(fusedRisk)} ${if (fused) "격상" else "유지"}")
    }

    /** 단독으로 이미 알림·세션을 만드는 등급. 이들은 재판정의 재료가 아니라 결과다. */
    private fun RiskSignal.isConfident() = this == RiskSignal.HIGH || this == RiskSignal.HIGH_UNBACKED

    private companion object {
        /** 앱의 경보 문턱과 같다. 결합으로 격상시키려면 그 자체가 경보 수준이어야 한다. */
        const val FUSE_THRESHOLD = 80.0
        /** 조각 최댓값 대비 결합이 이만큼은 올라야 "이어 봤기 때문"이라고 말할 수 있다. */
        const val FUSE_GAIN = 30.0
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

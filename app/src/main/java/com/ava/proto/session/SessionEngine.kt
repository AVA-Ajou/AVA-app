package com.ava.proto.session

import com.ava.proto.capture.Channel
import com.ava.proto.data.EventDao
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal
import com.ava.proto.data.SessionDao
import com.ava.proto.data.SessionEntity
import com.ava.proto.data.SessionState
import com.ava.proto.notification.AlertNotifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val WINDOW_MILLIS = 10 * 60 * 1000L // 10분

/**
 * 다채널 세션 융합의 핵심. 채널 하나에서 위험 신호가 잡히면 그 자체로 이미 사용자에게
 * 알릴 만큼 위험하다고 보고 SUSPECTED로 시작하며 즉시 알림을 띄운다. 같은 시간 창 안에
 * *다른* 채널에서도 신호가 잡히면 ESCALATED로 격상시켜 더 급한 알림을 띄운다.
 *
 * 즉 다채널 여부는 "알릴지 말지"를 정하는 문이 아니라 "얼마나 급하게 알릴지"를 정하는
 * 강도 조절 장치다. 통화 하나만으로 끝나는 보이스피싱도(STT가 붙으면) 이 로직에서
 * 놓치지 않는다.
 *
 * 시간 창은 이벤트가 실제로 일어난 시각([EventEntity.capturedAt])을 기준으로 계산한다 —
 * 처리 시각(now())을 쓰면 최대 15분 지연되는 통화 채널이 실제로는 가까운 시각에 있었던
 * 다른 채널과 엮이지 못하거나, 반대로 무관한 최근 이벤트와 잘못 엮일 수 있다.
 *
 * 여러 캡처 지점(NotificationCaptureService의 코루틴들, RecordingScanWorker)이 동시에
 * ingest()를 부를 수 있어서, "활성 세션 조회 → 갱신/생성"을 [mutex]로 직렬화한다 —
 * 이게 없으면 두 이벤트가 거의 동시에 들어왔을 때 각자 "활성 세션 없음"으로 보고
 * 세션을 두 개로 쪼개버려 정작 격상이 일어나지 않을 수 있다.
 */
class SessionEngine(
    private val eventDao: EventDao,
    private val sessionDao: SessionDao,
    private val alertNotifier: AlertNotifier,
    private val windowMillis: Long = WINDOW_MILLIS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun ingest(event: EventEntity): EventEntity {
        // 신호가 없는 이벤트(예: STT 대기 중인 통화 녹음, 분류 실패, 또는 실제로 무해하다고
        // 분류된 이벤트)는 기록만 하고 세션에는 영향을 주지 않는다.
        //
        // **[RiskSignal.NONE]만 걸러낸다.** 등급 이름이 붙은 것은 예보까지 전부 알린다 —
        // 예보는 판정기 어느 쪽도 위험하다고 하지 않은 구간이라 실측에서 9건 중 6건이
        // 정상이었지만, 나머지 3건이 진짜 피싱이었다. **미탐은 돈이 나가고 오탐은 짜증에
        // 그친다**는 이 프로젝트의 기준을 이 문턱에도 그대로 적용한 것이다.
        if (event.riskSignal == RiskSignal.NONE) {
            save(event)
            return event
        }

        return mutex.withLock { fuseIntoSession(event) }
    }

    /**
     * [EventEntity.id]가 있으면 그 행을 갱신하고, 없으면 새로 넣는다.
     *
     * 같은 통화 파일을 다시 분석할 때 행이 쌓이지 않게 하려는 것이다 — 쌓이면 탐지율·오탐률
     * 같은 숫자가 전부 부풀려진다. 기존 행을 찾는 일은 [com.ava.proto.pipeline.DetectionPipeline]이
     * 하고, 여기서는 id 유무만 본다.
     */
    private suspend fun save(event: EventEntity) {
        if (event.id != 0L) eventDao.update(event) else eventDao.insert(event)
    }

    private suspend fun fuseIntoSession(event: EventEntity): EventEntity {
        val referenceTime = event.capturedAt
        val processedAt = now()

        val existing = sessionDao.findActive(referenceTime, event.counterpart)
        val previousState = existing?.state
        val session = existing ?: SessionEntity(
            state = SessionState.SUSPECTED,
            createdAt = processedAt,
            updatedAt = processedAt,
            windowExpiresAt = referenceTime + windowMillis,
            channelsInvolved = emptySet(),
        )

        val channels = session.channelsInvolved.toMutableSet()
        val isNewChannel = channels.add(event.channel)

        // 하향 전이는 없다 — 한 번 격상된 세션은 이후 이벤트로 SUSPECTED로 되돌아가지 않는다.
        val newState =
            if (isNewChannel && channels.size >= 2) SessionState.ESCALATED else session.state

        val updatedSession = session.copy(
            state = newState,
            updatedAt = processedAt,
            // 세션이 계속 활동 중이라는 뜻이므로 창을 늘린다. 다만 이미 더 늦은 이벤트로
            // 늘어나 있는 창을, 뒤늦게 처리된 과거 이벤트(지연된 통화 등)가 되레 줄이면 안 된다.
            windowExpiresAt = maxOf(session.windowExpiresAt, referenceTime + windowMillis),
            channelsInvolved = channels,
            // 세션이 아직 특정 상대로 안 좁혀져 있었다면(통화만 있었다면) 이번에 식별자가
            // 있는 이벤트(SMS/카톡)가 들어온 순간 그 상대로 좁힌다.
            counterpart = session.counterpart ?: event.counterpart,
        )

        val sessionId = if (existing == null) {
            sessionDao.insert(updatedSession)
        } else {
            sessionDao.update(updatedSession)
            existing.id
        }

        val savedEvent = event.copy(sessionId = sessionId)
        save(savedEvent)

        val finalSession = updatedSession.copy(id = sessionId)
        // 알림 문구에 그대로 박히는 값이라 `name`(KAKAO)이 아니라 `label`(카카오톡)을 쓴다 —
        // 사용자가 읽는 자리에 내부 식별자가 나갈 이유가 없다. 저장은 여전히 `name`이다.
        val channelNames = channels.map(Channel::label).toSet()
        when {
            previousState == null ->
                // 이 세션에서 처음 잡힌 신호 — 채널이 하나뿐이어도 사용자에게 알려야 한다.
                alertNotifier.notifySuspected(finalSession, channelNames)

            newState == SessionState.ESCALATED && previousState != SessionState.ESCALATED ->
                alertNotifier.notifyEscalation(finalSession, channelNames)
        }

        return savedEvent
    }
}

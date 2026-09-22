package com.ava.proto.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ava.proto.capture.Channel

/**
 * 두 값뿐이고 하향 전이가 없다.
 *
 * 예전에는 "백엔드 확정" 자리로 `ALERT`를 하나 더 뒀는데, 그 확정이라는 것이 결국 모델
 * 판정이고 그건 이미 `SUSPECTED`를 만든 근거였다. 세션에 도달할 경로가 없는 값이 상태
 * 전이 코드에 분기를 하나 더 만들고 있어 걷어냈다.
 */
enum class SessionState {
    /** 한 채널에서 위험 신호가 잡혀 시작된 상태. */
    SUSPECTED,

    /** 같은 시간 창 안에 다른 채널에서도 신호가 잡혀 격상된 상태. */
    ESCALATED,
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val state: SessionState,
    val createdAt: Long,
    val updatedAt: Long,
    /** 이 시각 이후로는 새 이벤트가 이 세션에 합류하지 못하고 새 세션이 시작된다. */
    val windowExpiresAt: Long,
    /**
     * 세션에 든 이벤트 중 가장 이른 발생 시각([EventEntity.capturedAt]).
     *
     * [windowExpiresAt] 하나만 있으면 창의 **끝**만 알 수 있어서, 뒤늦게 처리된 과거 이벤트가
     * 훨씬 나중에 열린 세션에 합류했다 — 8월 17일 통화 전사본을 9월 22일에 재분석하자
     * 그날 카카오톡이 연 세션에 붙어 "다채널"로 격상됐다. 합류 조건은 양쪽으로 잰다:
     * 이벤트가 창이 끝나기 전이어야 하고, 이벤트의 창이 세션의 첫 이벤트에 닿아야 한다.
     */
    val firstCapturedAt: Long,
    /**
     * 조각을 이어 붙여 다시 물은 위험도. 세션 재판정이 격상시킨 경우에만 채워진다.
     *
     * 조각 각각의 [EventEntity.risk]와 다른 값이다 — 조각은 정상(0.4)이어도 결합은 92 가
     * 나온다. 이 값이 있으면 "단독으로는 정상이었지만 합쳐 보니 사기"라는 뜻이다.
     */
    val fusedRisk: Double? = null,
    /** [com.ava.proto.data.Converters]가 실제 저장 형식(문자열)과 자동 변환한다 — 호출부는 그냥 Set으로 다룬다. */
    val channelsInvolved: Set<Channel>,
    /**
     * 이 세션이 묶여 있는 상대방 식별자. SMS/카톡처럼 식별자가 있는 이벤트가 최초로 세션을
     * 만들면 채워지고, 통화처럼 식별자가 없는 이벤트만 있었다면 null로 남는다.
     * null이면 "아직 특정 상대로 좁혀지지 않음"이라는 뜻이라 어느 이벤트든 합류할 수 있다.
     */
    val counterpart: String? = null,
)

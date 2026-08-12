package com.ava.proto.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ava.proto.capture.Channel

enum class SessionState {
    /** 한 채널에서 위험 신호가 잡혀 시작된 상태. */
    SUSPECTED,

    /** 같은 시간 창 안에 다른 채널에서도 신호가 잡혀 격상된 상태. */
    ESCALATED,

    /** 자리만 잡아둔 값. 현재 코드 경로로는 도달하지 않는다. */
    ALERT,
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val state: SessionState,
    val createdAt: Long,
    val updatedAt: Long,
    /** 이 시각 이후로는 새 이벤트가 이 세션에 합류하지 못하고 새 세션이 시작된다. */
    val windowExpiresAt: Long,
    /** [com.ava.proto.data.Converters]가 실제 저장 형식(문자열)과 자동 변환한다 — 호출부는 그냥 Set으로 다룬다. */
    val channelsInvolved: Set<Channel>,
    /**
     * 이 세션이 묶여 있는 상대방 식별자. SMS/카톡처럼 식별자가 있는 이벤트가 최초로 세션을
     * 만들면 채워지고, 통화처럼 식별자가 없는 이벤트만 있었다면 null로 남는다.
     * null이면 "아직 특정 상대로 좁혀지지 않음"이라는 뜻이라 어느 이벤트든 합류할 수 있다.
     */
    val counterpart: String? = null,
)

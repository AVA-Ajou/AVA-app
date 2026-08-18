package com.ava.proto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 세션은 화면에 목록으로 나오지 않는다 — 사용자가 읽는 것은 이벤트 카드와 시스템 알림이고,
 * 세션은 그 뒤에서 다채널 격상을 판단하는 상태일 뿐이다. 그래서 `observe…` 류 쿼리가 없다.
 */
@Dao
interface SessionDao {
    /**
     * 활성 세션 조회. 조건은 **시간 창 하나뿐이다.**
     *
     * counterpart(발신자) 일치 조건을 쓰지 않는 이유 — 다채널 공격은 카톡·SMS·통화가
     * 서로 다른 이름·번호로 오는 경우가 많다. 여기에 도달한 이벤트는 이미 NONE이 아닌
     * RiskSignal을 가지고 있으므로(SessionEngine이 NONE을 걸러낸다), 10분 창 안에
     * 위험 신호가 겹쳤다는 사실 자체가 연관의 근거로 충분하다.
     */
    @Query(
        """
        SELECT * FROM sessions
        WHERE windowExpiresAt > :referenceTime
        ORDER BY updatedAt DESC LIMIT 1
        """,
    )
    suspend fun findActive(referenceTime: Long): SessionEntity?

    /** ESCALATED 세션의 id 목록을 실시간으로 흘린다. UI의 [다채널 탐지] 태그에 쓴다. */
    @Query("SELECT id FROM sessions WHERE state = 'ESCALATED'")
    fun observeEscalatedIds(): Flow<List<Long>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)
}

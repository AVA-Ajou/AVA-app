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
     * counterpart가 null이면(발신자 식별 불가 채널, 즉 통화) 아무 활성 세션에나 합류할 수 있다.
     * counterpart가 있으면 상대가 같거나(세션도 그 상대로 좁혀져 있거나) 세션이 아직 특정
     * 상대로 안 좁혀진(counterpart IS NULL) 경우에만 합류한다 — 무관한 두 사람이 시간만
     * 겹쳤다고 한 세션으로 섞이는 걸 막는다.
     */
    @Query(
        """
        SELECT * FROM sessions
        WHERE windowExpiresAt > :referenceTime
          AND (:counterpart IS NULL OR counterpart IS NULL OR counterpart = :counterpart)
        ORDER BY updatedAt DESC LIMIT 1
        """,
    )
    suspend fun findActive(referenceTime: Long, counterpart: String?): SessionEntity?

    /** ESCALATED 세션의 id 목록을 실시간으로 흘린다. UI의 [다채널 탐지] 태그에 쓴다. */
    @Query("SELECT id FROM sessions WHERE state = 'ESCALATED'")
    fun observeEscalatedIds(): Flow<List<Long>>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)
}

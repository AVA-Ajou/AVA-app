package com.ava.proto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

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

    /**
     * counterpart를 보지 않고 활성 세션만 찾는다. **[findActive]가 빈손일 때만** 쓴다.
     *
     * 위 조건은 같은 채널 안에서는 옳지만 채널을 건너뛰면 성립하지 않는다 — 카카오톡의
     * counterpart는 대화방 이름("금융감독원")이고 문자의 counterpart는 전화번호
     * ("010-9999-0000")라, 같은 사기범이 같은 번호로 보내도 **두 문자열이 같아질 경로가 없다.**
     * 그래서 카톡+문자 조합은 구조적으로 격상되지 않았고, 통화(counterpart가 null)가 낀
     * 조합만 격상됐다.
     *
     * 합류 여부는 [com.ava.proto.session.SessionEngine]이 "이번 채널이 세션에 아직 없는가"로
     * 한 번 더 거른다. 식별자를 비교할 수 없는 자리에서 억지로 비교하는 대신, **채널이
     * 다르다는 사실 자체**를 합류 근거로 쓰는 것이다.
     */
    @Query(
        """
        SELECT * FROM sessions
        WHERE windowExpiresAt > :referenceTime
        ORDER BY updatedAt DESC LIMIT 1
        """,
    )
    suspend fun findActiveAnyCounterpart(referenceTime: Long): SessionEntity?

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)
}

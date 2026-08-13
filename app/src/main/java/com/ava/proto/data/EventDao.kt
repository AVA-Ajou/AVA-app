package com.ava.proto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ava.proto.capture.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    /**
     * 같은 파일을 다시 분석할 때 쓴다. 없으면 새로 넣고, 있으면 그 행을 갱신한다 —
     * 재분석할 때마다 행이 쌓이면 탐지율 같은 숫자가 전부 부풀려진다.
     */
    @Query("SELECT * FROM events WHERE channel = :channel AND sourceLabel = :sourceLabel LIMIT 1")
    suspend fun findBySource(channel: Channel, sourceLabel: String): EventEntity?

    @Query("SELECT * FROM events ORDER BY capturedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<EventEntity>>

    /**
     * 통화 녹음 폴더 스캔에서 "이미 분석이 끝난 파일"을 걸러내는 데 쓴다. 타임스탬프 워터마크
     * 대신 파일명 자체로 판단한다 — mtime 해상도가 낮은 저장소에서 두 파일이 완전히
     * 같은 수정 시각을 가지면 타임스탬프 비교로는 뒤 파일이 영구히 스킵되기 때문이다.
     *
     * **[EventStatus.PENDING_TRANSCRIPTION]은 제외한다.** 그건 "아직 안 끝났다"는 뜻인데
     * 예전에는 상태를 안 보고 파일명만 봐서, STT가 한 번 실패하면 그 통화가 영영 다시
     * 시도되지 않았다. 파일을 쓰는 도중에 잡아 실패한 경우도 마찬가지로 굳어버렸다.
     */
    @Query(
        "SELECT sourceLabel FROM events " +
            "WHERE channel = :channel AND status != 'PENDING_TRANSCRIPTION'",
    )
    suspend fun analyzedSourceLabels(channel: Channel): List<String>
}

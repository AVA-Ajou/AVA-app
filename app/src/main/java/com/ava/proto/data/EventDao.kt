package com.ava.proto.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ava.proto.capture.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events ORDER BY capturedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<EventEntity>>

    /**
     * 통화 녹음 폴더 스캔에서 "이미 기록된 파일"을 걸러내는 데 쓴다. 타임스탬프 워터마크
     * 대신 파일명 자체로 판단한다 — mtime 해상도가 낮은 저장소에서 두 파일이 완전히
     * 같은 수정 시각을 가지면 타임스탬프 비교로는 뒤 파일이 영구히 스킵되기 때문이다.
     */
    @Query("SELECT sourceLabel FROM events WHERE channel = :channel")
    suspend fun sourceLabelsForChannel(channel: Channel): List<String>
}

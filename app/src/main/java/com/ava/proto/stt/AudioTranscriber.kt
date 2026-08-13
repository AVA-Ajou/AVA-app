package com.ava.proto.stt

/**
 * 녹음 파일 → 텍스트 변환 인터페이스.
 *
 * [com.ava.proto.capture.RecordingScanWorker]가 새 통화 녹음을 발견했을 때 호출한다.
 * 구현체가 없거나(서버 주소 미설정) 변환에 실패하면 null을 돌려주고,
 * 파이프라인은 기존처럼 PENDING_TRANSCRIPTION으로 기록만 남긴다.
 */
interface AudioTranscriber {
    /** @return 전사된 텍스트, 실패 또는 미설정이면 null */
    suspend fun transcribe(audioUri: String): String?
}

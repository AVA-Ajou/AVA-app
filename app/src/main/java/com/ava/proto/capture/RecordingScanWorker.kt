package com.ava.proto.capture

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ava.proto.ProtoApplication

private const val TAG = "RecordingScanWorker"

/**
 * 15분마다(WorkManager 주기 작업 최소 간격) 연결된 폴더를 훑어 삼성 통화 녹음 신규 파일을 처리한다.
 *
 * 처리 흐름:
 *  1. [RecordingFolder.findNewFiles]로 "Call recording_YYYYMMDD_HHMMSS.m4a" 패턴의 신규 파일 탐지
 *  2. [com.ava.proto.stt.AudioTranscriber]로 Gemini에 오디오를 보내 한국어 전사(STT)
 *  3. 전사 텍스트(또는 실패 시 null)를 [CapturedEvent.text]에 담아 파이프라인에 넘김
 *  4. 텍스트가 있으면 [com.ava.proto.classification.ClassificationClient]가 피싱 여부 판정
 *     텍스트가 없으면(STT 실패/미설정) PENDING_TRANSCRIPTION으로 기록만 남김
 *
 * 통화는 발신번호를 알 방법이 없어 [CapturedEvent.counterpart]는 항상 null이다.
 */
class RecordingScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ProtoApplication
        val alreadyProcessed = app.database.eventDao().sourceLabelsForChannel(Channel.CALL).toSet()

        RecordingFolder.findNewFiles(applicationContext, alreadyProcessed).forEach { file ->
            // 파일 하나의 처리가 실패해도 다음 15분 실행이 이 파일에서 멈추지 않고 계속 진행한다.
            try {
                val audioUri = file.uri.toString()

                // STT: API 키가 설정돼 있으면 Gemini로 전사, 아니면 null → PENDING_TRANSCRIPTION
                val transcript = app.audioTranscriber?.transcribe(audioUri)
                if (transcript != null) {
                    Log.d(TAG, "전사 완료 (${file.name}): ${transcript.take(80)}…")
                } else {
                    Log.d(TAG, "STT 스킵 또는 실패, PENDING_TRANSCRIPTION으로 기록: ${file.name}")
                }

                app.detectionPipeline.process(
                    CapturedEvent(
                        channel = Channel.CALL,
                        capturedAt = file.lastModified(),
                        sourceLabel = RecordingFolder.identityOf(file),
                        text = transcript,
                        audioUri = audioUri,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "녹음 파일 처리 실패, 다음 파일로 계속 진행: ${file.name}", e)
            }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "recording_scan"
        /** 즉시 실행 요청에 붙이는 태그 — UI가 이 태그로 진행 상태를 관찰한다. */
        const val TAG_IMMEDIATE = "recording_scan_immediate"
    }
}

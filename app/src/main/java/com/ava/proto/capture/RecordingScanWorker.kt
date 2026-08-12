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

        // 강제 재검사 — 이미 분석이 끝난 파일도 다시 판정한다. 시뮬레이션 탭의 버튼이 쓴다.
        // 15분 주기 스캔은 이 값이 꺼져 있어 예전처럼 새 파일만 본다.
        val force = inputData.getBoolean(KEY_FORCE, false)
        val alreadyProcessed = if (force) {
            emptySet()
        } else {
            app.database.eventDao().analyzedSourceLabels(Channel.CALL).toSet()
        }

        RecordingFolder.findNewFiles(applicationContext, alreadyProcessed).forEach { file ->
            // 파일 하나의 처리가 실패해도 다음 15분 실행이 이 파일에서 멈추지 않고 계속 진행한다.
            try {
                // 쓰는 중인 파일을 잡으면 0바이트로 보인다. 이벤트를 만들지 않고 넘긴다 —
                // 만들어두면 그 기록이 남아 완성된 뒤에도 다시 처리되지 못한 적이 있다.
                if (file.length() == 0L) {
                    Log.d(TAG, "빈 파일, 건너뜀 (쓰는 중일 수 있음): ${file.name}")
                    return@forEach
                }

                val audioUri = file.uri.toString()
                val isTranscript = RecordingFolder.isTranscript(file.name)

                // .txt 는 이미 전사된 통화다. STT를 건너뛰고 파일 내용을 그대로 쓴다 —
                // 전사가 끝난 직후 지점부터 실제 경로를 그대로 태우기 위한 통로다.
                val transcript = if (isTranscript) {
                    applicationContext.contentResolver.openInputStream(file.uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                } else {
                    app.audioTranscriber?.transcribe(audioUri)
                }

                when {
                    transcript == null && isTranscript ->
                        Log.w(TAG, "전사본 파일이 비어 있음: ${file.name}")
                    transcript == null ->
                        Log.d(TAG, "STT 스킵 또는 실패, PENDING_TRANSCRIPTION으로 기록: ${file.name}")
                    else ->
                        Log.d(TAG, "${if (isTranscript) "전사본 파일" else "전사 완료"} " +
                            "(${file.name}): ${transcript.take(80)}…")
                }

                app.detectionPipeline.process(
                    CapturedEvent(
                        channel = Channel.CALL,
                        capturedAt = file.lastModified(),
                        sourceLabel = RecordingFolder.identityOf(file),
                        text = transcript,
                        // 전사본 파일은 재생할 오디오가 없다.
                        audioUri = if (isTranscript) null else audioUri,
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
        /** 이미 분석한 파일도 다시 판정할지. 켜면 폴더 전체를 재검사한다. */
        const val KEY_FORCE = "force"
    }
}

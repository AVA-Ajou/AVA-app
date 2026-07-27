package com.ava.proto.capture

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ava.proto.ProtoApplication

private const val TAG = "RecordingScanWorker"

/**
 * 15분마다(WorkManager 주기 작업 최소 간격) 연결된 폴더를 훑어 신규 파일을 이벤트로 기록한다.
 *
 * STT가 아직 연결되지 않아 텍스트가 없다 — [CapturedEvent.text] 를 null로 넘기면
 * [com.ava.proto.pipeline.DetectionPipeline] 가 분류를 시도하지 않고 그대로 기록만 해둔다.
 * 백엔드 STT 연동이 붙으면 이 워커가 오디오를 업로드하고 돌아온 텍스트를 채워 넣는 지점이 된다.
 *
 * 통화는 발신번호를 알 방법이 없어 [CapturedEvent.counterpart] 는 항상 null로 넘긴다.
 */
class RecordingScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ProtoApplication
        val alreadyProcessed = app.database.eventDao().sourceLabelsForChannel(Channel.CALL).toSet()

        RecordingFolder.findNewFiles(applicationContext, alreadyProcessed).forEach { file ->
            // 파일 하나의 처리가 실패해도(예: STT 연동 후 분류 오류) 다음 15분 실행이 이
            // 파일에서 멈추지 않고 그 뒤의 새 파일들을 계속 처리해야 한다.
            try {
                app.detectionPipeline.process(
                    CapturedEvent(
                        channel = Channel.CALL,
                        capturedAt = file.lastModified(),
                        sourceLabel = RecordingFolder.identityOf(file),
                        text = null,
                        audioUri = file.uri.toString(),
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
    }
}

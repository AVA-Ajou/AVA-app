package com.ava.proto.capture

import android.content.Context
import android.os.Build
import android.os.FileObserver
import android.util.Log
import java.io.File

private const val TAG = "CallRecordingWatcher"

/**
 * SAF 폴더를 FileObserver(Linux inotify)로 실시간 감시한다.
 *
 * 삼성 패턴("Call recording_*.m4a")의 파일이 쓰기 완료(CLOSE_WRITE)되거나
 * 이동(MOVED_TO)되는 순간 [RecordingScanWorker]를 즉시 실행한다.
 * 사용자가 "지금 스캔" 같은 별도 액션을 취하지 않아도 된다.
 *
 * FileObserver는 실제 파일시스템 경로가 필요하므로 SAF tree URI를
 * [RecordingFolder.getAsPath]로 변환해 사용한다. SD카드 등 외부 볼륨은 미지원.
 */
class CallRecordingWatcher(private val context: Context) {

    private var observer: FileObserver? = null

    fun restart() {
        observer?.stopWatching()
        observer = null

        val path = RecordingFolder.getAsPath(context) ?: run {
            Log.d(TAG, "폴더 경로 변환 불가, 감시 중단")
            return
        }
        val dir = File(path)
        if (!dir.exists()) {
            Log.w(TAG, "폴더 없음: $path")
            return
        }

        observer = buildObserver(dir).also {
            it.startWatching()
            Log.d(TAG, "녹음 폴더 감시 시작: $path")
        }
    }

    @Suppress("DEPRECATION")
    private fun buildObserver(dir: File): FileObserver {
        val mask = FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(dir, mask) {
                override fun onEvent(event: Int, path: String?) = handleEvent(path)
            }
        } else {
            object : FileObserver(dir.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) = handleEvent(path)
            }
        }
    }

    private fun handleEvent(fileName: String?) {
        // 전사본 텍스트도 받는다 — 실기기 없이 탐지 경로를 태워보려고 열어둔 통로다.
        if (fileName == null || !RecordingFolder.isCallSource(fileName)) return
        Log.d(TAG, "새 통화 파일 감지 → 즉시 스캔: $fileName")
        RecordingScanWorker.enqueueNow(context)
    }
}

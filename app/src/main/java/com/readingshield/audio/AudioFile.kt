package com.readingshield.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.IOException

/** 분석 대상으로 넘어온 오디오 한 건의 메타데이터. */
data class AudioFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
    val mimeType: String?,
    val durationMs: Long?,
)

/**
 * 공유 시트나 파일 선택기로 받은 [uri]를 읽어 메타데이터를 뽑는다.
 *
 * MediaStore 폴링과 달리 파일의 실제 저장 경로나 인덱싱 여부에 의존하지 않는다.
 * 사용자가 넘겨준 URI에는 이미 읽기 권한이 붙어 있으므로 저장소 권한도 필요 없다.
 */
fun Context.readAudioFile(uri: Uri): AudioFile {
    // 먼저 실제로 열리는지 확인한다.
    // 권한 없는 URI 에 대해 query() 는 예외 대신 null 을 돌려주기 때문에,
    // 이 검사가 없으면 "메타데이터를 하나도 못 읽은 파일"이 정상 파일처럼 표시된다.
    contentResolver.openInputStream(uri)?.close()
        ?: throw IOException("파일 스트림을 열 수 없습니다.")

    var displayName: String? = null
    var sizeBytes: Long? = null

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                .takeIf { it >= 0 && !cursor.isNull(it) }
                ?.let { displayName = cursor.getString(it) }

            cursor.getColumnIndex(OpenableColumns.SIZE)
                .takeIf { it >= 0 && !cursor.isNull(it) }
                ?.let { sizeBytes = cursor.getLong(it) }
        }
    }

    return AudioFile(
        uri = uri,
        displayName = displayName ?: uri.lastPathSegment ?: "(이름 없음)",
        sizeBytes = sizeBytes,
        mimeType = contentResolver.getType(uri),
        durationMs = readDurationMs(uri),
    )
}

/** 재생 길이는 컨테이너를 열어봐야 알 수 있고, 손상된 파일이면 실패할 수 있어 null을 허용한다. */
private fun Context.readDurationMs(uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (e: RuntimeException) {
        // setDataSource 는 지원하지 않는 포맷에 대해 IllegalArgumentException 을 던진다.
        null
    } finally {
        // MediaMetadataRetriever 가 AutoCloseable 이 된 건 API 29 부터라 직접 해제한다.
        retriever.release()
    }
}

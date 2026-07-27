package com.readingshield.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

/** 연결된 녹음 폴더에서 발견한 파일 한 건. */
data class FoundRecording(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
)

private const val PREFS = "readingshield"
private const val KEY_TREE_URI = "recording_folder_uri"

/**
 * 사용자가 지정한 녹음 폴더를 영구 권한으로 저장한다.
 *
 * SAF 트리 권한은 MediaStore 색인과 무관하게 실제 파일시스템을 훑기 때문에,
 * 삼성이 통화 녹음을 MediaStore 에 등록하지 않더라도 파일을 찾을 수 있다.
 * takePersistableUriPermission 덕분에 재부팅 후에도 권한이 유지된다.
 */
fun Context.saveRecordingFolder(treeUri: Uri) {
    contentResolver.takePersistableUriPermission(
        treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
        putString(KEY_TREE_URI, treeUri.toString())
    }
}

/** 저장된 폴더 URI. 아직 연결하지 않았으면 null. */
fun Context.recordingFolder(): Uri? =
    getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_TREE_URI, null)
        ?.toUri()

/**
 * 연결된 폴더 안의 파일 목록. 최신 파일이 앞에 온다.
 *
 * 필터링을 하지 않고 전부 돌려주는 이유는, MediaStore 가 오디오로 인식하지 않는 파일까지
 * SAF 로는 보인다는 걸 확인할 수 있어야 하기 때문이다.
 */
fun Context.scanRecordingFolder(): List<FoundRecording> {
    val treeUri = recordingFolder() ?: return emptyList()
    val folder = DocumentFile.fromTreeUri(this, treeUri) ?: return emptyList()

    return folder.listFiles()
        .filter { it.isFile }
        .map {
            FoundRecording(
                uri = it.uri,
                name = it.name ?: "(이름 없음)",
                sizeBytes = it.length(),
                lastModified = it.lastModified(),
                mimeType = it.type,
            )
        }
        .sortedByDescending { it.lastModified }
}

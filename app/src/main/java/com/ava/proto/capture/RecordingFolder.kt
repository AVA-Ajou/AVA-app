package com.ava.proto.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.DocumentFile.fromTreeUri
import android.provider.DocumentsContract

private const val PREFS = "proto_prefs"
private const val KEY_TREE_URI = "recording_folder_uri"

/**
 * 삼성 갤럭시 통화 녹음 파일명 패턴 (One UI 기준).
 *
 * 저장 경로: 내부저장소/Recordings/Call/
 * 파일명 형식: "Call recording_YYYYMMDD_HHMMSS.m4a"
 *   예시: Call recording_20241201_143022.m4a
 *
 * 한국어 기기 설정에서는 "통화 녹음_YYYYMMDD_HHMMSS.m4a" 형태가 나올 수 있어
 * 두 패턴을 모두 허용한다. 구분자는 언더스코어(_) 또는 공백 모두 허용.
 */
private val SAMSUNG_CALL_RECORDING = Regex(
    """^(Call recording|통화[ _]?녹음)[_ ]\d{8}[_ ]\d{6}.*\.m4a$""",
    RegexOption.IGNORE_CASE,
)

/**
 * 사용자가 SAF로 지정한 통화 녹음 폴더를 관리한다.
 *
 * MediaStore 색인 여부와 무관하게 폴더 안 파일을 직접 훑기 때문에, 삼성이 통화 녹음을
 * MediaStore.Audio 에 등록하지 않아도 파일을 찾을 수 있다 (에뮬레이터에서 실측 확인됨).
 * MANAGE_EXTERNAL_STORAGE 나 READ_MEDIA_AUDIO 없이 동작한다.
 */
object RecordingFolder {

    fun save(context: Context, treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_TREE_URI, treeUri.toString())
        }
    }

    fun get(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)
            ?.toUri()

    /**
     * 연결된 폴더에서 [alreadyProcessedNames]에 없는 파일만 오래된 순으로 돌려준다.
     *
     * 수정 시각 비교가 아니라 파일명으로 판단한다 — mtime 해상도가 낮은 저장소(SAF 제공자,
     * 일부 파일시스템)에서는 두 파일이 완전히 같은 수정 시각을 가질 수 있는데, 그 상태에서
     * 타임스탬프 워터마크를 쓰면 먼저 처리된 파일과 시각이 같은 뒤 파일이 "워터마크보다
     * 크지 않다"는 이유로 영구히 스킵된다. 통화 녹음 파일명은 통상 발신번호+시각이 들어가
     * 사실상 유일하므로 이름 기반 판별이 더 안전하다.
     */
    fun findNewFiles(context: Context, alreadyProcessedNames: Set<String>): List<DocumentFile> {
        val treeUri = get(context) ?: return emptyList()
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return folder.listFiles()
            .filter { it.isFile && isSamsungCallRecording(it.name) && identityOf(it) !in alreadyProcessedNames }
            .sortedBy { it.lastModified() }
    }

    /** 파일명이 없는 드문 경우를 대비해 URI를 대신 쓴다 — dedup 판별과 기록에 항상 같은 값을 쓴다. */
    fun identityOf(file: DocumentFile): String = file.name ?: file.uri.toString()

    /** 삼성 갤럭시 통화 녹음 파일명인지 확인한다. null(이름 없음)은 false. */
    fun isSamsungCallRecording(name: String?): Boolean =
        name != null && SAMSUNG_CALL_RECORDING.matches(name)

    /**
     * SAF tree URI를 실제 파일시스템 경로로 변환한다.
     * FileObserver는 URI가 아닌 실제 경로가 필요하므로 여기서 변환한다.
     * primary 볼륨(내부저장소)만 지원하며, SD카드 등 외부 볼륨은 null을 반환한다.
     */
    fun getAsPath(context: Context): String? {
        val treeUri = get(context) ?: return null
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        val parts = docId.split(":")
        if (parts.size != 2) return null
        val (volume, relativePath) = parts
        return if (volume == "primary") "/storage/emulated/0/$relativePath" else null
    }
}

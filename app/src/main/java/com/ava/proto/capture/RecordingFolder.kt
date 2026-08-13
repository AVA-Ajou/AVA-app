package com.ava.proto.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
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
 * 이미 전사된 통화. 파일 내용이 곧 전사본이라 STT를 건너뛴다.
 *
 * 실기기 없이 탐지 경로 전체를 태워보려고 둔 통로다. 음성 파일을 넣으면 서버 전사를 거쳐야
 * 하는데, 그 단계는 지금 확인하려는 부분이 아니고 실제 녹음과 수십 초가 필요하다.
 * `.txt`를 떨어뜨리면 전사가 끝난 직후 지점부터 실제 경로를 그대로 탄다.
 */
private val TRANSCRIPT_FILE = Regex(""".+\.txt$""", RegexOption.IGNORE_CASE)

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
            .filter { it.isFile && isCallSource(it.name) && identityOf(it) !in alreadyProcessedNames }
            .sortedBy { it.lastModified() }
    }

    /** 파일명이 없는 드문 경우를 대비해 URI를 대신 쓴다 — dedup 판별과 기록에 항상 같은 값을 쓴다. */
    fun identityOf(file: DocumentFile): String = file.name ?: file.uri.toString()

    /**
     * 통화 채널로 처리할 파일인가 — 삼성 녹음이거나 이미 전사된 텍스트.
     *
     * `FileObserver` 필터([CallRecordingWatcher])와 폴더 스캔 필터([findNewFiles])가 **이 함수
     * 하나를** 쓴다. 갈라지면 한쪽에만 걸리는 파일이 생겨, 즉시 감지는 되는데 스캔에서는
     * 안 보이는(또는 그 반대) 버그가 난다.
     */
    fun isCallSource(name: String?): Boolean =
        isSamsungCallRecording(name) || isTranscript(name)

    private fun isSamsungCallRecording(name: String?): Boolean =
        name != null && SAMSUNG_CALL_RECORDING.matches(name)

    /** 전사본 텍스트 파일인지 확인한다. 이 경우 STT를 건너뛴다. */
    fun isTranscript(name: String?): Boolean =
        name != null && TRANSCRIPT_FILE.matches(name)

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

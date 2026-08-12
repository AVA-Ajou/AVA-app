package com.ava.proto.stt

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ServerTranscriber"

/** 통화 한 건이 수 분짜리일 수 있어 넉넉히 잡는다. 모델이 실시간의 절반 속도로 받아적는다. */
private const val TIMEOUT_MS = 300_000

/**
 * 통화 녹음을 **우리 서버**로 보내 글로 옮긴다.
 *
 * 예전에는 Groq Whisper(실패 시 Gemini)를 썼다. 두 가지가 걸렸다 — 외부 API 키에 매이고,
 * 무엇보다 **통화 음성이 외부 업체로 나갔다.** 이 앱은 `READ_CALL_LOG` 조차 쓰지 않기로
 * 하며 권한을 깎아온 프로젝트라 그 방향과 맞지 않았다.
 *
 * 서버가 쓰는 모델(Gemma 4)은 오디오를 직접 받는다. 판정하는 모델과 같은 것이라 서버에
 * 모델을 하나만 올려도 된다 — 어댑터를 끄면 전사기, 켜면 판정기다.
 */
class ServerAudioTranscriber(
    private val context: Context,
    private val baseUrl: String,
    private val task: String = "voice",
) : AudioTranscriber {

    override suspend fun transcribe(audioUri: String): String? = withContext(Dispatchers.IO) {
        runCatching { upload(audioUri.toUri()) }
            .onFailure { Log.w(TAG, "전사 실패 — PENDING_TRANSCRIPTION 으로 남긴다", it) }
            .getOrNull()
    }

    /**
     * multipart/form-data 를 손으로 만든다. 이 저장소는 Retrofit/OkHttp 를 쓰지 않는다.
     *
     * 파일을 통째로 메모리에 올리지 않고 스트림으로 흘려보낸다 — 통화 녹음이 수십 MB 가
     * 될 수 있다.
     */
    private fun upload(uri: Uri): String? {
        val boundary = "----ava${System.currentTimeMillis()}"
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "call.m4a"

        val connection = (URL("$baseUrl/transcribe").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setChunkedStreamingMode(0)
        }

        try {
            DataOutputStream(connection.outputStream).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"task\"\r\n\r\n")
                out.writeBytes("$task\r\n")

                out.writeBytes("--$boundary\r\n")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n",
                )
                out.writeBytes("Content-Type: application/octet-stream\r\n\r\n")
                context.contentResolver.openInputStream(uri)?.use { it.copyTo(out) }
                    ?: throw IllegalStateException("오디오를 읽을 수 없음: $uri")
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IllegalStateException("서버 응답 $code: $detail")
            }

            val body = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val text = body.optString("text").takeIf { it.isNotBlank() }
            Log.d(TAG, "전사 완료 (${body.optInt("elapsed_ms")}ms): ${text?.take(60)}…")
            return text
        } finally {
            connection.disconnect()
        }
    }
}

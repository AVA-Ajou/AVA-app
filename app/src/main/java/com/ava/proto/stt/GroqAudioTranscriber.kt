package com.ava.proto.stt

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val TAG = "GroqAudioTranscriber"
private const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
private const val MODEL = "whisper-large-v3"
private const val BOUNDARY = "----GroqBoundary7MA4YWxkTrZu0gW"
private const val TIMEOUT_MS = 60_000

/**
 * Groq Whisper API로 통화 녹음을 한국어로 전사한다.
 *
 * multipart/form-data로 오디오 파일을 직접 전송하므로 base64 변환이 없고,
 * 무료 티어 한도(오디오 7,200req/day)가 Gemini보다 넉넉하다.
 */
class GroqAudioTranscriber(
    private val context: Context,
    private val apiKey: String,
) : AudioTranscriber {

    override suspend fun transcribe(audioUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(audioUri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.w(TAG, "오디오 파일을 읽을 수 없음: $audioUri")
                return@withContext null
            }

            val url = URL(ENDPOINT)
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.doOutput = true

            DataOutputStream(conn.outputStream).use { out ->
                // model
                out.writeBytes("--$BOUNDARY\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
                out.writeBytes("$MODEL\r\n")
                // language
                out.writeBytes("--$BOUNDARY\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"language\"\r\n\r\n")
                out.writeBytes("ko\r\n")
                // file
                out.writeBytes("--$BOUNDARY\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"audio.m4a\"\r\n")
                out.writeBytes("Content-Type: audio/mp4\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n")
                out.writeBytes("--$BOUNDARY--\r\n")
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e(TAG, "Groq 응답 오류 $responseCode: $error")
                return@withContext null
            }

            val responseText = conn.inputStream.use { it.bufferedReader().readText() }
            JSONObject(responseText).getString("text").trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "전사 실패: $audioUri", e)
            null
        }
    }
}

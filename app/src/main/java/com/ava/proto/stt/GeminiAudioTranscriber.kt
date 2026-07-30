package com.ava.proto.stt

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val TAG = "GeminiAudioTranscriber"
private const val MAX_BYTES = 15 * 1024 * 1024 // 15MB — Gemini inline data 상한
private const val TIMEOUT_MS = 60_000          // 대용량 오디오 업로드 여유 시간
private const val RETRY_DELAY_MS = 65_000L     // rate limit 시 재시도 대기 (서버 권고 + 여유)

/**
 * Gemini 1.5 Flash에 음성 파일을 직접 보내 한국어로 전사한다.
 *
 * SDK 의존성 없이 [HttpsURLConnection]으로 REST API를 호출한다.
 * 성공하면 전사 텍스트를 돌려주고, 이후 기존 [com.ava.proto.classification.ClassificationClient]가
 * 그 텍스트를 분류한다 — 이 클래스는 "말을 글로 바꾸는 것"까지만 담당한다.
 */
class GeminiAudioTranscriber(
    private val context: Context,
    private val apiKey: String,
) : AudioTranscriber {

    override suspend fun transcribe(audioUri: String): String? = transcribeInternal(audioUri, retryLeft = 1)

    private suspend fun transcribeInternal(audioUri: String, retryLeft: Int): String? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(audioUri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.w(TAG, "오디오 파일을 읽을 수 없음: $audioUri")
                return@withContext null
            }
            if (bytes.size > MAX_BYTES) {
                Log.w(TAG, "파일이 너무 큼(${bytes.size / 1024}KB), 스킵: $audioUri")
                return@withContext null
            }

            val base64Audio = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "audio/mp4")
                                    put("data", base64Audio)
                                })
                            })
                            put(JSONObject().apply {
                                put(
                                    "text",
                                    "이 통화 녹음을 한국어로 그대로 전사해주세요. " +
                                        "발화자 구분 없이 대화 내용만 순서대로 출력하고, " +
                                        "다른 설명이나 마크다운은 포함하지 마세요.",
                                )
                            })
                        })
                    })
                })
            }

            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash:generateContent?key=$apiKey",
            )
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true

            connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: ""
                if (retryLeft <= 0) {
                    Log.e(TAG, "Rate limit(429) — 재시도 횟수 초과, 포기: $error")
                    return@withContext null
                }
                Log.w(TAG, "Rate limit(429) — ${RETRY_DELAY_MS / 1000}초 후 재시도: $error")
                delay(RETRY_DELAY_MS)
                return@withContext transcribeInternal(audioUri, retryLeft - 1)
            }
            if (responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e(TAG, "Gemini 응답 오류 $responseCode: $error")
                return@withContext null
            }

            val responseText = connection.inputStream.use { it.bufferedReader().readText() }
            JSONObject(responseText)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "전사 실패: $audioUri", e)
            null
        }
    }
}

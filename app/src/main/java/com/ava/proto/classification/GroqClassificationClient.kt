package com.ava.proto.classification

import android.util.Log
import com.ava.proto.data.RiskSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val TAG = "GroqClassification"
private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
private const val MODEL = "llama-3.3-70b-versatile"
private const val TIMEOUT_MS = 30_000

private const val SYSTEM_PROMPT = "당신은 보이스피싱·스미싱 탐지 전문가입니다. " +
    "주어진 메시지가 금융사기(보이스피싱, 스미싱, 피싱)인지 판단하세요. " +
    "반드시 아래 JSON 형식으로만 응답하세요: " +
    "{\"is_phishing\": true 또는 false, \"reason\": \"한 문장으로 판단 이유\"}"

/**
 * Groq LLM(llama-3.3-70b)으로 피싱 여부를 판단하는 [ClassificationClient] 구현체.
 *
 * 키워드 목록 없이 LLM이 문맥 전체를 읽어 판단하므로,
 * 피싱 단어를 우회한 정교한 사기 문구도 탐지할 수 있다.
 */
class GroqClassificationClient(private val apiKey: String) : ClassificationClient {

    override suspend fun classify(text: String): ClassificationVerdict = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            })
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("temperature", 0)
        }

        val url = URL(ENDPOINT)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.doOutput = true

        conn.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
            Log.e(TAG, "Groq 응답 오류 $responseCode: $error")
            // 호출부(DetectionPipeline)가 CLASSIFICATION_FAILED로 처리하도록 예외를 올린다
            error("Groq API 오류 $responseCode")
        }

        val responseText = conn.inputStream.use { it.bufferedReader().readText() }
        val content = JSONObject(responseText)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val verdict = JSONObject(content)
        val isPhishing = verdict.getBoolean("is_phishing")
        val reason = verdict.optString("reason").takeIf { it.isNotBlank() }

        Log.d(TAG, "분류 결과: isPhishing=$isPhishing / $reason")
        if (isPhishing) ClassificationVerdict(RiskSignal.HIGH, reason)
        else ClassificationVerdict(RiskSignal.NONE, null)
    }
}

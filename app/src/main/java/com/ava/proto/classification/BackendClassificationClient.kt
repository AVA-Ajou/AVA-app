package com.ava.proto.classification

import android.util.Log
import com.ava.proto.data.RiskSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "BackendClassification"
private const val TIMEOUT_MS = 60_000

/** 모델이 쓰는 따옴표가 곧은 것("")과 굽은 것(“”) 두 종류라 둘 다 받는다. */
private val QUOTED = Regex("[\"“]([^\"”]{5,})[\"”]")
private val MARKUP = Regex("""^[\s\d.*#\-]+|\*+""")

/**
 * 파인튜닝한 Gemma를 올린 서버(`Detection-Server`)에 판정을 맡기는 [ClassificationClient].
 *
 * 서버가 **두 가지를 따로** 준다.
 *
 *   위험도   어댑터를 켠 채 정답 토큰 자리의 확률을 읽은 값. 보정돼 있다
 *   단계     어댑터를 끈 원본 Gemma가 전사본을 읽고 매긴 진행 단계 1~4
 *
 * 앱은 둘을 다시 계산하지 않는다. 위험도는 학습으로 점검된 숫자라 여기서 손댈 근거가 없고,
 * 단계는 전사본에 적힌 사실이라 마찬가지다.
 *
 * 서버가 없으면 예외를 던진다. 계약대로 호출부가 CLASSIFICATION_FAILED로 기록한다 —
 * 실패를 [RiskSignal.NONE]으로 뭉개면 "무해함"과 구분이 사라진다.
 */
class BackendClassificationClient(
    private val baseUrl: String,
    private val task: String = "voice",
    /**
     * 단계·근거를 받아올 위험도 문턱. 이 값을 넘을 때만 두 번째 호출을 한다.
     *
     * 단계 판정은 원본 Gemma가 160토큰을 생성해야 해서 십수 초가 걸린다. 위험도는 순전파
     * 한 번이라 1초 미만이다. 모든 메시지에 단계를 물으면 정상 통화에도 그 시간을 쓰고
     * 경보가 그만큼 늦어진다 — 설명이 필요한 건 위험하다고 판정한 건뿐이다.
     */
    private val assessThreshold: Double = 70.0,
) : ClassificationClient {

    override suspend fun classify(text: String): ClassificationVerdict = withContext(Dispatchers.IO) {
        // 1단계 — 위험도만. 빠르게 받아 경보 여부를 먼저 정한다.
        val risk = post(body(text, reason = false)).getDouble("risk")
        Log.d(TAG, "위험도 $risk")

        // 2단계 — 위험할 때만 단계·근거. 실패해도 판정은 살린다.
        val detail = if (risk >= assessThreshold) {
            runCatching { post(body(text, reason = true)) }
                .onFailure { Log.w(TAG, "단계 판정 실패 — 위험도만 사용", it) }
                .getOrNull()
        } else {
            null
        }

        val reason = detail?.optString("reason")?.takeIf { it.isNotBlank() }
        val stage = detail?.optInt("stage", 0)?.takeIf { it in 1..4 }
        if (stage != null) Log.d(TAG, "단계 $stage ${detail?.optString("stage_label")}")

        ClassificationVerdict(
            riskSignal = signalOf(risk),
            matchedPhrase = quotedPhrase(reason),
            risk = risk,
            stage = stage,
            stageLabel = detail?.optString("stage_label")?.takeIf { it.isNotBlank() },
            reason = reason,
        )
    }

    private fun body(text: String, reason: Boolean) = JSONObject()
        .put("text", text)
        .put("task", task)
        .put("reason", reason)

    /**
     * 0~100 위험도를 세 단계로 접는다.
     *
     * 현재 모델은 확률이 0 아니면 100 근처로 몰려 있어 [RiskSignal.LOW]가 사실상 나오지
     * 않는다. 검증셋이 통화 **전체**라 애매할 여지가 없었기 때문으로 보이며, 실전처럼
     * 앞부분만 들어오면 달라질 수 있다. 경계를 미리 잡아두고 그때 조정한다.
     */
    private fun signalOf(risk: Double): RiskSignal = when {
        risk >= 70 -> RiskSignal.HIGH
        risk >= 40 -> RiskSignal.LOW
        else -> RiskSignal.NONE
    }

    /**
     * 근거에서 **원문 인용 부분만** 뽑아 화면용 한 줄로 쓴다.
     *
     * 첫 줄을 그대로 쓰면 모델이 붙이는 머리말(`판정 근거:`)이 잡혀 아무 정보가 없다.
     * 서버 프롬프트가 원문 인용을 강제하므로 따옴표 안쪽이 실제 발언이고, 그게 사용자에게
     * 보여줄 값이다. 인용이 없으면 머리말·번호를 걷어낸 첫 문장으로 물러선다.
     */
    private fun quotedPhrase(reason: String?): String? {
        if (reason.isNullOrBlank()) return null
        QUOTED.find(reason)?.groupValues?.get(1)?.trim()?.let { if (it.length >= 5) return it }
        return reason.lineSequence()
            .map { it.replace(MARKUP, "").trim() }
            .firstOrNull { it.length >= 10 }
    }

    private fun post(body: JSONObject): JSONObject {
        val connection = (URL("$baseUrl/analyze").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IllegalStateException("서버 응답 $code: $detail")
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}

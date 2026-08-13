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

/**
 * 서버가 어느 어댑터를 켤지 고르는 값(`adapters/voice`). 생성자 인자로 열어뒀었는데 다른
 * 값을 넘기는 곳이 없었다 — 서버가 어댑터를 하나만 올리기 때문이다. 베이스가 다른 어댑터를
 * 섞으면 확률이 조용히 틀어져서 그렇다(`../Detection-Server/README.md`).
 */
private const val TASK = "voice"

/**
 * 파인튜닝한 Gemma를 올린 서버(`Detection-Server`)에 판정을 맡기는 [ClassificationClient].
 *
 * 서버가 **두 가지를 따로** 준다.
 *
 *   위험도   어댑터를 켠 채 정답 토큰 자리의 확률을 읽은 값. 보정돼 있다
 *   단계     서버의 정규식이 전사본에서 읽은 진행 단계 1~3
 *
 * 앱은 둘을 다시 계산하지 않는다. 위험도는 학습으로 점검된 숫자라 여기서 손댈 근거가 없고,
 * 단계는 서버가 정답지로 검증한 규칙이 뽑은 값이라 마찬가지다.
 *
 * 서버가 없으면 예외를 던진다. 계약대로 호출부가 CLASSIFICATION_FAILED로 기록한다 —
 * 실패를 [RiskSignal.NONE]으로 뭉개면 "무해함"과 구분이 사라진다.
 */
class BackendClassificationClient(private val baseUrl: String) : ClassificationClient {

    /**
     * **호출은 한 번뿐이다.**
     *
     * 예전에는 두 번 불렀다 — 위험도를 먼저 받고, 문턱을 넘으면 단계·근거를 다시 요청했다.
     * 단계가 원본 Gemma의 160토큰 생성에 딸려 있어서 판정 한 건에 16초가 걸렸다.
     * 지금은 단계를 서버의 정규식이 뽑으므로 비용이 없고, 위험도와 함께 나온다.
     * 근거 문장은 화면에 쓰지 않아 아예 요청하지 않는다(`reason` 을 켜지 않는다).
     */
    override suspend fun classify(text: String): ClassificationVerdict = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", text).put("task", TASK)
        val response = post(body)

        val risk = response.getDouble("risk")
        val stage = response.optInt("stage", 0).takeIf { it in 1..3 }
        val stageLabel = response.optString("stage_label").takeIf { it.isNotBlank() }
        Log.d(TAG, "위험도 $risk" + if (stage != null) " · 단계 $stage $stageLabel" else "")

        ClassificationVerdict(
            riskSignal = signalOf(risk),
            // 화면에는 안 나가지만 기록에는 남긴다 — 규칙이 어느 문구를 보고 그 단계를
            // 매겼는지가 오판을 되짚는 유일한 실마리다.
            matchedPhrase = firstEvidence(response),
            risk = risk,
            stage = stage,
            stageLabel = stageLabel,
        )
    }

    /** 규칙이 그 단계를 발동시킨 원문 인용구. 없으면 null. */
    private fun firstEvidence(response: JSONObject): String? =
        response.optJSONArray("stage_evidence")
            ?.takeIf { it.length() > 0 }
            ?.optString(0)
            ?.takeIf { it.isNotBlank() }

    /**
     * 0~100 위험도를 세 단계로 접는다.
     *
     * 검증셋 213건에서는 값이 0 아니면 100으로 갈려 [RiskSignal.LOW]가 거의 나오지 않는다.
     * 다만 그건 검증셋에 애매한 통화가 없어서다 — 은행이 먼저 걸어온 정상 통화를 넣으면
     * 41 / 51 / 59 같은 값이 실제로 나온다. LOW 구간은 그때를 위해 잡아둔 것이다.
     * 화면에서는 70 이상만 피싱으로 표시하므로 LOW 와 NONE 은 지금 구분되지 않는다.
     */
    private fun signalOf(risk: Double): RiskSignal = when {
        risk >= 70 -> RiskSignal.HIGH
        risk >= 40 -> RiskSignal.LOW
        else -> RiskSignal.NONE
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

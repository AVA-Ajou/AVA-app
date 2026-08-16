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
 * 경보(빨강) 문턱. 이 위는 모델 혼자서도 확신하는 구간이다.
 *
 * 검증셋 213건에서 정상 통화의 최고 점수가 38.3이고 피싱 106건 중 102건이 84 이상이라,
 * 40~84 사이는 사실상 비어 있다. 그 빈 구간 어디에 그어도 검증셋 결과는 같으므로
 * 아래 [CAUTION_THRESHOLD]와 짝이 맞는 값으로 잡았다.
 */
private const val ALERT_THRESHOLD = 66.0

/**
 * 주의보(주황) 문턱. **이 구간은 위험도만으로 판정하지 않는다** — 서버 규칙이 진행 단계를
 * 하나라도 찾아냈을 때만 주의보가 되고, 못 찾으면 정상으로 둔다.
 *
 * 두 판정기의 교집합을 쓰는 것이 요점이다. 규칙만 쓰면 헐거워서 정상 통화 10건 중 8건에서
 * 신호가 켜지고, 모델만 쓰면 이 구간에서 정상·피싱이 섞인다. 모델이 33점 이상 준 것에만
 * 규칙을 적용하면 검증셋 정상 107건에서 주의보가 한 건도 나오지 않았다(실측).
 */
private const val CAUTION_THRESHOLD = 33.0

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
            riskSignal = signalOf(risk, stage),
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
     * 위험도와 단계를 함께 보고 등급을 정한다.
     *
     * ```
     *   66 이상                   → HIGH     경보(빨강)
     *   33 이상 + 단계가 잡힘      → CAUTION  주의보(주황)
     *   33 이상 + 단계가 없음      → NONE     표시하지 않는다
     *   33 미만                   → NONE
     * ```
     *
     * **단계를 같이 보는 것이 이 함수의 요점이다.** 33~66 구간은 모델이 애매해하는 자리라
     * 위험도만으로는 정상과 피싱이 섞인다. 실측에서 이 구간에 정상 6건(퇴직연금 상담 51.0,
     * 부동산 잔금일정 47.0 등)과 피싱 3건(가족사칭 48.0 등)이 함께 들어왔다. 서버 규칙이
     * 요구 문형을 찾았는지를 두 번째 근거로 쓰면 그중 상당수가 갈린다.
     *
     * 위험도가 [ALERT_THRESHOLD] 이상이면 단계 없이도 HIGH다. 규칙이 신호를 못 찾는 피싱이
     * 검증셋 기준 19%나 되므로, 단계를 경보의 조건으로 걸면 그만큼이 통째로 빠진다.
     *
     * **[CAUTION_THRESHOLD]는 서버 `ASSESS_THRESHOLD` 와 같은 값이어야 한다** — 서버가 그
     * 아래로는 단계를 계산하지 않으므로, 앱이 더 낮은 값을 쓰면 단계가 영영 null인 구간이
     * 생겨 주의보가 나올 수 없다.
     */
    private fun signalOf(risk: Double, stage: Int?): RiskSignal = when {
        risk >= ALERT_THRESHOLD -> RiskSignal.HIGH
        risk >= CAUTION_THRESHOLD && stage != null -> RiskSignal.CAUTION
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

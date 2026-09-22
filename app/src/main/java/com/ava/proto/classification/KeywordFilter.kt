package com.ava.proto.classification

import com.ava.proto.data.RiskSignal

/**
 * 단순 문자열 매칭기. 그 자체로는 게이트가 아니다 — [LocalKeywordClassificationClient]가
 * 이걸 감싸서 [ClassificationClient] 대역으로 쓰고 있을 뿐이고, 텍스트가 있는 이벤트는
 * 이 결과와 무관하게 전부 분류 단계를 거친다.
 *
 * **문구를 두 갈래로 나눈다.** 예전에는 16개가 한 목록이었고 하나라도 걸리면 곧바로
 * `경보`였는데, 그 목록에 `금융감독원`·`수사관`·`검찰청` 같은 **기관 이름**이 섞여 있었다.
 * 기관 이름은 정상 통화에도 그대로 나온다 — `eval/hard_normal.jsonl`의 어려운 정상 20건이
 * 전부 금융기관이 먼저 걸어온 통화라, 이 대역에서는 그 20건이 남김없이 최고 등급을 받았다.
 *
 * [strongPhrases]는 **사기범이 무언가를 요구하는 순간**의 말이다. 정상 통화에서 나올 일이
 * 거의 없다. [weakPhrases]는 정황을 가리킬 뿐이라 단독으로는 아무것도 확정하지 못한다.
 *
 * 약한 문구만 걸리면 [RiskSignal.FORECAST]다. 이 등급의 정의가 **"판정기 어느 쪽도
 * 확신하지 않은 구간"**이고, 기관 이름 하나 나온 상태가 정확히 그 자리다. 없는 위험도를
 * 지어내지 않고 지금 아는 만큼만 말하는 셈이다.
 *
 * **이 대역이 만드는 등급은 여전히 규칙의 판단이지 모델의 판정이 아니다.** 서버가 붙으면
 * [BackendClassificationClient]가 보정된 위험도로 등급을 다시 매긴다.
 */
object KeywordFilter {

    /** 요구·지시의 말. 걸리면 곧바로 경보로 본다. */
    private val strongPhrases = listOf(
        "안전계좌",
        "계좌 이체",
        "대포통장",
        "출석 요구",
        "구속영장",
        "개인정보 유출",
        "범죄에 연루",
        "협조하지 않으면",
        "동결 계좌",
        "가상계좌",
        "설치 링크",
        "원격 제어 앱",
    )

    /** 정황을 가리키는 말. 정상 통화에도 나오므로 단독으로는 확정하지 않는다. */
    private val weakPhrases = listOf(
        "수사관",
        "검찰청",
        "금융감독원",
        "명의도용",
    )

    fun evaluate(text: String): FilterResult {
        strongPhrases.firstOrNull { text.contains(it) }
            ?.let { return FilterResult(RiskSignal.HIGH, it) }
        weakPhrases.firstOrNull { text.contains(it) }
            ?.let { return FilterResult(RiskSignal.FORECAST, it) }
        return FilterResult(RiskSignal.NONE, null)
    }
}

data class FilterResult(val riskSignal: RiskSignal, val matchedPhrase: String?)

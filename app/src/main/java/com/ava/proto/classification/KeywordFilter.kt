package com.ava.proto.classification

import com.ava.proto.data.RiskSignal

/**
 * 단순 문자열 매칭기. 그 자체로는 게이트가 아니다 — [LocalKeywordClassificationClient]가
 * 이걸 감싸서 [ClassificationClient] 대역으로 쓰고 있을 뿐이고, 텍스트가 있는 이벤트는
 * 이 결과와 무관하게 전부 분류 단계를 거친다.
 *
 * [RiskSignal.LOW]는 아직 이 필터가 만들어내지 않는다 — 백엔드(Gemini) 분류가 붙으면
 * 애매한 경우를 LOW로 내려보내는 역할을 맡긴다. 지금은 매칭되면 HIGH, 아니면 NONE뿐이다.
 */
object KeywordFilter {

    private val highRiskPhrases = listOf(
        "안전계좌",
        "계좌 이체",
        "수사관",
        "검찰청",
        "금융감독원",
        "명의도용",
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

    fun evaluate(text: String): FilterResult {
        val matched = highRiskPhrases.firstOrNull { text.contains(it) }
        return if (matched != null) {
            FilterResult(RiskSignal.HIGH, matched)
        } else {
            FilterResult(RiskSignal.NONE, null)
        }
    }
}

data class FilterResult(val riskSignal: RiskSignal, val matchedPhrase: String?)

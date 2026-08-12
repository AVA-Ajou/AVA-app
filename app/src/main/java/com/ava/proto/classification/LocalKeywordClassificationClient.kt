package com.ava.proto.classification

/**
 * 서버 주소가 없을 때 쓰는 대역 [ClassificationClient] 구현체.
 *
 * 키워드 매칭이라 정교한 사기 문구는 놓칠 수 있다 — 이건 이 구현체의 한계이지, 인터페이스나
 * 파이프라인의 한계가 아니다. [BackendClassificationClient] 가 붙으면 자동으로 해소된다.
 * 설정이 덜 돼도 앱은 떠야 하므로 지우지 말 것.
 */
class LocalKeywordClassificationClient : ClassificationClient {
    override suspend fun classify(text: String): ClassificationVerdict {
        val result = KeywordFilter.evaluate(text)
        return ClassificationVerdict(result.riskSignal, result.matchedPhrase)
    }
}

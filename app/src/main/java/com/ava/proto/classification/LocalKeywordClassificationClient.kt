package com.ava.proto.classification

/**
 * 백엔드가 준비되기 전까지 쓰는 임시 [ClassificationClient] 구현체.
 *
 * 키워드 매칭이라 정교한 사기 문구는 놓칠 수 있다 — 이건 이 구현체의 한계이지, 인터페이스나
 * 파이프라인의 한계가 아니다. Gemini 기반 구현체로 교체되면 자동으로 해소된다.
 */
class LocalKeywordClassificationClient : ClassificationClient {
    override suspend fun classify(text: String): ClassificationVerdict {
        val result = KeywordFilter.evaluate(text)
        return ClassificationVerdict(result.riskSignal, result.matchedPhrase)
    }
}

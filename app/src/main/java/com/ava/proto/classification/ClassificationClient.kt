package com.ava.proto.classification

import com.ava.proto.data.RiskSignal

data class ClassificationVerdict(
    val riskSignal: RiskSignal,
    val matchedPhrase: String?,
)

/**
 * 텍스트가 있는 이벤트는 예외 없이 전부 이 인터페이스를 거친다 — 게이트를 먼저 통과해야
 * 판정을 받는 구조가 아니다. 사기꾼일수록 뻔한 트리거 단어를 피하기 때문에, 고정 규칙으로
 * 먼저 걸러내면 오히려 정교한 사기일수록 판정기 앞까지 못 온다.
 *
 * 지금은 백엔드(Gemini)가 없어 [LocalKeywordClassificationClient]가 대역을 서고 있고,
 * 백엔드가 준비되면 이 구현체 하나만 교체하면 된다 — 호출하는 쪽([com.ava.proto.pipeline.DetectionPipeline])은
 * 손댈 필요 없다.
 */
interface ClassificationClient {
    suspend fun classify(text: String): ClassificationVerdict
}

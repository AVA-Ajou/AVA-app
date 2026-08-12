package com.ava.proto.classification

import com.ava.proto.data.RiskSignal

data class ClassificationVerdict(
    val riskSignal: RiskSignal,
    val matchedPhrase: String?,
    /**
     * 0~100 위험도. 모델이 로짓에서 읽은 **보정된 확률**이라 87점은 실제로 87% 확률을 뜻한다
     * (검증셋 ECE 0.006). [riskSignal]은 이 값을 세 단계로 접은 것이라 87점과 100점을
     * 구분하지 못하므로, 원본 숫자를 따로 남긴다. 키워드 폴백에서는 null이다.
     */
    val risk: Double? = null,
    /**
     * 사기 진행 단계 1~4. **위험도와는 다른 값이다** — 위험도가 "피싱이 맞나"라면 이건
     * "지금 어디까지 왔나"다. 같은 99%라도 사칭만 한 상태와 계좌번호를 부르는 상태는
     * 사용자가 해야 할 행동이 다르다. 정상 판정이거나 모델이 형식을 어기면 null.
     */
    val stage: Int? = null,
    val stageLabel: String? = null,
    /** 모델이 원문을 인용해 쓴 판정 근거. */
    val reason: String? = null,
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

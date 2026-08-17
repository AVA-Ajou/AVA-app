package com.ava.proto.classification

import com.ava.proto.capture.Channel
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
     * 사기 진행 단계 1~3(압박·유인 / 정보·계좌 / 이체 지시). **위험도와는 다른 값이다** —
     * 위험도가 "피싱이 맞나"라면 이건 "지금 어디까지 왔나"다. 같은 99%라도 겁만 준 상태와
     * 계좌로 입금하라는 상태는 사용자가 해야 할 행동이 다르다.
     *
     * **화면에 나가는 판정 표시는 이것 하나다** — 위험도 숫자는 띄우지 않는다.
     * 정상 판정이거나 서버 규칙이 신호를 하나도 못 찾으면 null.
     */
    val stage: Int? = null,
    val stageLabel: String? = null,
)

/**
 * 텍스트가 있는 이벤트는 예외 없이 전부 이 인터페이스를 거친다 — 게이트를 먼저 통과해야
 * 판정을 받는 구조가 아니다. 사기꾼일수록 뻔한 트리거 단어를 피하기 때문에, 고정 규칙으로
 * 먼저 걸러내면 오히려 정교한 사기일수록 판정기 앞까지 못 온다.
 *
 * 기본 구현체는 파인튜닝 모델 서버를 부르는 [BackendClassificationClient] 다. 서버 주소가
 * 비어 있으면 [LocalKeywordClassificationClient]가 대역을 선다 — 고르는 곳은
 * [com.ava.proto.ProtoApplication] 한 군데뿐이고, 호출하는 쪽
 * ([com.ava.proto.pipeline.DetectionPipeline])은 누가 왔는지 모른다.
 */
interface ClassificationClient {
    suspend fun classify(text: String, channel: Channel): ClassificationVerdict
}

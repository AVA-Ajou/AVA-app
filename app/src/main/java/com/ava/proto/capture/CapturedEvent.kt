package com.ava.proto.capture

/**
 * 캡처 계층이 만들어내는 가공 전 데이터. 분류·세션 융합을 전혀 모른다 — 이 계층의 책임은
 * "무슨 일이 있었는지 받아적는 것"까지다.
 */
data class CapturedEvent(
    val channel: Channel,
    val capturedAt: Long,
    val sourceLabel: String,
    /** null이면 아직 분석할 텍스트가 없다는 뜻 (통화 녹음, STT 대기 중). */
    val text: String?,
    val audioUri: String? = null,
    /** 상대방 식별자. 통화는 발신번호를 알 방법이 없어 항상 null이다. */
    val counterpart: String? = null,
)

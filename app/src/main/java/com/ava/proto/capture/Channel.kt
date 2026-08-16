package com.ava.proto.capture

/**
 * 세 채널 모두 이 이름으로 이벤트·세션에 기록된다.
 *
 * [label]은 화면과 알림에 나가는 이름이다. 저장·조회는 계속 `name`(CALL/SMS/KAKAO)으로 하고
 * 표시만 갈라놓은 이유는, 한글 이름을 그대로 저장하면 [com.ava.proto.data.Converters]가 만든
 * 기존 행이 전부 못 읽는 값이 되기 때문이다 — 표시 문구는 앞으로도 바뀔 수 있는 값이다.
 */
enum class Channel(val label: String) {
    CALL("통화 녹음"),
    SMS("문자"),
    KAKAO("카카오톡"),
}

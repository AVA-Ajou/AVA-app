package com.ava.proto.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ava.proto.capture.Channel

/** 로컬 키워드 필터(또는 추후 백엔드 분류)가 매긴 위험도. */
enum class RiskSignal {
    NONE,
    LOW,
    HIGH,
}

enum class EventStatus {
    /** 텍스트까지 확보되어 분석이 끝난 상태 (SMS/카톡은 캡처 즉시 이 상태). */
    ANALYZED,

    /** 통화 녹음처럼 원본은 있지만 아직 텍스트가 없는 상태 (STT 대기, 백엔드 연동 전까지 유지됨). */
    PENDING_TRANSCRIPTION,

    /**
     * 텍스트는 있었지만 분류 호출 자체가 실패한 상태 (네트워크 오류 등).
     * [RiskSignal.NONE]과 구분해서 기록한다 — NONE은 "분류해봤더니 무해함"이라는 뜻이지,
     * "분류를 못 함"이라는 뜻이 아니다. 이 상태가 없으면 실패를 NONE으로 뭉개게 되어
     * 게이트 시절의 모호함("NONE = 분석 안 됨")이 조용히 되돌아온다.
     */
    CLASSIFICATION_FAILED,
}

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channel: Channel,
    /** SMS/카톡은 알림 시각, 통화 녹음은 파일의 마지막 수정 시각. */
    val capturedAt: Long,
    /** 알림 발신 패키지명 또는 녹음 파일명. */
    val sourceLabel: String,
    val text: String?,
    val status: EventStatus,
    val riskSignal: RiskSignal,
    val matchedPhrase: String?,
    val sessionId: Long?,
    /** 통화 녹음 채널에서만 채워짐. STT 연동 전까지는 재생·전송에 쓰이지 않는다. */
    val audioUri: String? = null,
    /**
     * 상대방 식별자 (SMS/카톡은 알림의 발신자 제목, 통화는 발신번호를 알 방법이 없어 항상 null —
     * READ_CALL_LOG를 쓰지 않기로 한 결정 때문에 이 채널만 식별자가 비어 있다).
     * 세션 융합이 시간 창만으로 무관한 상대를 섞어버리지 않도록 이 값으로 스코프를 좁힌다.
     */
    val counterpart: String? = null,
    /**
     * 모델이 매긴 0~100 위험도. 백엔드 판정 경로에서만 채워진다.
     *
     * [riskSignal]은 이 값을 세 단계로 접은 것이라 87점과 100점을 구분하지 못한다.
     * 원본을 따로 남기는 이유는 그것이 **보정된 확률**이기 때문이다 — 검증셋에서 ECE 0.006으로
     * 확인했듯 87점은 실제로 87% 확률을 뜻한다. 접어버리면 그 정보가 사라진다.
     */
    val risk: Double? = null,
    /**
     * 사기 진행 단계 1~4와 그 이름. 위험도와 **다른 값**이다 — 위험도가 "피싱이 맞나"라면
     * 이건 "지금 어디까지 왔나"다. 같은 99%라도 1단계(사칭만)와 4단계(송금 지시)는
     * 사용자가 해야 할 행동이 다르다.
     */
    val stage: Int? = null,
    val stageLabel: String? = null,
)

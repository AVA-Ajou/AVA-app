package com.ava.proto.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ava.proto.capture.Channel

/**
 * 판정을 세 단계로 접은 값. 기본 경로는 모델 서버가 준 [EventEntity.risk]를 접은 것이고,
 * 서버 주소가 없을 때만 키워드 대역이 만든다(그쪽은 `HIGH`/`NONE` 둘뿐이다).
 */
enum class RiskSignal {
    NONE,
    LOW,
    HIGH,
}

enum class EventStatus {
    /** 텍스트까지 확보되어 분석이 끝난 상태 (SMS/카톡은 캡처 즉시 이 상태). */
    ANALYZED,

    /**
     * 원본은 있는데 텍스트가 없는 상태. 서버 주소가 없어 전사를 아예 하지 않았거나,
     * 전사를 시도했다 실패한 경우다. [EventDao.analyzedSourceLabels]가 이 상태를 제외하므로
     * 다음 스캔에서 다시 시도된다.
     */
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
    /**
     * 음성 파일의 SAF URI. 통화 채널에서만, 그중에서도 실제 녹음일 때만 채워진다
     * (`.txt` 전사본은 재생할 오디오가 없어 null).
     *
     * **읽는 코드는 아직 없다** — 앱에 재생 화면이 없기 때문이다. 그래도 남기는 이유는
     * 오판을 되짚을 때 "그 판정이 어느 파일에서 나왔나"를 아는 유일한 값이라서다.
     * [sourceLabel]은 파일명뿐이라 폴더를 바꾸면 같은 이름이 겹칠 수 있다.
     */
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
     * 사기 진행 단계 1~3과 그 이름. 위험도와 **다른 값**이다 — 위험도가 "피싱이 맞나"라면
     * 이건 "지금 어디까지 왔나"다. 같은 99%라도 1단계(겁주기만)와 3단계(송금 지시)는
     * 사용자가 해야 할 행동이 다르다. 화면에 나가는 것은 이 값이다.
     */
    val stage: Int? = null,
    val stageLabel: String? = null,
)

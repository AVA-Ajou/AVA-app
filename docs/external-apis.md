# 서버 연동

**외부 API를 쓰지 않는다.** 앱이 부르는 것은 우리가 띄운 탐지 서버(`../Detection-Server`)
하나뿐이다.

예전에는 Groq Whisper(STT)와 Groq LLM(분류)에 매여 있었다. 걷어낸 이유는 두 가지다.

- 서버 모델(Gemma 4)이 **오디오를 직접 받는다.** 전사를 남에게 맡길 이유가 없어졌다
- **통화 음성이 외부 업체로 나갔다.** `READ_CALL_LOG` 조차 쓰지 않기로 하며 권한을 깎아온
  이 프로젝트에서 앞뒤가 맞지 않았다

## 주소 주입

`local.properties` → `buildConfigField` 경로만 쓴다. 코드나 커밋에 주소를 넣지 않는다.

```properties
DETECTION_SERVER_URL=http://localhost:8000
```

비어 있으면 판정은 `LocalKeywordClassificationClient`로 떨어지고 전사는 하지 않는다
(이벤트는 `PENDING_TRANSCRIPTION`으로 남는다). 설정이 덜 돼도 앱은 떠야 한다.

에뮬레이터·실기기에서는 `adb reverse tcp:8000 tcp:8000` 으로 붙인다. `10.0.2.2` 직결은
맥 방화벽이 TCP를 막아 타임아웃난다 — ICMP는 통과해서 `ping`은 성공하므로 헷갈리기 쉽다.

평문 HTTP는 `res/xml/network_security_config.xml`이 허용한 호스트에서만 열린다.

## 호출하는 곳

| | 언제 | 실패하면 |
|---|---|---|
| `POST /transcribe` | `RecordingScanWorker`가 새 음성 파일을 찾았을 때 | `null` → `PENDING_TRANSCRIPTION`. 다음 스캔에서 다시 시도한다 |
| `POST /analyze` | 텍스트가 있는 모든 이벤트 | **예외를 던진다** → 호출부가 `CLASSIFICATION_FAILED`로 기록 |

**실패 처리 계약이 다르다.** 전사는 "아직 못 함"이라 다시 시도할 수 있고, 분류는 "판정을
못 했다"를 `RiskSignal.NONE`("무해함")과 구분해야 하기 때문이다.

## 분류는 호출 한 번이다

예전에는 두 번 불렀다 — 위험도를 먼저 받고, 문턱을 넘으면 단계·근거를 다시 요청했다.
단계가 원본 Gemma의 160토큰 생성에 딸려 있어 **판정 한 건에 16초**가 걸렸다.

지금은 단계를 서버의 정규식(`../Detection-Server/signals.py`)이 뽑으므로 위험도와 같은
응답에 함께 온다. 근거 문장은 화면에 쓰지 않으므로 아예 요청하지 않는다(`reason`을 켜지
않는다). **두 번째 호출을 되살리지 말 것.**

```json
요청   {"text": "…전사본…", "task": "voice"}
응답   {"risk": 93.2, "stage": 2, "stage_label": "정보·계좌", "stage_evidence": ["…"]}
```

`task`는 서버가 어느 어댑터를 켤지 고르는 값이다. 서버가 어댑터를 하나만 올리므로 앱에서는
`"voice"` 상수로 고정돼 있다 — 베이스가 다른 어댑터를 섞으면 확률이 조용히 틀어진다.

스펙은 `../Detection-Server/docs/api.md`.

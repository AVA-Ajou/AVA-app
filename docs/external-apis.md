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

분류는 두 번에 나눠 부른다 — 위험도(순전파 한 번, 1초 미만)를 먼저 받아 경보 여부를 정하고,
문턱을 넘을 때만 단계·근거(생성, 십수 초)를 요청한다. 두 번째가 실패해도 판정은 살린다.

스펙은 `../Detection-Server/docs/api.md`.

# 외부 API 연동

HTTP 클라이언트 라이브러리를 쓰지 않는다. `HttpsURLConnection` + `org.json`만으로 호출한다
(의존성 추가 없이 동작하는 게 프로토타입 단계에서 더 낫다는 판단).
따라서 새 API를 붙일 때도 Retrofit/OkHttp를 끌어들이기 전에 이 방식으로 충분한지 먼저 볼 것.

## API 키 주입 경로

`local.properties`(gitignore됨) → `app/build.gradle.kts`가 직접 파싱 → `buildConfigField` → `BuildConfig.*`

```properties
sdk.dir=/path/to/android/sdk
GROQ_API_KEY=gsk_...
GEMINI_API_KEY=...     # 선택 — STT 폴백용
```

`project.findProperty()`는 `local.properties`를 읽지 못하므로 `Properties().load()`로 직접 읽는다.
키가 없으면 빈 문자열이 들어가고, 런타임 폴백이 그 상태를 처리한다 — 빌드는 깨지지 않는다.

## 구현체 선택 (`ProtoApplication`, 모두 `by lazy`)

```
classificationClient : GROQ_API_KEY 있음 → GroqClassificationClient
                       없음             → LocalKeywordClassificationClient

audioTranscriber     : GROQ_API_KEY 있음 → GroqAudioTranscriber
                       GEMINI_API_KEY만  → GeminiAudioTranscriber
                       둘 다 없음        → null (STT 생략 → PENDING_TRANSCRIPTION)
```

## 분류 — `GroqClassificationClient`

- `POST https://api.groq.com/openai/v1/chat/completions`, 모델 `llama-3.3-70b-versatile`
- `response_format: json_object`, `temperature: 0`
- 시스템 프롬프트가 `{"is_phishing": bool, "reason": "한 문장"}` 형태를 강제한다
- `is_phishing == true` → `RiskSignal.HIGH` + `matchedPhrase = reason`, false → `NONE`

**비-200 응답에서는 예외를 던진다.** 이건 실수가 아니라 계약이다 —
`DetectionPipeline`이 그 예외를 잡아 `EventStatus.CLASSIFICATION_FAILED`로 기록해야
"분류 실패"와 "무해함 확인"이 구분된다. 여기서 조용히 `NONE`을 돌려주면 그 구분이 사라진다.

키워드 폴백(`KeywordFilter`)은 16개 고위험 문구 단순 `contains` 매칭이다.
정교하게 우회한 사기 문구는 놓치지만, 그건 이 구현체의 한계일 뿐 인터페이스나 파이프라인의 한계가 아니다.

## STT — `GroqAudioTranscriber`

- `POST https://api.groq.com/openai/v1/audio/transcriptions`, 모델 `whisper-large-v3`, `language=ko`
- multipart/form-data로 오디오 바이트를 직접 전송 (base64 변환 없음)
- 타임아웃 60초
- **실패 시 예외 대신 `null`을 돌려준다** — `AudioTranscriber` 계약이 그렇다.
  `RecordingScanWorker`는 null을 받으면 텍스트 없는 `CapturedEvent`를 만들고,
  파이프라인이 `PENDING_TRANSCRIPTION`으로 기록한다. 이벤트 자체를 유실시키지 않는다.

분류와 STT의 실패 처리 방식이 다르다(예외 vs null)는 점에 유의 —
각 인터페이스의 KDoc에 명시된 계약이므로 새 구현체도 이를 따라야 한다.

## 파일 단위 실패 격리

`RecordingScanWorker`는 파일 하나의 처리가 실패해도 `try/catch`로 삼키고 다음 파일로 넘어간다.
문제 파일 하나가 이후 모든 실행을 막는 상황을 피하기 위한 것이다.
다만 실패한 파일은 이벤트로 기록되지 않으므로 dedup 목록에도 안 들어가 다음 스캔에서 재시도된다.

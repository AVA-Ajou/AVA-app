# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AVA Proto** — 다채널 보이스피싱 탐지 Android 프로토타입.
카카오톡 · SMS · 통화 녹음 세 채널을 동시에 감시하고, 위험 신호가 잡히면 즉시 알리고,
같은 시간 창 안에서 두 채널 이상이 겹치면 경보를 격상시킨다.

단일 모듈 앱(`:app`)이며 서버·백엔드가 없다. 분류와 STT는 Groq API를 직접 호출한다.
`SessionState.ALERT`와 `RiskSignal.LOW`는 "백엔드 확정 분류"용으로 자리만 잡아둔 값으로,
현재 코드 경로로는 도달하지 않는다.

## Critical Rules

- **권한을 늘리지 말 것.** `READ_CALL_LOG` / `RECEIVE_SMS` / `READ_SMS` /
  `MANAGE_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO`는 의도적으로 뺐다 (Play 심사 + SAF 대체).
  기능이 막히면 권한을 추가하는 대신 설계로 우회한다 — 이 제약이 프로젝트의 전제다.
  자세한 근거: `docs/capture-channels.md`
- **분류 앞에 게이트를 두지 말 것.** 텍스트가 있는 이벤트는 예외 없이 전부
  `ClassificationClient.classify()`를 거친다. 사기꾼일수록 뻔한 트리거 단어를 피하므로,
  고정 규칙으로 먼저 걸러내면 정교한 사기일수록 판정기 앞까지 오지 못한다.
- **"분류 실패"를 `RiskSignal.NONE`으로 뭉개지 말 것.** `EventStatus.CLASSIFICATION_FAILED`로
  구분해 기록한다. NONE은 "분류해봤더니 무해함"이지 "분류를 못 함"이 아니다.
- **다채널 격상을 알림 여부의 문으로 쓰지 말 것.** 채널 하나만으로도 `SUSPECTED` + 알림이 나간다.
  다채널은 "얼마나 급한가"를 정하는 강도 조절기다.
- **API 키를 코드나 커밋에 넣지 말 것.** `local.properties` → `buildConfigField` 경로만 쓴다.
- **세션 시간 창은 `capturedAt`(발생 시각)으로 계산한다.** `now()`를 쓰면 STT 때문에 최대 15분
  지연되는 통화 채널이 같은 시각대의 다른 채널과 엮이지 못한다.
- **주석은 "왜"만 쓴다.** 이 코드베이스의 기존 KDoc/주석은 전부 결정의 근거를 적고 있다.
  코드를 읽으면 아는 "무엇"을 반복하지 말 것.

## Architecture

```
app/src/main/java/com/ava/proto/
├── ProtoApplication.kt              # 수동 DI 컨테이너 (by lazy 싱글턴), WorkManager 스케줄링
├── MainActivity.kt                  # Compose 진입점, SAF 폴더 피커, 알림 접근 상태 감시
├── capture/                         # 받아적기만 — 분류를 모른다
│   ├── NotificationCaptureService.kt  # KAKAO·SMS 공용 NotificationListenerService
│   ├── CallRecordingWatcher.kt        # FileObserver(inotify) 실시간 감지
│   ├── RecordingScanWorker.kt         # WorkManager: 폴더 스캔 → STT → 파이프라인
│   ├── RecordingFolder.kt             # SAF tree URI 관리 + 삼성 파일명 패턴 + 경로 변환
│   ├── TargetPackages.kt              # 알림 허용 목록 (카톡 / 자기 자신 / 기본 문자 앱)
│   ├── CapturedEvent.kt               # 캡처 계층의 가공 전 출력
│   └── Channel.kt                     # CALL / SMS / KAKAO
├── stt/                             # 실패 시 null 반환 (예외 아님)
│   ├── AudioTranscriber.kt
│   ├── GroqAudioTranscriber.kt        # whisper-large-v3, multipart
│   └── GeminiAudioTranscriber.kt      # 폴백
├── classification/                  # 실패 시 예외 전파 (null 아님)
│   ├── ClassificationClient.kt
│   ├── GroqClassificationClient.kt    # llama-3.3-70b, JSON 강제
│   ├── LocalKeywordClassificationClient.kt
│   └── KeywordFilter.kt               # 16개 고위험 문구, 키 없을 때만 사용
├── pipeline/DetectionPipeline.kt    # 캡처 → 분류 → 세션의 유일한 조립 지점
├── session/SessionEngine.kt         # 10분 창 다채널 융합, Mutex 직렬화
├── notification/AlertNotifier.kt    # SUSPECTED / ESCALATED 시스템 알림
├── data/                            # Room (events, sessions), 파괴적 마이그레이션
├── demo/DemoInjector.kt             # 자가 알림 + 샘플 음성 복사로 실제 경로 재현
└── ui/                              # HomeScreen(단일 화면) + HomeViewModel
```

`DetectionPipeline`이 유일한 조립 지점이다. 캡처 계층은 이 클래스만 알고 분류가 누구인지 모르며,
분류·세션 로직도 캡처 방식(알림 리스너인지 SAF 스캔인지)을 모른다.
**계층을 가로지르는 직접 호출을 추가하지 말 것** — 새 캡처 소스도 `CapturedEvent`를 만들어
`pipeline.process()`로 넘긴다.

## Tech Stack

- Kotlin 2.2.10 / AGP 9.3.1 (**AGP 9는 Kotlin을 내장하므로 `kotlin.android` 플러그인을 적용하지 않는다**)
- Jetpack Compose + Material 3 (compose-bom 2026.06.01), 단일 Activity
- Room 2.8.4 (KSP), WorkManager 2.11.2, DocumentFile 1.1.0
- HTTP: `HttpsURLConnection` + `org.json` — Retrofit/OkHttp 없음
- DI: 없음. `Application`이 `by lazy`로 싱글턴을 들고 있는 수동 DI (Hilt 도입 안 함)
- minSdk 26 / target·compileSdk 37 / Java 17
- 버전은 전부 `gradle/libs.versions.toml` 버전 카탈로그에서 관리 —
  Compose 컴파일러 플러그인·KSP 버전은 Kotlin 버전과 정확히 일치해야 한다

## Build & Test Commands

**`local.properties`가 필요하다** (gitignore됨, 현재 저장소에 없음). 없으면 빌드가 실패한다:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
GROQ_API_KEY=gsk_...
GEMINI_API_KEY=          # 선택
```

```bash
./gradlew assembleDebug                                   # 디버그 APK 빌드
./gradlew installDebug                                    # 빌드 + 연결된 기기에 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk  # 수동 설치
./gradlew lintDebug                                       # Android Lint
./gradlew clean
adb logcat -s DetectionPipeline GroqClassification GroqAudioTranscriber \
           RecordingScanWorker CallRecordingWatcher NotificationCapture DemoInjector
```

**자동화된 테스트가 없다.** `test`/`androidTest` 소스 세트도, 테스트 의존성도 없다
(`./gradlew test`는 아무것도 실행하지 않고 성공한다). 검증은 기기에서 앱 내 데모 버튼으로 한다:

1. "알림 접근 설정 열기" → AVA Proto 허용 (카톡·SMS 채널 필수)
2. "녹음 폴더 연결" → SAF 폴더 선택 (통화 채널 필수)
3. 카카오톡 데모 → `SUSPECTED`, 이어서 SMS 데모 → 같은 세션 `ESCALATED`
4. 통화 녹음 데모 → 샘플 파일 복사 → FileObserver → STT → 분류 (수 초~수십 초)
5. 오탐 검증: 자동 테스트 버튼이 정상 메시지 ↔ 피싱 메시지를 번갈아 발송하며 결과를 관찰

`SessionEngine`은 `windowMillis`와 `now`를 생성자로 주입받게 되어 있다 —
테스트를 추가한다면 이 지점이 진입점이다.

## Domain Context

- **세션(session)** — 같은 사기 시도에 속한다고 판단된 이벤트들의 묶음. 10분 시간 창으로 정의된다.
- **채널(channel)** — 사기가 도달한 경로. `CALL` / `SMS` / `KAKAO`.
- **counterpart** — 상대방 식별자. SMS·카톡은 알림 제목(발신자), 통화는 **항상 null**
  (`READ_CALL_LOG`를 안 쓰기로 한 결정의 대가). null은 "아직 특정 상대로 안 좁혀짐"이라
  아무 세션에나 합류할 수 있다는 뜻이다.
- **SUSPECTED → ESCALATED** — 채널 1개 신호 → 같은 창의 다른 채널 신호. 하향 전이는 없다.
- 전형적인 한국 보이스피싱 시나리오(금융감독원·검찰청 사칭 → 안전계좌 이체 요구)를 기준으로
  키워드와 데모 시나리오가 구성돼 있다.

## Coding Conventions

- 모든 주석·KDoc·UI 문자열·로그 메시지는 **한국어**. 커밋 메시지는 영어.
- KDoc은 클래스/인터페이스의 **설계 의도와 트레이드오프**를 적는다. 특히
  "왜 이 방식이 아닌가"를 남긴다 (예: 왜 MediaStore가 아닌 SAF인지, 왜 타임스탬프가 아닌 파일명인지).
- 파일 상단에 `private const val TAG = "..."` 를 두고 `android.util.Log`로 로깅.
- 네트워크·파일 I/O는 `withContext(Dispatchers.IO)`.
- 상태는 `StateFlow`로 노출하고 Compose에서 `collectAsStateWithLifecycle()`로 구독.
- Compose는 상태 호이스팅 — `HomeScreen`은 값과 콜백만 받는 무상태 컴포저블이다.
- 새 의존성은 `libs.versions.toml`에 등록한 뒤 `libs.*` 별칭으로 참조. 하드코딩 금지.

## Key Patterns

- **인터페이스 뒤 폴백 체인** — `ClassificationClient` / `AudioTranscriber`는 API 키 유무에 따라
  `ProtoApplication`에서 구현체가 선택된다. 키가 없어도 앱은 동작해야 한다.
- **실패 처리 계약이 계층마다 다르다** — STT는 실패 시 `null`(이벤트는 기록됨),
  분류는 실패 시 **예외**(호출부가 `CLASSIFICATION_FAILED`로 기록). 새 구현체도 이를 따를 것.
- **데모가 실제 경로를 그대로 탄다** — `DemoInjector`는 파이프라인을 직접 호출하지 않고
  Proto 앱 이름으로 알림을 띄워 `NotificationCaptureService`가 되잡게 한다.
  채널 구분은 알림 extras의 `demo_channel` 힌트. 통화 데모도 샘플 파일을 SAF 폴더에 복사할 뿐이다.
  **데모 편의를 위해 파이프라인을 우회하는 코드를 추가하지 말 것.**
- **dedup 키와 저장 값은 같은 함수에서 나온다** — 통화는 `RecordingFolder.identityOf()`,
  알림은 `"${sbn.key}:$text"`. 두 값이 갈라지면 중복 처리나 영구 스킵이 생긴다.
- **동시 진입 지점이 여럿이다** — 알림 서비스의 코루틴들과 WorkManager 워커가 동시에
  `SessionEngine.ingest()`를 부를 수 있어 `Mutex`로 직렬화한다. 이 락을 제거하면 세션이 쪼개진다.

## Reference Docs

- `docs/architecture.md` — 계층 구조·시퀀스·세션 상태 전이 다이어그램(Mermaid), 융합 규칙 표
- `docs/data-model.md` — Room 엔티티/enum 의미론, DAO 쿼리 의도, 마이그레이션 정책
- `docs/capture-channels.md` — 채널별 캡처 방식, 권한 모델과 배제 근거, 삼성 파일명 패턴, SAF 경로 변환
- `docs/external-apis.md` — Groq 분류/STT·Gemini 폴백, API 키 주입 경로, 실패 처리 계약
- `README.md` — 데모 시연 절차와 기기 설정 안내
  (일부 내용이 최신 코드와 어긋난다: 세션 목록 UI는 제거됐고, 분류의 기본 경로는 키워드가 아니라 Groq LLM이다)

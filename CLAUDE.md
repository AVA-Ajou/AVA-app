# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AVA Proto** — 다채널 보이스피싱 탐지 Android 프로토타입.
카카오톡 · SMS · 통화 녹음 세 채널을 동시에 감시하고, 위험 신호가 잡히면 즉시 알리고,
같은 시간 창 안에서 두 채널 이상이 겹치면 경보를 격상시킨다.

단일 모듈 앱(`:app`)이다. 판정은 **파인튜닝한 Gemma를 올린 서버**(`../Detection-Server`)가
맡는다. 앱은 텍스트를 보내고 위험도를 받으며, **통화 음성을 글로 옮기는 일도 같은 서버가
한다** — 모델 하나로 어댑터를 껐다 켜며 세 가지를 처리한다.

**외부 API가 없다.** 예전에는 Groq Whisper(STT)와 Groq LLM(분류)에 매여 있었고 통화 음성이
외부 업체로 나갔는데, 서버 모델(Gemma 4)이 오디오를 직접 받게 되면서 걷어냈다.
서버 주소가 없으면 키워드 대역으로 떨어진다 — 설정이 덜 돼도 앱은 떠야 한다.

```
카카오톡 · SMS · 통화 녹음  ──텍스트──▶  Detection-Server  ──▶  위험도 0~100 (+ 진행 단계)
```

위험도는 모델이 정답 토큰 자리의 로짓에서 읽은 **보정된 확률**이다. 앱은 이 값을 다시
계산하지 않는다 — 근거는 `../Voice-Detection/docs/METHOD.md`.

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
- **서버 주소를 코드나 커밋에 넣지 말 것.** `local.properties` → `buildConfigField` 경로만
  쓴다 (`DETECTION_SERVER_URL`).
- **통화 음성을 외부로 내보내지 말 것.** 권한을 깎아온 이 프로젝트에서 음성을 제3자 API에
  넘기는 것은 앞뒤가 맞지 않는다. 전사는 우리 서버가 한다.
- **우리가 띄운 경보를 되잡지 말 것.** 자기 앱을 알림 감시 대상에 넣은 건 데모 버튼이 실제
  경로를 타게 하려던 것인데, 그 통로로 `AlertNotifier`의 경보까지 돌아온다. 통화 한 건을
  넣었더니 경보 → SMS 이벤트 → 다시 경보로 3초 간격 세 바퀴가 돌아 이벤트가 1건에서 4건으로
  늘었다. `NotificationCaptureService`가 `AlertNotifier.CHANNEL_ID`를 걸러낸다 — 이 필터를
  지우면 모든 측정치가 조용히 오염된다.
- **즉시 스캔은 `RecordingScanWorker.enqueueNow()`로만 넣을 것.** `WorkManager.enqueue()`를
  직접 부르면 파일이 연달아 들어올 때 워커가 동시에 여러 개 뜨고, 같은 목록을 훑어 **같은
  파일을 두 번 판정**한다. 서버는 순전파를 락으로 직렬화하므로 늦은 쪽이 읽기 타임아웃으로
  죽어 `CLASSIFICATION_FAILED` 이벤트가 남는다. 실측으로 확인했다.
- **위험도를 앱에서 다시 계산하지 말 것.** 서버가 준 값은 학습으로 점검된 확률이다.
  `RiskSignal`은 위험도와 단계를 함께 보고 접은 파생값일 뿐이고, 원본은 `EventEntity.risk`에
  남긴다.
- **화면에 나가는 것은 등급 이름 하나뿐이다.** 위험도 숫자도, 진행 단계도, 근거 문장도
  띄우지 않는다. 위험도는 사실상 0 아니면 100으로 갈려 `위험도 100.0`이 "피싱임"과 같은
  말이 되고, 단계는 **등급을 가르는 재료로만 쓴다** — 규칙이 "단계가 있다/없다"는 잘
  가르지만 1단계인지 2단계인지까지 맞다는 보장이 없어, 틀린 단계를 띄우면 사용자가
  "아직 2단계니까 괜찮다"고 읽는다. 근거 문장은 모델이 **가장 결정적인 문구를 못 골라서**
  뺐다(계좌번호를 부르는 대목 대신 "통화가 녹취됩니다"를 뽑아오는 것을 두 번 확인).
  **지우는 게 아니라 화면에서만 뺀 것이다** — `EventEntity`와 `BackendClassification`
  로그에는 그대로 남는다. 경계선 오탐(정상 통화 59.8점 같은 값)은 숫자로만 보이기 때문에
  개발 중에는 볼 수 있어야 한다.
  등급 이름만으로는 순서를 알 수 없으므로 기록 탭의 **`등급 설명` 버튼**이 다섯 등급을
  펼쳐 보여준다(`HistoryTab.TierGuide`). 여기에도 숫자와 단계는 적지 않는다.
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
│   │                                  #   .txt 는 STT를 건너뛰고 내용을 전사본으로 쓴다
│   │   └── enqueueNow()               #   즉시 스캔의 유일한 진입점. 유일 작업으로 줄을 세운다
│   ├── RecordingFolder.kt             # SAF tree URI 관리 + 파일명 패턴 + 경로 변환
│   │                                  #   isCallSource = 삼성 녹음(.m4a) 또는 전사본(.txt)
│   ├── TargetPackages.kt              # 알림 허용 목록 (카톡 / 자기 자신 / 기본 문자 앱)
│   ├── CapturedEvent.kt               # 캡처 계층의 가공 전 출력
│   └── Channel.kt                     # CALL / SMS / KAKAO
├── stt/                             # 실패 시 null 반환 (예외 아님)
│   ├── AudioTranscriber.kt
│   └── ServerAudioTranscriber.kt      # 우리 서버 /transcribe 에 multipart 업로드
├── classification/                  # 실패 시 예외 전파 (null 아님)
│   ├── ClassificationClient.kt        # ClassificationVerdict: riskSignal + risk + stage + reason
│   ├── BackendClassificationClient.kt # 파인튜닝 모델 서버. 2단계 호출(위험도 → 단계·근거)
│   ├── LocalKeywordClassificationClient.kt
│   └── KeywordFilter.kt               # 16개 고위험 문구, 키 없을 때만 사용
├── pipeline/DetectionPipeline.kt    # 캡처 → 분류 → 세션의 유일한 조립 지점
├── session/SessionEngine.kt         # 10분 창 다채널 융합, Mutex 직렬화
├── notification/AlertNotifier.kt    # SUSPECTED / ESCALATED 시스템 알림
├── data/                            # Room (events, sessions), 파괴적 마이그레이션
├── demo/DemoInjector.kt             # 자가 알림으로 실제 경로 재현 (카톡·SMS)
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
- HTTP: `HttpsURLConnection`(외부 API) / `HttpURLConnection`(로컬 서버) + `org.json` —
  Retrofit/OkHttp 없음. 평문 HTTP는 `res/xml/network_security_config.xml`이 허용한 호스트만
- DI: 없음. `Application`이 `by lazy`로 싱글턴을 들고 있는 수동 DI (Hilt 도입 안 함)
- minSdk 26 / target·compileSdk 37 / Java 17
- 버전은 전부 `gradle/libs.versions.toml` 버전 카탈로그에서 관리 —
  Compose 컴파일러 플러그인·KSP 버전은 Kotlin 버전과 정확히 일치해야 한다

## Build & Test Commands

**`local.properties`가 필요하다** (gitignore됨, 현재 저장소에 없음). 없으면 빌드가 실패한다:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
DETECTION_SERVER_URL=http://localhost:8000   # 판정·전사 서버. 비면 키워드 대역으로 떨어진다
```

**서버는 `adb reverse`로 붙인다.** `10.0.2.2` 직결은 맥 방화벽이 TCP를 막아 타임아웃난다
(ICMP는 통과해서 `ping`은 성공하므로 헷갈리기 쉽다).

```bash
adb reverse tcp:8000 tcp:8000
```

```bash
./gradlew assembleDebug                                   # 디버그 APK 빌드
./gradlew installDebug                                    # 빌드 + 연결된 기기에 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk  # 수동 설치
./gradlew lintDebug                                       # Android Lint
./gradlew clean
adb logcat -s DetectionPipeline BackendClassification ServerTranscriber \
           RecordingScanWorker CallRecordingWatcher NotificationCapture DemoInjector
```

**자동화된 테스트가 없다.** `test`/`androidTest` 소스 세트도, 테스트 의존성도 없다
(`./gradlew test`는 아무것도 실행하지 않고 성공한다). 검증은 기기에서 앱 내 데모 버튼으로 한다:

1. "알림 접근 설정 열기" → AVA Proto 허용 (카톡·SMS 채널 필수)
2. "녹음 폴더 연결" → SAF 폴더 선택 (통화 채널 필수).
   **최상위가 아니라 하위 폴더를 고를 것** — 안드로이드가 루트 접근을 막는다
3. 카카오톡 데모 → `SUSPECTED`, 이어서 SMS 데모 → 같은 세션 `ESCALATED`
4. 오탐 검증: 자동 테스트 버튼이 정상 메시지 ↔ 피싱 메시지를 번갈아 발송하며 결과를 관찰

**통화 전사본으로 시험하려면** 연결한 폴더에 `.txt`를 떨어뜨린다. STT를 건너뛰고 파일 내용을
전사본으로 쓰므로, 실기기 녹음 없이 전사 직후 지점부터 실제 경로를 그대로 탄다.

```bash
adb push 통화녹음_테스트.txt /sdcard/Recordings/
# FileObserver 가 0.1초 안에 잡아 즉시 분류로 넘긴다
```

이미 넣어둔 파일을 **다시 판정**하려면 시뮬레이션 탭의 `통화 전사본 [분석]`을 누른다.
평소 스캔은 분석이 끝난 파일을 건너뛰지만 이 버튼은 강제로 전부 다시 본다.

`pm clear`로 기록을 지우면 **SAF 폴더 권한까지 사라진다** — 폴더를 다시 연결해야 한다.

`SessionEngine`은 `windowMillis`와 `now`를 생성자로 주입받게 되어 있다 —
테스트를 추가한다면 이 지점이 진입점이다.

## Domain Context

- **세션(session)** — 같은 사기 시도에 속한다고 판단된 이벤트들의 묶음. 10분 시간 창으로 정의된다.
- **채널(channel)** — 사기가 도달한 경로. `CALL` / `SMS` / `KAKAO`.
- **counterpart** — 상대방 식별자. SMS·카톡은 알림 제목(발신자), 통화는 **항상 null**
  (`READ_CALL_LOG`를 안 쓰기로 한 결정의 대가). null은 "아직 특정 상대로 안 좁혀짐"이라
  아무 세션에나 합류할 수 있다는 뜻이다.
- **SUSPECTED → ESCALATED** — 채널 1개 신호 → 같은 창의 다른 채널 신호. 하향 전이는 없다.
  **상태만 오르고 위험도 숫자는 오르지 않는다** — 다채널 융합을 로그오즈 덧셈으로 바꾸는 것이
  다음 수다 (`../Voice-Detection/docs/METHOD.md` 6절).
- **위험도(risk)** — 모델이 로짓에서 읽은 0~100. 보정돼 있어 87점은 실제로 87% 확률을 뜻한다.
  **화면에는 나오지 않는다** — DB와 로그에만 남는 내부 값이다.
- **세 문턱(80 / 50 / 20)** — 위험도 하나로 자르지 않는다. 모델과 규칙 중 **몇 개가
  위험하다고 보는가**로 등급이 갈린다. 다만 80 위에서는 모델 하나로 충분하다고 본다.

  화면 등급 다섯과 `RiskSignal` 상수 다섯이 일대일로 맞는다 — **등급 판단은 전부
  `BackendClassificationClient.signalOf()` 한 곳에 있고, UI는 이름과 색만 붙인다.**

  | 위험도 | 단계 | 등급 | RiskSignal | 배지 |
  |---|---|---|---|---|
  | 80 이상 | 무관 | 경보 | `HIGH` | 빨강 `경보` |
  | 50~80 | 있음 | 경보 | `HIGH` | 빨강 `경보` |
  | 50~80 | 없음 | 경보우려 | `HIGH_UNBACKED` | 주황 `경보우려` (모델만 봤다) |
  | 20~50 | 있음 | 주의보 | `CAUTION` | 주황 `주의보` (규칙만 봤다) |
  | 20~50 | 없음 | 예보 | `FORECAST` | 노랑 `예보` (어느 쪽도 확신 못 했다) |
  | 20 미만 | — | 정상 | `NONE` | 표시 없음 |

  **배지에는 등급 이름만 나간다.** 단계도 위험도도 붙이지 않는다 — 위 Critical Rules 참조.

  80은 단계 없이도 말을 단정하는 선이다 — 백테스트 100건에서 90 이상이면서 단계가 안 잡힌
  4건이 전부 실제 피싱이었다(투자사기 99.2, 은행사칭 99.0, 카드사사칭 97.6, 기관사칭 92.2).
  그 구간에서 규칙이 못 따라오는 것은 모델이 틀렸다는 뜻이 아니었다. **대신 아래
  건강보험공단 환급금 안내(정상, 99.0)도 이 선 위라 빨간 경보로 나간다** — 그것을 알고
  올린 값이다.

  `경보우려`는 `경보`와 **알림·세션이 완전히 같다.** 낮춘 것은 색과 이름뿐이다.

  20~50은 모델이 애매해하는 자리라 정상과 피싱이 섞인다(실측: 정상 6건 · 피싱 3건). 규칙을
  두 번째 근거로 쓰면 갈린다 — 규칙만 쓰면 정상 통화 10건 중 8건에서 신호가 켜질 만큼
  헐겁지만, **모델이 문턱 위로 준 것에만 적용하니** 검증셋 정상 107건에서 주의보가 한 건도
  나오지 않았다(당시 문턱 33). 50 위에서 단계를 조건으로 걸지 않는 이유는 규칙이 신호를
  못 찾는 피싱이 19%나 되기 때문이다.

  규칙이 못 찾은 나머지를 정상으로 접지 않고 **예보**로 남기면 위 피싱 3건이 알림까지
  받는다. 정상으로 접으면 화면에서도 사라져 무엇을 놓쳤는지 되짚을 수 없다.
  **알림은 정상을 뺀 네 등급 전부에서 나간다** — 예보 구간은 같은 자리의 정상 6건이 함께
  울리지만, 미탐은 돈이 나가고 오탐은 짜증에 그친다는 기준을 여기에도 적용했다.
  `SessionEngine.ingest()`가 걸러내는 것은 `RiskSignal.NONE` 하나뿐이다.

  **앱 `CAUTION_THRESHOLD`(20)와 서버 `ASSESS_THRESHOLD`(20)는 같은 값이어야 한다** —
  서버가 그 아래로는 단계를 계산하지 않으므로, 앱이 더 낮으면 그 사이 구간이 단계를 못 받아
  통째로 예보로 내려앉는다. **문턱을 옮기면 서버를 반드시 함께 고치고 재시작할 것.**
- **진행 단계(stage)** — "지금 어디까지 왔나". **화면에 나가지 않는 내부 판별 재료다** —
  등급을 가르는 데까지만 쓴다.

  | | | 사용자가 할 일 |
  |---|---|---|
  | 1 | 압박·유인 | 끊으면 된다. 아직 아무것도 안 줬다 |
  | 2 | 정보·계좌 | 이미 뭔가 줬을 수 있다. 준 게 뭔지 확인하고 차단 |
  | 3 | 이체 지시 | **지금 당장 멈춰야 한다.** 몇 분 안에 돈이 나간다 |

  서버의 정규식(`../Detection-Server/signals.py`)이 뽑는다. 예전에는 원본 Gemma가 생성으로
  뱉어 한쪽으로 쏠렸다 — 정답지 36건에서 규칙 91.7% / 생성 33.3%.
  **신호가 하나도 안 잡히면 `null`이고, 위험도 50~80이면 등급이 `경보우려`로 내려간다** —
  모델만 위험하다고 본 상태다. 80 위라면 단계 없이도 그냥 `경보`다. 진짜 피싱의 19%가
  단계 없이 올라오므로(검증셋 실측) 어느 쪽이든 **걸러내지는 않는다.**

  단계 번호를 화면에 띄우지 않는 이유는 규칙의 정확도가 **"있다/없다"에 대한 것**이기
  때문이다. 정답지 36건에서 91.7%는 그 판별의 정확도지, 1단계와 2단계를 가려낸 정확도가
  아니다. 틀린 번호를 띄우면 사용자가 "아직 2단계니까 시간이 있다"고 읽는다.
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

- **인터페이스 뒤에서 구현체가 갈린다** — `ClassificationClient` / `AudioTranscriber`는
  `ProtoApplication`이 서버 주소 유무를 보고 고른다. 서버가 없으면 판정은 키워드 대역으로
  떨어지고 전사는 아예 없다(PENDING_TRANSCRIPTION 으로 남는다). 설정이 덜 돼도 앱은 떠야 한다.
- **판정은 서버 호출 한 번이다** — 위험도(순전파)와 단계(정규식)가 한 응답에 함께 온다.
  예전에는 단계가 근거 생성(160토큰, 십수 초)에 딸려 있어 요청을 두 번 보냈고 판정 한 건에
  16초가 걸렸다. 근거 문장은 화면에 쓰지 않으므로 아예 요청하지 않는다 —
  `BackendClassificationClient` 에 두 번째 호출을 되살리지 말 것.
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
- `docs/external-apis.md` — 서버 호출 지점(`/analyze`·`/transcribe`)과 계층별 실패 처리 계약
- `README.md` — 데모 시연 절차와 기기 설정 안내

**다른 저장소** — 판정을 실제로 하는 쪽이다. 앱을 고치기 전에 읽을 것.

- `../Detection-Server/CLAUDE.md` — 앱이 부르는 서버. 어댑터 적재, 프롬프트 계약
- `../Detection-Server/docs/api.md` — `/analyze` 요청·응답 스펙, 단계 정의표
- `../Voice-Detection/docs/METHOD.md` — 위험도를 왜 생성하지 않고 로짓에서 읽는가.
  다채널 로그오즈 융합 설계도 여기 있다
- `../Voice-Detection/docs/RESULTS.md` — 측정된 성능과 **아직 안 되는 것**
  (확률 양극화, 단계 판정 실패, 출처 교란)

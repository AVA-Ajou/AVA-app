# AVA Proto — 다채널 보이스피싱 탐지 데모

카카오톡 · SMS · 통화 녹음 세 채널을 동시에 감시하고, 위험 신호가 잡히면 즉시 알리고,
같은 시간 창 안에서 두 채널 이상이 겹치면 경보를 격상시키는 Android 프로토타입입니다.

---

## 동작 구조

```
[카카오톡 알림]  ──┐
[SMS 알림]       ──┼──→ DetectionPipeline ──→ SessionEngine ──→ AlertNotifier
[통화 녹음 파일]  ──┘         │
                    Detection-Server
                (전사 + 판정, Gemma 4 + LoRA)
```

**다채널은 "알릴지 말지"의 문이 아니라 "얼마나 급한가"의 강도 조절기입니다.**
채널 하나만으로도 `SUSPECTED` + 알림이 나갑니다 — 통화 한 건으로 끝나는 보이스피싱을
놓치지 않기 위한 결정입니다.

### 채널별 캡처 방식

| 채널 | 방식 | 필요 권한 |
|------|------|-----------|
| 카카오톡 | `NotificationListenerService` — 알림 텍스트를 직접 읽음 | 알림 접근 권한 |
| SMS | `NotificationListenerService` — 동일한 서비스로 처리 | 알림 접근 권한 |
| 통화 녹음 | SAF 폴더 감시 + `FileObserver` → 서버 전사 | 폴더 선택(SAF) |

> **카카오톡 주의**: 카카오톡 → 설정 → 알림에서 **"메시지 미리보기"가 켜져 있어야** 알림 텍스트를 읽을 수 있습니다. 꺼져 있으면 내용이 보이지 않습니다.

### 위험 탐지 흐름

1. **캡처** — 알림 수신 또는 녹음 파일 감지
2. **전사** (통화 채널만) — 서버가 음성을 한국어 텍스트로 변환 (`.txt`를 넣으면 건너뜀)
3. **판정** — 서버가 위험도(0~100)와 진행 단계(1~3)를 한 응답으로 돌려줌.
   서버 주소가 없을 때만 로컬 키워드 대역으로 떨어짐
4. **세션 융합** — 10분 시간 창 내 이벤트를 하나의 세션으로 묶음
   - 단일 채널 위험 신호 → `SUSPECTED` + 알림
   - 같은 창 안 다른 채널 신호 → `ESCALATED` + 더 급한 알림
5. **알림** — Android 시스템 알림으로 사용자에게 경보

**텍스트가 있는 이벤트는 예외 없이 전부 판정을 거칩니다.** 키워드로 먼저 걸러내지 않습니다 —
사기꾼일수록 뻔한 트리거 단어를 피하므로, 고정 규칙을 앞에 두면 정교한 사기일수록 판정기
앞까지 오지 못합니다.

---

## 빌드 & 설정

### 사전 요구사항

- Android Studio Hedgehog 이상
- Android SDK 37
- 실기기 또는 에뮬레이터 (API 26+)

### 서버 주소 설정

프로젝트 루트의 `local.properties`에 아래 항목을 추가합니다. (`local.properties`는 `.gitignore`에 포함되어 있어 커밋되지 않습니다.) **외부 API 키는 필요 없습니다** — 판정도 전사도 우리 서버가 합니다.

```properties
sdk.dir=/path/to/your/android/sdk
DETECTION_SERVER_URL=http://localhost:8000
```

탐지 서버를 먼저 띄우고(`../Detection-Server/README.md`), `adb reverse tcp:8000 tcp:8000` 으로 연결합니다.

> 주소가 비어 있으면 판정은 키워드 대역으로 떨어지고 전사는 하지 않습니다. 설정이 덜 돼도 앱은 뜹니다.

### 빌드

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 데모 사용 방법

앱은 네 개의 탭으로 되어 있습니다 — **대시보드 · 기록 · 시뮬레이션 · 설정**.

### 1단계 — 채널 연결 (설정 탭)

#### 통화 녹음 채널
1. **"녹음 폴더 연결"** 버튼을 누릅니다.
2. 파일 탐색기에서 통화 녹음이 저장되는 폴더를 선택합니다.
   - 갤럭시: `내부저장소 › Recordings › Call`
   - **최상위가 아니라 하위 폴더를 고르세요** — 안드로이드가 루트 접근을 막습니다.
3. 한 번 선택하면 앱 재시작 후에도 기억합니다.

#### 문자 · 카카오톡 채널
1. **"알림 접근 설정 열기"** 버튼을 누릅니다.
2. 시스템 설정에서 **AVA Proto**를 찾아 허용합니다.

> `pm clear`로 기록을 지우면 **SAF 폴더 권한까지 사라집니다** — 폴더를 다시 연결해야 합니다.

### 2단계 — 채널별 데모 실행 (시뮬레이션 탭)

#### 카카오톡 데모
- **"카카오톡 피싱"** 행의 `실행`을 누르면 기관 사칭 메시지 수신을 시뮬레이션합니다.
- 즉시 위험 신호가 탐지되어 **기록 탭에 이벤트 카드**가 생기고 시스템 알림이 뜹니다.

#### SMS 데모
- **"SMS 피싱"** 행의 `실행`을 누르면 피싱 문자 수신을 시뮬레이션합니다.
- 카카오톡 데모 직후 누르면 동일 세션에 두 번째 채널이 추가되어 `ESCALATED`로 격상됩니다.

> 두 데모 모두 Proto 앱이 **자기 이름으로 알림을 띄우고** `NotificationCaptureService`가 그걸
> 다시 잡는 방식입니다. 따라서 실제 카카오톡·문자 수신과 완전히 같은 코드 경로를 탑니다.

#### 통화 전사본 분석
- 연결한 폴더에 통화 전사본(`.txt`)을 넣어두고 **"통화 전사본"** 행의 `분석`을 누릅니다.
- `FileObserver`가 새 파일을 즉시 감지하므로, 파일을 방금 넣었다면 버튼을 누르지 않아도
  자동으로 처리됩니다. 이 버튼은 **이미 판정이 끝난 파일까지 강제로 다시** 보는 용도입니다.

```bash
adb push 통화녹음_테스트.txt /sdcard/Recordings/
```

- 실기기 녹음(`Call recording_YYYYMMDD_HHMMSS.m4a`)을 넣으면 서버 전사를 먼저 거칩니다.
- 진행 중엔 로딩 바와 함께 **"AI가 통화 내용을 분석 중..."** 메시지가 표시됩니다.
  파일당 수십 초가 걸릴 수 있고, **"중단"** 버튼으로 취소할 수 있습니다.

### 3단계 — 오탐 검증 (시뮬레이션 탭)

**"오탐 검증 (자동 순환)"** 섹션의 `시작`을 누르면 정상 메시지와 피싱 메시지를 번갈아
발송하며 결과를 관찰할 수 있습니다. 정상 쪽은 택배 · 급여 입금 · 카드 청구 같은,
피싱 키워드가 없는 문장들입니다. 멈출 때까지 순환합니다.

### 결과 확인

기록 탭의 이벤트 카드에 나오는 것은 두 가지입니다.

| | |
|---|---|
| 전사 텍스트 | 통화 내용. `.txt`를 넣었으면 그 내용 그대로 |
| **진행 단계** | `1단계 압박·유인` / `2단계 정보·계좌` / `3단계 이체 지시` |

**위험도 점수는 화면에 나오지 않습니다.** 값이 사실상 0 아니면 100으로 갈려 정보가 없고,
사용자가 할 행동을 정하는 것은 점수가 아니라 단계이기 때문입니다. 원본 값이 필요하면
로그에서 봅니다.

```bash
adb logcat -s DetectionPipeline BackendClassification ServerTranscriber \
           RecordingScanWorker CallRecordingWatcher NotificationCapture DemoInjector
```

위험도가 66을 넘었는데 단계 신호가 하나도 안 잡히면 `경보우려`(주황)로 표시됩니다.
**모델만 위험하다고 본 상태**라는 뜻입니다 — 없는 근거로 단계를 붙이지 않습니다.
다만 진짜 피싱의 19%도 여기 해당하므로 걸러내지는 않습니다.

---

## 실제 기기에서의 동작 (데모 vs 실제)

| 기능 | 데모 모드 | 실제 기기 |
|------|-----------|-----------|
| 카카오톡 감지 | 자가 알림으로 피싱 텍스트 주입 | 실제 카카오톡 알림에서 텍스트 추출 |
| SMS 감지 | 자가 알림으로 피싱 텍스트 주입 | 실제 문자 알림에서 텍스트 추출 |
| 통화 감지 | 폴더에 `.txt`를 넣어 전사 이후 경로만 재현 | 갤럭시 녹음 앱이 저장한 `.m4a` 자동 감지 |
| 전사 | 건너뜀 (`.txt`) 또는 서버 (Gemma 4) | 서버 (Gemma 4) |
| 판정 | 서버 모델 (실제 동작) | 동일 |
| 알림 | 실제 시스템 알림 | 동일 |

---

## 프로젝트 구조

```
app/src/main/java/com/ava/proto/
├── ProtoApplication.kt                # 수동 DI 컨테이너, WorkManager 스케줄링
├── MainActivity.kt                    # Compose 진입점, SAF 폴더 피커
├── capture/
│   ├── NotificationCaptureService.kt  # 카카오톡·SMS 알림 수신
│   ├── CallRecordingWatcher.kt        # FileObserver (실시간 파일 감지)
│   ├── RecordingFolder.kt             # SAF 폴더 관리 + 파일명 패턴
│   ├── RecordingScanWorker.kt         # WorkManager 워커 (전사 → 파이프라인)
│   ├── TargetPackages.kt              # 알림 허용 목록
│   ├── CapturedEvent.kt / Channel.kt
├── stt/
│   ├── AudioTranscriber.kt            # 실패 시 null 반환
│   └── ServerAudioTranscriber.kt      # 서버 /transcribe 업로드
├── classification/                    # 실패 시 예외 전파
│   ├── ClassificationClient.kt
│   ├── BackendClassificationClient.kt # 서버 /analyze (기본 경로)
│   ├── LocalKeywordClassificationClient.kt
│   └── KeywordFilter.kt               # 16개 고위험 문구, 서버 없을 때만
├── pipeline/
│   └── DetectionPipeline.kt           # 캡처 → 분류 → 세션 조립점
├── session/
│   └── SessionEngine.kt               # 다채널 세션 융합 (10분 창)
├── data/                              # Room DB (EventEntity, SessionEntity)
├── notification/
│   └── AlertNotifier.kt               # SUSPECTED / ESCALATED 알림
├── demo/
│   └── DemoInjector.kt                # 자가 알림으로 실제 경로 재현
└── ui/
    ├── HomeScreen.kt                  # 탭 셸
    ├── HomeViewModel.kt               # UI 상태 관리
    ├── DashboardTab.kt / HistoryTab.kt / SimulationTab.kt / SettingsTab.kt
    ├── UiKit.kt                       # 공용 카드·버튼·배지 규격
    └── Theme.kt
```

---

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DB**: Room
- **백그라운드**: WorkManager
- **전사·판정**: 자체 서버 (`../Detection-Server`, Gemma 4 + LoRA). 외부 API 없음
- **알림 캡처**: `NotificationListenerService`
- **파일 감시**: `FileObserver` (Linux inotify) + SAF
- **최소 SDK**: API 26 (Android 8.0)

---

## 더 읽을 것

- `docs/architecture.md` — 계층 구조·시퀀스·세션 상태 전이 다이어그램, 융합 규칙 표
- `docs/data-model.md` — Room 엔티티/enum 의미론, DAO 쿼리 의도
- `docs/capture-channels.md` — 채널별 캡처 방식, 권한 모델과 배제 근거
- `docs/external-apis.md` — 서버 호출 지점과 실패 처리 계약
- `../Detection-Server/docs/api.md` — `/analyze`·`/transcribe` 스펙
- `../Voice-Detection/docs/METHOD.md` — 위험도를 왜 생성하지 않고 로짓에서 읽는가

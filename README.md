# AVA Proto — 다채널 보이스피싱 탐지 데모

카카오톡 · SMS · 통화 녹음 세 채널을 동시에 감시하고, 두 채널 이상에서 위험 신호가 겹치면 알림을 울리는 Android 프로토타입입니다.

---

## 동작 구조

```
[카카오톡 알림]  ──┐
[SMS 알림]       ──┼──→ DetectionPipeline ──→ SessionEngine ──→ AlertNotifier
[통화 녹음 파일]  ──┘         │
                          KeywordFilter
                       (+ Groq Whisper STT)
```

### 채널별 캡처 방식

| 채널 | 방식 | 필요 권한 |
|------|------|-----------|
| 카카오톡 | `NotificationListenerService` — 알림 텍스트를 직접 읽음 | 알림 접근 권한 |
| SMS | `NotificationListenerService` — 동일한 서비스로 처리 | 알림 접근 권한 |
| 통화 녹음 | SAF 폴더 감시 + `FileObserver` → Groq Whisper STT | 폴더 선택(SAF) |

> **카카오톡 주의**: 카카오톡 → 설정 → 알림에서 **"메시지 미리보기"가 켜져 있어야** 알림 텍스트를 읽을 수 있습니다. 꺼져 있으면 내용이 보이지 않습니다.

### 위험 탐지 흐름

1. **캡처** — 알림 수신 또는 녹음 파일 감지
2. **STT** (통화 채널만) — Groq Whisper API로 음성을 한국어 텍스트로 변환
3. **키워드 분류** — 16개 고위험 키워드 매칭 (안전계좌, 수사관, 검찰청, 구속영장 등)
4. **세션 융합** — 10분 시간 창 내 이벤트를 하나의 세션으로 묶음
   - 단일 채널 위험 신호 → `SUSPECTED` + 알림
   - 두 채널 이상 → `ESCALATED` + 긴급 알림
5. **알림** — Android 시스템 알림으로 사용자에게 경보

---

## 빌드 & 설정

### 사전 요구사항

- Android Studio Hedgehog 이상
- Android SDK 37
- 실기기 또는 에뮬레이터 (API 26+)

### API 키 설정

프로젝트 루트의 `local.properties`에 아래 항목을 추가합니다. (`local.properties`는 `.gitignore`에 포함되어 있어 커밋되지 않습니다.)

```properties
sdk.dir=/path/to/your/android/sdk
GROQ_API_KEY=your_groq_api_key_here
```

Groq API 키는 [console.groq.com](https://console.groq.com)에서 무료로 발급받을 수 있습니다.

> Gemini API 키(`GEMINI_API_KEY`)도 지원하며, Groq 키가 없을 때 폴백으로 사용됩니다.

### 빌드

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 데모 사용 방법

앱을 설치하고 실행하면 홈 화면이 나타납니다.

### 1단계 — 채널 연결

#### 통화 녹음 채널
1. **"녹음 폴더 연결"** 버튼을 누릅니다.
2. 파일 탐색기에서 통화 녹음이 저장되는 폴더를 선택합니다.
   - 갤럭시: `내부저장소 › Recordings › Call`
   - 에뮬레이터: 아무 폴더나 선택해도 데모는 동작합니다.
3. 한 번 선택하면 앱 재시작 후에도 기억합니다.

#### 문자 · 카카오톡 채널
1. **"알림 접근 설정 열기"** 버튼을 누릅니다.
2. 시스템 설정에서 **AVA Proto**를 찾아 허용합니다.

### 2단계 — 채널별 데모 실행

홈 화면 하단의 **"채널별 데모"** 섹션에서 채널을 개별 테스트할 수 있습니다.

#### 카카오톡 데모
- **"카카오톡"** 버튼을 누르면 카카오톡 피싱 메시지 수신을 시뮬레이션합니다.
- 즉시 위험 신호가 탐지되어 세션 목록에 `SUSPECTED` 항목이 생깁니다.
- 알림 접근 권한이 있으면 시스템 알림도 함께 표시됩니다.

#### SMS 데모
- **"SMS"** 버튼을 누르면 피싱 문자 수신을 시뮬레이션합니다.
- 카카오톡 데모 직후 누르면 동일 세션에 두 번째 채널이 추가되어 `ESCALATED`로 격상됩니다.

#### 통화 녹음 데모
- 녹음 폴더가 연결된 상태에서 **"통화 녹음"** 버튼을 누릅니다.
- 연결한 폴더에 통화 전사본(`.txt`)을 넣어두고 `통화 전사본 [분석]`을 누르면 판정합니다.
  - 파일명: `Call recording_YYYYMMDD_HHMMSS.m4a` (갤럭시 형식 그대로)
- `FileObserver`가 새 파일을 즉시 감지 → Groq Whisper STT → 키워드 분류 순으로 처리됩니다.
- 진행 중엔 로딩 바와 함께 **"AI가 녹음을 분석 중..."** 메시지가 표시됩니다.
- 분석이 완료되면 이벤트 목록에 전사 텍스트와 탐지된 위험 키워드가 나타납니다.
- 분석 중 **"중단"** 버튼으로 취소할 수 있습니다.

> **샘플 음성 내용**: 금융감독원 사칭 수사관이 대포통장·구속영장을 언급하며 안전계좌 이체를 요구하는 전형적인 보이스피싱 스크립트 (23초, 한국어, macOS `say -v Yuna` 생성)

### 결과 확인

- **세션 목록**: 위험 신호가 탐지된 세션. `SUSPECTED`(1채널) 또는 `ESCALATED`(2채널 이상)
- **최근 이벤트**: 채널별 수신 내용, 전사 텍스트, 매칭된 위험 키워드

---

## 실제 기기에서의 동작 (데모 vs 실제)

| 기능 | 데모 모드 | 실제 기기 |
|------|-----------|-----------|
| 카카오톡 감지 | 버튼으로 피싱 텍스트 주입 | 실제 카카오톡 알림에서 텍스트 추출 |
| SMS 감지 | 버튼으로 피싱 텍스트 주입 | 실제 문자 알림에서 텍스트 추출 |
| 통화 녹음 감지 | 샘플 파일 복사 후 자동 분석 | 갤럭시 녹음 앱이 저장한 파일 자동 감지 |
| STT | Groq Whisper API (실제 AI) | 동일 |
| 키워드 분류 | 로컬 키워드 필터 (실제 동작) | 동일 |
| 알림 | 실제 시스템 알림 | 동일 |

---

## 프로젝트 구조

```
app/src/main/java/com/ava/proto/
├── capture/
│   ├── NotificationCaptureService.kt  # 카카오톡·SMS 알림 수신
│   ├── CallRecordingWatcher.kt        # FileObserver (실시간 파일 감지)
│   ├── RecordingFolder.kt             # SAF 폴더 관리 + 삼성 파일명 패턴
│   └── RecordingScanWorker.kt         # WorkManager 워커 (STT 호출)
├── stt/
│   ├── GroqAudioTranscriber.kt        # Groq Whisper API (기본)
│   └── GeminiAudioTranscriber.kt      # Gemini STT (폴백)
├── classification/
│   ├── KeywordFilter.kt               # 16개 고위험 키워드 매칭
│   └── LocalKeywordClassificationClient.kt
├── pipeline/
│   └── DetectionPipeline.kt           # 캡처 → 분류 → 세션 조립점
├── session/
│   └── SessionEngine.kt               # 다채널 세션 융합 (10분 창)
├── data/                              # Room DB (EventEntity, SessionEntity)
├── notification/
│   └── AlertNotifier.kt               # SUSPECTED / ESCALATED 알림
├── demo/
│   └── DemoInjector.kt                # 채널별 데모 주입
└── ui/
    ├── HomeScreen.kt                  # Compose UI
    └── HomeViewModel.kt               # UI 상태 관리
```

---

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DB**: Room
- **백그라운드**: WorkManager
- **STT**: Groq Whisper large-v3 (REST API, SDK 없음)
- **알림 캡처**: `NotificationListenerService`
- **파일 감시**: `FileObserver` (Linux inotify) + SAF
- **최소 SDK**: API 26 (Android 8.0)

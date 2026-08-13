# 시스템 아키텍처

## 계층 구조

캡처 → 분류 → 세션 융합 → 알림. 각 계층은 인접 계층만 알고, 서로의 구현을 모른다.

```mermaid
flowchart TD
    subgraph capture["capture — 받아적기만 함"]
        NCS["NotificationCaptureService<br/>(KAKAO / SMS)"]
        CRW["CallRecordingWatcher<br/>(FileObserver)"]
        RSW["RecordingScanWorker<br/>(WorkManager)"]
    end

    subgraph stt["stt"]
        AT["AudioTranscriber"]
        GAT["ServerAudioTranscriber<br/>서버 /transcribe"]
    end

    subgraph cls["classification — 판정"]
        CC["ClassificationClient"]
        GCC["BackendClassificationClient<br/>서버 /analyze"]
        LKC["LocalKeywordClassificationClient<br/>(서버 주소 없을 때 대역)"]
    end

    DP["DetectionPipeline<br/>유일한 조립 지점"]
    SE["SessionEngine<br/>10분 창 다채널 융합"]
    AN["AlertNotifier"]
    DB[("Room: events / sessions")]

    NCS --> DP
    CRW -- "CLOSE_WRITE / MOVED_TO" --> RSW
    RSW --> AT
    AT -.-> GAT
    RSW --> DP
    DP --> CC
    CC -.-> GCC
    CC -.-> LKC
    DP --> SE
    SE --> DB
    SE --> AN
```

## 통화 녹음 채널 시퀀스

```mermaid
sequenceDiagram
    participant FS as SAF 폴더
    participant W as CallRecordingWatcher
    participant WM as WorkManager
    participant SW as RecordingScanWorker
    participant STT as ServerAudioTranscriber
    participant P as DetectionPipeline
    participant S as SessionEngine

    FS->>W: CLOSE_WRITE (Call recording_*.m4a 또는 *.txt)
    W->>WM: enqueueNow() — 유일 작업으로 줄 세움
    WM->>SW: doWork()
    SW->>SW: eventDao.analyzedSourceLabels(CALL) 로 dedup
    alt .txt (이미 전사된 통화)
        SW->>SW: 파일 내용을 그대로 전사본으로 사용
    else .m4a
        SW->>STT: transcribe(audioUri)
        STT-->>SW: 전사 텍스트 또는 null
    end
    SW->>P: process(CapturedEvent(text=전사 or null))
    alt text == null
        P->>P: PENDING_TRANSCRIPTION (분류 생략)
    else
        P->>P: classify() → 위험도 + 진행 단계
    end
    P->>S: ingest(EventEntity)
    S->>S: RiskSignal.NONE 이면 기록만, 세션 미형성
```

`enqueueNow()`가 유일한 즉시 실행 진입점이다. `WorkManager.enqueue()`를 직접 부르면 파일이
연달아 들어올 때 워커가 동시에 여러 개 떠 같은 파일을 두 번 판정하고, 서버가 순전파를 락으로
직렬화하므로 늦은 쪽이 읽기 타임아웃으로 죽는다.

`RecordingScanWorker`는 15분 주기(`UNIQUE_WORK_NAME = "recording_scan"`)로도 돌지만, 실사용 경로는
`FileObserver`의 즉시 실행이다. 주기 실행은 앱이 죽어 있던 동안 쌓인 파일을 뒤늦게 줍는 안전망이다.

## 세션 상태 전이

```mermaid
stateDiagram-v2
    [*] --> SUSPECTED: 첫 위험 신호 (채널 1개)<br/>notifySuspected()
    SUSPECTED --> ESCALATED: 같은 창 안 다른 채널 신호<br/>notifyEscalation()
    ESCALATED --> ESCALATED: 추가 신호 (알림 재발송 없음)
```

**상태는 둘뿐이고 하향 전이가 없다.** 예전에는 "백엔드 확정" 자리로 `ALERT`를 하나 더 뒀는데,
그 확정이라는 것이 결국 모델 판정이고 그건 이미 `SUSPECTED`를 만든 근거였다 — 도달할 경로가
없는 값이 전이 코드에 분기만 하나 더 만들고 있어 걷어냈다.

핵심 설계 의도: **다채널 여부는 "알릴지 말지"의 문이 아니라 "얼마나 급하게 알릴지"의 강도 조절기다.**
채널 하나만으로도 즉시 `SUSPECTED` + 알림이 나간다. 통화 한 건으로 끝나는 보이스피싱을 놓치지 않기 위한 결정.

## 세션 융합 규칙 (`SessionEngine.fuseIntoSession`)

| 항목 | 규칙 | 이유 |
|------|------|------|
| 시간 창 기준 | `event.capturedAt` (발생 시각), `now()` 아님 | 통화 채널은 STT 때문에 최대 15분 지연 — 처리 시각을 쓰면 실제로 가까웠던 이벤트가 안 엮인다 |
| 창 연장 | `maxOf(기존 windowExpiresAt, capturedAt + 10분)` | 뒤늦게 처리된 과거 이벤트가 이미 늘어난 창을 되레 줄이면 안 됨 |
| 동시성 | `Mutex`로 "활성 세션 조회 → 갱신/생성" 직렬화 | 없으면 두 이벤트가 각자 "활성 세션 없음"으로 보고 세션을 쪼개 격상이 안 일어남 |
| 상대 스코프 | `counterpart`가 있으면 같은 상대 또는 아직 안 좁혀진 세션에만 합류 | 무관한 두 사람이 시간만 겹쳐 한 세션으로 섞이는 것 방지 |
| 통화의 counterpart | 항상 `null` → 아무 활성 세션에나 합류 | `READ_CALL_LOG`를 쓰지 않기로 한 결정의 대가 |

## 데모 경로가 실제 경로와 같은 이유

`DemoInjector`는 **파이프라인을 아예 들고 있지 않다** — 생성자로 받던 참조를 한 번도 쓰지 않아
걷어냈다. 알림을 띄우는 것 말고 탐지 계층으로 가는 통로가 없어야, 데모 편의를 위해 파이프라인을
우회하는 코드가 나중에 슬그머니 끼어들 수 없다.

`injectKakao()/injectSms()`는 파이프라인을 직접 호출하지 않는다.
**Proto 앱이 자기 이름으로 Android 알림을 띄우고**, `NotificationCaptureService`가 그걸 다시 잡는다.
따라서 데모 버튼도 실제 카카오톡·문자 수신과 완전히 동일한 코드 경로를 탄다.
채널 구분은 알림 extras의 `demo_channel` 힌트로 한다.

통화 채널에는 주입할 것이 없다. 사용자가 연결한 폴더에 파일을 넣으면 `FileObserver`가 잡고,
시뮬레이션 탭의 `통화 전사본 [분석]`은 그 폴더를 **강제로 다시** 훑을 뿐이다
(`RecordingScanWorker`의 `KEY_FORCE`).

`.txt`는 이미 전사된 통화로 보고 STT를 건너뛴다 — 실기기 녹음 없이 전사 직후 지점부터
실제 경로를 그대로 태우기 위한 통로다. 예전에는 앱에 박힌 샘플 음성(`res/raw/demo_call.m4a`)을
복사했는데, 빈 파일이 먼저 노출되어 STT가 실패하고 그대로 굳는 문제가 있어 걷어냈다.

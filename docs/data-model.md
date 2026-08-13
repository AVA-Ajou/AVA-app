# 데이터 모델 (Room)

DB 이름 `proto.db`, 현재 version **3**, `exportSchema = true` → `app/schemas/com.ava.proto.data.AppDatabase/*.json`.
(3에서 `risk` / `stage` / `stageLabel`이 붙었다 — 모델 서버 판정 경로.)

마이그레이션은 작성하지 않는다 — `fallbackToDestructiveMigration(dropAllTables = true)`.
실사용자 데이터가 없는 프로토타입 단계의 의도적 선택이다. 엔티티를 바꾸면 `version`만 올리면 되고,
기기의 기존 데이터는 날아간다.

## `events` — `EventEntity`

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `id` | Long PK | autoGenerate |
| `channel` | `Channel` | `CALL` / `SMS` / `KAKAO` |
| `capturedAt` | Long | SMS·카톡은 `sbn.postTime`, 통화는 파일 `lastModified()` |
| `sourceLabel` | String | 알림 발신 패키지명 또는 녹음 파일명 |
| `text` | String? | null = 아직 분석할 텍스트 없음 (STT 대기) |
| `status` | `EventStatus` | 아래 참조 |
| `riskSignal` | `RiskSignal` | `NONE` / `LOW` / `HIGH` |
| `matchedPhrase` | String? | 키워드 대역은 매칭 문구, 서버 경로는 `stage_evidence[0]`(규칙이 그 단계를 매긴 원문 인용구). 화면에는 안 나오고 오판을 되짚을 때만 쓴다 |
| `sessionId` | Long? | 세션에 합류하지 않은 이벤트는 null |
| `audioUri` | String? | 통화 채널의 **실제 녹음**만. `.txt` 전사본은 재생할 오디오가 없어 null |
| `counterpart` | String? | 알림 `EXTRA_TITLE`(발신자). 통화는 항상 null |
| `risk` | Double? | 모델이 로짓에서 읽은 보정된 0~100. 키워드 대역에서는 null |
| `stage` | Int? | 진행 단계 1~3. 서버 정규식이 신호를 못 찾으면 null |
| `stageLabel` | String? | `압박·유인` / `정보·계좌` / `이체 지시` |

`audioUri`는 **아직 읽는 코드가 없다** — 재생 화면이 없기 때문이다. 그래도 남기는 이유는
오판을 되짚을 때 "그 판정이 어느 파일에서 나왔나"를 아는 유일한 값이라서다. `sourceLabel`은
파일명뿐이라 폴더를 바꾸면 같은 이름이 겹칠 수 있다.

### `EventStatus`의 세 값이 각각 다른 이유

- `ANALYZED` — 텍스트가 있었고 분류도 끝났다.
- `PENDING_TRANSCRIPTION` — 원본은 있는데 텍스트가 없다 (STT 미설정/실패).
- `CLASSIFICATION_FAILED` — 텍스트는 있었는데 분류 호출 자체가 실패했다 (네트워크 오류 등).

`CLASSIFICATION_FAILED`를 `riskSignal = NONE`으로 뭉개면 안 된다.
`NONE`은 "분류해봤더니 무해함"이라는 뜻이지 "분류를 못 함"이 아니다.
이 구분이 사라지면 과거 게이트 설계의 모호함("NONE = 분석 안 됨")이 조용히 되돌아온다.

### `RiskSignal.LOW`

서버 경로는 40~70 구간을 `LOW`로 내려보내지만 **화면에서는 `NONE`과 구분되지 않는다.**
피싱 여부는 70을 기준으로만 가르기 때문이다. 키워드 폴백은 `HIGH`/`NONE`뿐이다.

`risk`(0~100 원본)는 계속 저장되지만 **화면에는 나오지 않는다.** 값이 사실상 0 아니면
100으로 갈려 정보가 없고, 사용자가 할 행동은 진행 단계가 정한다. 다만 경계선 오탐
(정상 통화 59.8점 같은 값)은 이 컬럼으로만 보이므로 지우지 말 것.

## `sessions` — `SessionEntity`

| 컬럼 | 타입 | 비고 |
|------|------|------|
| `id` | Long PK | 알림 ID로도 재사용됨 (`notify(session.id.toInt(), ...)`) |
| `state` | `SessionState` | `SUSPECTED` / `ESCALATED` |
| `createdAt` / `updatedAt` | Long | 처리 시각(`now()`) 기준 |
| `windowExpiresAt` | Long | `capturedAt + 10분`, 단조 증가만 |
| `channelsInvolved` | `Set<Channel>` | `Converters`가 콤마 문자열로 저장 |
| `counterpart` | String? | null = 아직 특정 상대로 안 좁혀짐 → 누구든 합류 가능 |

`SessionState`는 두 값뿐이고 하향 전이가 없다. 예전에는 "백엔드 확정" 자리로 `ALERT`를 하나 더
뒀는데, 도달할 경로가 없는 값이 상태 전이 코드에 분기만 만들고 있어 걷어냈다.

세션에는 `observe…` 류 쿼리가 없다 — 화면에 목록으로 나오지 않기 때문이다. 사용자가 읽는 것은
이벤트 카드와 시스템 알림이고, 세션은 그 뒤에서 다채널 격상을 판단하는 상태일 뿐이다.

## DAO 쿼리 중 주의할 것

### `SessionDao.findActive(referenceTime, counterpart)`

```sql
WHERE windowExpiresAt > :referenceTime
  AND (:counterpart IS NULL OR counterpart IS NULL OR counterpart = :counterpart)
ORDER BY updatedAt DESC LIMIT 1
```

3항 OR이 핵심이다. 통화(counterpart null)는 아무 활성 세션에나 붙고,
식별자가 있는 이벤트는 같은 상대이거나 아직 안 좁혀진 세션에만 붙는다.

### `EventDao.analyzedSourceLabels(CALL)`

```sql
SELECT sourceLabel FROM events
WHERE channel = :channel AND status != 'PENDING_TRANSCRIPTION'
```

통화 녹음 dedup을 **파일명**으로 한다. 타임스탬프 워터마크를 쓰지 않는 이유:
SAF 제공자 등 mtime 해상도가 낮은 저장소에서 두 파일이 완전히 같은 수정 시각을 가질 수 있고,
그 상태에서 워터마크 비교를 하면 뒤 파일이 "워터마크보다 크지 않다"는 이유로 영구히 스킵된다.
같은 판별값을 `RecordingFolder.identityOf()`가 만들고 `sourceLabel`에 그대로 저장한다 —
dedup 키와 기록 값이 갈라지면 안 된다.

**`PENDING_TRANSCRIPTION`을 제외하는 것이 이 쿼리의 핵심이다.** 그건 "아직 안 끝났다"는
뜻인데, 예전에는 상태를 안 보고 파일명만 봐서 STT가 한 번 실패하면 그 통화가 영영 다시
시도되지 않았다. 파일을 쓰는 도중에 잡아 실패한 경우도 마찬가지로 굳어버렸다.

시뮬레이션 탭의 `통화 전사본 [분석]`은 이 목록을 아예 비운다(`RecordingScanWorker.KEY_FORCE`) —
이미 판정이 끝난 파일도 강제로 다시 본다.

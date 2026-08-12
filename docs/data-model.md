# 데이터 모델 (Room)

DB 이름 `proto.db`, 현재 version **2**, `exportSchema = true` → `app/schemas/com.ava.proto.data.AppDatabase/*.json`.

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
| `matchedPhrase` | String? | 키워드 대역은 매칭 문구, 서버 경로는 근거에서 뽑은 원문 인용 |
| `sessionId` | Long? | 세션에 합류하지 않은 이벤트는 null |
| `audioUri` | String? | 통화 채널만 |
| `counterpart` | String? | 알림 `EXTRA_TITLE`(발신자). 통화는 항상 null |

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
| `state` | `SessionState` | `SUSPECTED` / `ESCALATED` / `ALERT` |
| `createdAt` / `updatedAt` | Long | 처리 시각(`now()`) 기준 |
| `windowExpiresAt` | Long | `capturedAt + 10분`, 단조 증가만 |
| `channelsInvolved` | `Set<Channel>` | `Converters`가 콤마 문자열로 저장 |
| `counterpart` | String? | null = 아직 특정 상대로 안 좁혀짐 → 누구든 합류 가능 |

`SessionState.ALERT`는 백엔드 확정 분류용으로 정의만 되어 있고 현재 코드 경로로는 도달하지 않는다.

## DAO 쿼리 중 주의할 것

### `SessionDao.findActive(referenceTime, counterpart)`

```sql
WHERE windowExpiresAt > :referenceTime
  AND (:counterpart IS NULL OR counterpart IS NULL OR counterpart = :counterpart)
ORDER BY updatedAt DESC LIMIT 1
```

3항 OR이 핵심이다. 통화(counterpart null)는 아무 활성 세션에나 붙고,
식별자가 있는 이벤트는 같은 상대이거나 아직 안 좁혀진 세션에만 붙는다.

### `EventDao.sourceLabelsForChannel(CALL)`

통화 녹음 dedup을 **파일명**으로 한다. 타임스탬프 워터마크를 쓰지 않는 이유:
SAF 제공자 등 mtime 해상도가 낮은 저장소에서 두 파일이 완전히 같은 수정 시각을 가질 수 있고,
그 상태에서 워터마크 비교를 하면 뒤 파일이 "워터마크보다 크지 않다"는 이유로 영구히 스킵된다.
같은 판별값을 `RecordingFolder.identityOf()`가 만들고 `sourceLabel`에 그대로 저장한다 —
dedup 키와 기록 값이 갈라지면 안 된다.

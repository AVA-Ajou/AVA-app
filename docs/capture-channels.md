# 캡처 채널과 권한 모델

## 세 채널

| 채널 | 진입점 | 트리거 | 필요한 사용자 조치 |
|------|--------|--------|--------------------|
| `KAKAO` | `NotificationCaptureService` | 카카오톡 알림 posted | 시스템 설정 → 알림 접근 허용 |
| `SMS` | `NotificationCaptureService` | 기본 문자 앱 알림 posted | 동일 (같은 서비스) |
| `CALL` | `CallRecordingWatcher` → `RecordingScanWorker` | 녹음 파일 `CLOSE_WRITE`/`MOVED_TO` | SAF 폴더 선택 |

## 선언하지 않은 권한과 그 이유

`AndroidManifest.xml`이 요청하는 건 `POST_NOTIFICATIONS`와 `INTERNET` 둘뿐이다.
아래는 **의도적으로 뺀 것**이므로, 편의를 위해 다시 추가하지 말 것:

- `READ_CALL_LOG`, `RECEIVE_SMS`, `READ_SMS` — 기본 전화/문자 앱 자격이 없으면 Play 심사를 통과하지 못한다.
  SMS는 알림 리스너로 대체했고, 통화 발신번호는 포기했다(그래서 `CALL` 이벤트의 `counterpart`는 항상 null).
- `MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_AUDIO` — SAF 트리 권한(`OpenDocumentTree` +
  `takePersistableUriPermission`)으로 대체했다. 저장소 권한 선언 자체가 불필요하다.

`BIND_NOTIFICATION_LISTENER_SERVICE`는 앱이 요청하는 권한이 아니라 사용자가 시스템 설정에서
직접 켜야 바인딩되는 것이다. 코드로 요청할 방법이 없으므로 UI는
`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`로 안내만 하고,
`NotificationManagerCompat.getEnabledListenerPackages()`로 상태를 `ON_RESUME`마다 재확인한다.

## 알림 캡처 세부

**허용 목록**(`TargetPackages.allowlist`) 밖 알림은 `onNotificationPosted` 진입 즉시 버려지고
어디에도 기록되지 않는다. 목록은 3개:

- `com.kakao.talk` (고정)
- `com.ava.proto` (자기 자신 — 데모 알림 자가 수신용)
- `Telephony.Sms.getDefaultSmsPackage(context)` — 문자 앱 패키지명은 제조사마다 다르므로
  사용자에게 고르게 하지 않고 시스템에 물어본다

**텍스트 추출**은 `EXTRA_BIG_TEXT` → `EXTRA_TEXT` 순으로 폴백하고 둘 다 없으면 건너뛴다.
카카오톡에서 "메시지 미리보기"가 꺼져 있으면 둘 다 비어 있어 아무것도 캡처되지 않는다 — 데모 전 필수 확인 사항.

**중복 제거**는 `"${sbn.key}:$text"` 키를 `LinkedHashSet`(상한 200)에 넣어 판별한다.
시스템은 기존 알림이 갱신될 때도 같은 콜백을 다시 부르므로, 이게 없으면 한 메시지가 여러 번 처리된다.

이 서비스는 **분류하지 않는다**. 받아적어 `DetectionPipeline`에 넘기는 것까지가 책임이다.

## 통화 녹음 파일 감지

### 삼성 파일명 패턴

```kotlin
"""^(Call recording|통화[ _]?녹음)[_ ]\d{8}[_ ]\d{6}.*\.m4a$"""  // IGNORE_CASE
```

기기 언어 설정에 따라 영문/한글 두 형태가 모두 나올 수 있어 둘 다 허용한다.

`RecordingFolder.isCallSource()`가 `FileObserver` 필터와 폴더 스캔 필터 양쪽에서 **같은 판정**을
쓴다 — 갈라지면 즉시 감지는 되는데 스캔에서는 안 보이는(또는 그 반대) 버그가 난다.
이 함수는 위 패턴에 더해 **`.txt`도 통과시킨다.**

### `.txt` — 이미 전사된 통화

파일 내용이 곧 전사본이라 STT를 건너뛴다. 실기기 녹음 없이 탐지 경로 전체를 태워보려고 둔
통로다 — 음성을 넣으면 서버 전사를 거쳐야 하는데, 그 단계는 대개 지금 확인하려는 부분이
아니고 실제 녹음과 수십 초가 필요하다.

```bash
adb push 통화녹음_테스트.txt /sdcard/Recordings/
# FileObserver 가 0.1초 안에 잡아 즉시 분류로 넘긴다
```

이때 `audioUri`는 null로 남는다(재생할 오디오가 없다).

### SAF tree URI → 파일시스템 경로

`FileObserver`는 URI가 아니라 실제 경로가 필요하므로 `RecordingFolder.getAsPath()`가 변환한다.
`DocumentsContract.getTreeDocumentId()`의 `"primary:Recordings/Call"` 형태를 파싱해
`/storage/emulated/0/...`로 만든다.

**`primary` 볼륨만 지원한다.** SD카드 등 외부 볼륨은 `null`을 반환하고, 이때 실시간 감시는 동작하지 않는다
(15분 주기 `RecordingScanWorker`만 남는다). 폴더 스캔 자체는 `DocumentFile`로 하므로 볼륨과 무관하게 동작한다.

### 폴더 변경 시

`RecordingFolder.save()` 직후 반드시 `ProtoApplication.restartWatcher()`를 불러야 새 폴더가 감시된다.
`MainActivity`의 `folderPicker` 콜백이 이 순서를 지키고 있다.

## MediaStore를 쓰지 않는 이유

삼성이 통화 녹음을 `MediaStore.Audio`에 등록하지 않는 경우가 있어(에뮬레이터에서 실측 확인됨),
색인 여부와 무관하게 폴더 안 파일을 `DocumentFile.listFiles()`로 직접 훑는다.
이 결정이 저장소 권한을 통째로 없앨 수 있었던 근거이기도 하다.

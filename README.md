# Stock Broadcast Catch-up App

주식 유튜브 라이브 방송을 보다가 잠깐 놓친 내용을 쉽게 복구하기 위한 Android 태블릿용 캐치업 앱입니다.

이 앱은 단순 STT 앱이 아니라, 사용자가 주식방송을 틀어두고 잠깐 자리를 비우거나 딴짓을 하더라도:

- 지금 무슨 이야기를 하는지
- 방금 1분 동안 무슨 내용이 나왔는지
- 방금 5분 동안 핵심 내용이 무엇인지
- 방송 종료 후 어떤 주제와 종목 이야기가 나왔는지

를 빠르게 복구하고 정리할 수 있도록 돕는 것을 목표로 합니다.

---

## 1차 목표

- Android 태블릿 우선 개발
- iOS는 추후 고려
- 부모님과 지인도 쉽게 사용할 수 있는 단순 UI
- Google Play 내부 테스트 링크 배포
- 공개 출시 전 가족/지인 테스트 진행
- Codex 기반 개발 워크플로우 적용

---

## 핵심 기능

1. 방송 듣기 시작
2. 현재 무슨 이야기를 하는지 한 줄 요약
3. 실시간 자막 최근 내용 표시
4. 방금 1분 요약
5. 방금 5분 요약
6. 종료 후 전체 요약
7. 방송 기록 저장
8. 저신뢰 구간 표시
9. 주식방송 용어 및 종목명 보정
10. 현재 지수는 기본 화면에서 숨기고 원할 때만 표시

---

## 사용자 경험(UX) 방향

기본 화면은 복잡한 대시보드가 아니라 매우 단순해야 합니다.

### 기본 화면에 보여줄 것

- 현재 듣는 중 상태
- 지금 무슨 얘기?
- 실시간 자막 최근 3줄
- 방금 1분 요약 버튼
- 종료하고 정리 버튼
- 기록 보기 버튼
- 현재 지수 보기 버튼

### 기본 화면에서 숨길 것

- 현재 지수 상세 카드
- 언급 종목 전체 리스트
- 복잡한 설정
- 기술 용어(WebSocket, MediaProjection 등)

---

## 입력 방식

입력 소스는 추상화 구조로 설계합니다.

우선순위:

1. Android 내부 오디오 캡처
2. 마이크 입력 fallback
3. 외장 마이크 / USB 오디오 / PC companion은 추후 확장
4. 텍스트 입력 기반 테스트 소스는 개발용으로 지원

예상 구조:

```kotlin
interface InputSource {
    suspend fun start()
    suspend fun stop()
}
```

예상 구현:

- FakeInputSource
- MicInputSource
- AndroidPlaybackCaptureSource
- TextInputSource

---

## 기술 스택

- Android
- Kotlin
- Jetpack Compose
- MVVM
- StateFlow
- Room DB
- Backend WebSocket
- GitHub + Codex Workflow

보안 원칙:

- API Key는 Android 앱에 저장하지 않음
- 서버 비밀값은 backend/env에서만 관리
- Public 저장소에는 비밀값 커밋 금지

---

## 개발 단계

### Phase 0: Fake MVP

실제 오디오, STT, 백엔드 없이 가짜 방송 텍스트 기반으로 UI 흐름을 구현합니다.

포함 화면:

- 기본 라이브 화면
- 방금 1분 요약 화면
- 현재 지수 화면
- 기록 화면
- 방송 상세 화면

목표:

- 부모님도 이해 가능한 단순 UX 검증
- 화면 흐름 검증
- 상태 관리 구조 검증

---

### Phase 1: 입력 구조 추상화

- InputSource interface 추가
- FakeInputSource 구현
- MicInputSource placeholder
- AndroidPlaybackCaptureSource placeholder
- TextInputSource placeholder

---

### Phase 2: 마이크 입력

- RECORD_AUDIO 권한
- 마이크 오디오 캡처
- 오디오 품질 경고
- 저신뢰 구간 처리

---

### Phase 3: Android 내부 오디오 캡처

- MediaProjection 권한
- 내부 오디오 캡처
- 유튜브 재생음 캡처
- 실패 시 마이크 입력 fallback

---

### Phase 4: Backend WebSocket

- 실시간 STT
- 실시간 한 줄 요약
- 최근 1분/5분 요약
- 종료 후 전체 요약

---

### Phase 5: 주식방송 보정

- 금융 용어 사전
- 종목명 보정
- 방송 맥락 기반 보정
- 원문과 보정문 둘 다 저장
- 숫자는 확실할 때만 보정

---

### Phase 6: 테스트 배포

- Google Play Internal Testing
- 부모님/지인 테스트 링크 배포
- 피드백 수집
- 공개 출시 여부 판단

---

## 첫 번째 Codex 작업 목표

처음 Codex 작업은 실제 오디오 기능이 아니라 Fake MVP UI 구현입니다.

Codex에게 맡길 첫 작업:

```text
Build the Phase 0 Fake MVP for an Android Kotlin Jetpack Compose app.
Do not implement real audio, STT, backend, or API calls yet.
Use fake stock broadcast data.
Implement a simple parent-friendly UI with:
- Main live screen
- Recent 1-minute summary screen
- Optional current index screen
- History screen
- Detail summary screen
Use MVVM and StateFlow.
Keep the architecture ready for InputSource abstraction later.
```

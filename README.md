# Stock Broadcast Catch-up App

## 목적

주식 유튜브 라이브 방송을 보다가 잠깐 놓친 내용을 쉽게 복구하기 위한 Android 앱입니다.

사용자는 Android 태블릿에서 방송을 재생하고, 앱은 방송 음성을 듣거나 내부 오디오를 캡처하여 실시간 STT, 현재 주제 요약, 방금 1분/5분 요약, 종료 후 전체 정리를 제공합니다.

## 1차 목표

- Android 태블릿 전용
- iOS는 1차 버전에서 제외
- 부모님과 지인이 쉽게 사용할 수 있는 단순 UI
- Google Play 내부 테스트 링크로 배포
- 공개 출시 전 가족/지인 테스트 먼저 진행

## 핵심 기능

1. 방송 듣기 시작
2. 현재 무슨 이야기를 하는지 한 줄 요약
3. 실시간 자막 표시
4. 방금 1분 요약
5. 방금 5분 요약
6. 종료 후 전체 정리
7. 기록 저장
8. 저신뢰 구간 표시
9. 주식방송 용어 보정
10. 현재 지수는 기본 화면에서 숨기고, 원할 때만 보기

## 입력 방식

우선순위:

1. Android 내부 오디오 캡처
2. 마이크 입력 fallback
3. 외장 마이크 / USB 오디오 / PC companion은 추후 확장

## UI 방향

기본 화면은 매우 단순해야 합니다.

기본 화면에 보일 것:

- 듣는 중 상태
- 지금 무슨 얘기?
- 실시간 자막 최근 3줄
- 방금 1분 요약 버튼
- 종료하고 정리 버튼
- 기록 보기 버튼
- 현재 지수 보기 버튼은 선택 기능

기본 화면에는 너무 많은 정보, 지수 카드, 종목 리스트, 복잡한 설정을 넣지 않습니다.

## 기술 스택

- Android
- Kotlin
- Jetpack Compose
- MVVM
- StateFlow
- Room DB
- Backend WebSocket
- API Key는 Android 앱에 저장하지 않음

## 개발 단계

### Phase 0: Fake MVP

- 실제 오디오 없이 가짜 방송 텍스트로 UI 구현
- 기본 화면 구현
- 방금 1분 요약 화면 구현
- 현재 지수 선택 화면 구현
- 기록 화면 구현
- 상세 요약 화면 구현

### Phase 1: 입력 구조 추상화

- InputSource interface 추가
- FakeInputSource 추가
- MicInputSource placeholder
- AndroidPlaybackCaptureSource placeholder

### Phase 2: 마이크 입력

- RECORD_AUDIO 권한
- 마이크 오디오 캡처
- 오디오 품질 경고

### Phase 3: Android 내부 오디오 캡처

- MediaProjection 권한
- 내부 오디오 캡처
- 실패 시 마이크 입력으로 전환

### Phase 4: Backend WebSocket

- 실시간 STT
- 실시간 요약
- 최근 1분/5분 요약
- 종료 후 전체 요약

### Phase 5: 주식방송 보정

- 금융 용어 사전
- 종목명 보정
- 원문과 보정문 둘 다 저장
- 숫자는 확실할 때만 보정

### Phase 6: 테스트 배포

- Google Play Internal Testing
- 부모님/지인에게 링크 배포
- 피드백 수집
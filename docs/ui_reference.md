# UI Reference - Simple Parent-Friendly MVP

이 문서는 사용자가 제공한 UI mockup 이미지를 바탕으로 Phase 0 앱 화면 구현 기준을 정리한 것입니다.

목표는 복잡한 금융 대시보드가 아니라, 부모님도 바로 이해할 수 있는 단순한 주식방송 캐치업 앱입니다.

---

## 전체 디자인 방향

- Dark theme 기반
- 배경은 짙은 남색/검정 계열
- 주요 액센트는 teal/cyan 계열
- 카드형 UI 중심
- 큰 글자와 넓은 여백 사용
- 기본 화면에는 꼭 필요한 정보만 표시
- 현재 지수는 기본 화면에서 숨기고, 사용자가 원할 때만 표시

---

## 1. 기본 화면

상단:

- 제목: `주식방송 캐치업`
- 상태 pill: `● 듣는 중 00:12:34`

메인 카드:

- 제목: `지금 무슨 얘기?`
- 내용 예시:
  - `반도체, 엔비디아 실적, 금리 부담을 이야기하는 중입니다.`

실시간 자막 카드:

- 제목: `실시간 자막`
- 최근 3줄만 표시
- 예시:
  - `00:12:31  반도체 업종이 강세를 보이고 있어요.`
  - `00:12:26  엔비디아 실적이 시장 기대를 상회했습니다.`
  - `00:12:21  금리 부담이 지속되며 변동성이 커지고 있습니다.`

선택 기능:

- `현재 지수 보기` 버튼은 작게 표시
- 지수 상세 정보는 기본 화면에 노출하지 않음

하단 주요 버튼:

- `방금 1분 요약`
- `종료하고 정리`
- `기록 보기`

---

## 2. 놓친 내용 요약 화면

상단:

- 제목: `방금 1분 요약`
- 뒤로가기 버튼

요약 카드:

- 4개 정도의 bullet point
- 부모님도 이해할 수 있는 자연스러운 문장 사용

예시:

- 반도체 업종이 강세를 보이며 시장 주도 흐름을 이끌고 있어요.
- 엔비디아 실적이 시장 기대를 상회해 투자 심리가 개선되었습니다.
- 금리 부담이 지속되며 변동성이 커지고 있습니다.
- 단기적으로는 실적과 금리 방향에 따라 등락이 이어질 수 있어요.

하단 안내:

- `일부 구간은 소음으로 누락될 수 있어요`

하단 버튼:

- `라이브로 돌아가기`

---

## 3. 현재 지수 화면

상단:

- 제목: `현재 지수`
- 보조 문구: `원할 때만 보기`

지수 카드 4개:

- 코스피
- 코스닥
- 나스닥
- USD/KRW

각 카드 구성:

- 지수명
- 큰 숫자
- 등락률

예시:

- `코스피 2,655.42 ▲ 0.68%`
- `코스닥 856.21 ▲ 0.62%`
- `나스닥 17,689.36 ▲ 0.91%`
- `USD/KRW 1,382.10 ▼ 0.21%`

하단 버튼:

- `닫기`

---

## 구현 우선순위

1. 현재 MainActivity의 Phase 0 Fake MVP 화면을 이 문서 기준으로 조금씩 다듬는다.
2. 실시간 기능을 붙이기 전에 화면 간 이동과 버튼 흐름을 먼저 안정화한다.
3. 현재 지수는 반드시 선택 화면으로 유지한다.
4. 기본 화면이 복잡해지지 않도록 한다.
5. 나중에 실제 STT와 내부 오디오 캡처가 붙어도 기본 UX는 유지한다.

---

## Demo mode handoff

현재 `MainViewModel`에는 앱 실행 직후 샘플 방송 자막을 자동 재생하는 데모 모드가 들어가 있습니다.

목적:

- 사용자가 APK를 설치하자마자 화면 흐름을 볼 수 있게 하기 위함
- 마이크 권한, 주변 소음, Android STT 성공 여부와 UI 피드백을 분리하기 위함

현재 동작:

- `demoUseCase`가 `TextInputSource`를 사용합니다.
- `init { startDemoInput() }`로 앱 실행 시 데모가 자동 시작됩니다.
- 데모 자막은 최근 자막, 현재 주제, 최근 1분 요약, 캐치업 알림을 갱신합니다.
- STT를 시작하면 데모 재생은 자동 중지됩니다.
- 데모 속도는 빠른 확인을 위해 `DEMO_MILLISECONDS_PER_SCRIPT_SECOND = 250L`입니다.

다음 Codex 작업 추천:

```text
Run the latest Android debug build.
Fix build errors only.
Then add visible Live screen controls:
- 데모 다시 보기
- 데모 중지
Keep STT controls separate from demo controls.
Do not add backend, external STT SDK, internal audio capture, or new market APIs yet.
```

---

## Codex 작업 지시 예시

```text
Use docs/ui_reference.md as the UI design reference.
Improve the existing Phase 0 Compose UI to better match the reference.
Do not add real audio, STT, backend, API calls, or live market API yet.
Keep current indices optional and hidden from the main screen.
Focus only on layout, text hierarchy, spacing, and parent-friendly UX.
```

---
name: commit
description: mody-server에서 변경사항을 확인하고 Conventional Commits 규칙으로 커밋한다. 사용자가 "커밋", "커밋해줘", "stage and commit" 등을 요청할 때 사용.
---

# commit

mody-server의 변경사항을 **Conventional Commits** 규칙에 따라 커밋한다.
**타입(앞)만 영문**으로 쓰고, **제목과 본문은 한글**로 작성한다.

## 핵심 원칙

- 사용자가 커밋을 요청했을 때만 커밋한다. 진행 중인 기능 구현 도중에는 사용자 확인 없이 커밋하지 않는다.
- 어떤 변경을 어떤 이슈/단위로 묶을지 모호하면 추측하지 말고 사용자에게 물어본다.
- 관련 없는 변경을 한 커밋에 섞지 않는다. 논리적 단위로 나눈다.
- `.agents/`와 `docs/policy/`는 로컬 작업물이다. 사용자가 명시하지 않으면 스테이징하지 않는다.
- 커밋 메시지에는 `spot`이라는 단어를 넣지 않는다.

## 절차

1. `git status --short --branch`로 현재 브랜치와 변경 파일을 확인한다.
2. `git diff`와 필요 시 `git diff --staged`로 변경 내용을 읽는다.
3. 변경을 논리적 단위로 나눈다. unrelated 변경은 제외한다.
4. 아직 검증하지 않았다면 관련 테스트/빌드 실행 여부를 사용자 요청과 작업 위험도에 맞춰 판단한다.
5. 스테이징은 의도한 파일만 한다. 로컬 정책 파일과 스킬 파일은 별도 요청이 있을 때만 포함한다.
6. 관련 GitHub Issue가 있으면 본문 trailer에 `Refs #N` 또는 PR에서 닫을 경우 `Closes #N`를 쓴다.
7. 아래 형식으로 커밋한다.

## 커밋 메시지 형식

```
<type>(<scope>): <한글 제목, 마침표 없음>

<한글 본문 — "왜" 변경했는지. 무엇을 했는지보다 이유 중심>

Refs #<issue-number>
Co-Authored-By: Codex <codex@openai.com>
```

타입(`feat`, `fix` 등)과 scope만 영문이고, **제목·본문은 한글**이다.

### type (영문 고정)

- `feat` 새 기능
- `fix` 버그 수정
- `docs` 문서 (LLD/ADR/README 등)
- `refactor` 동작 변경 없는 구조 개선
- `perf` 성능 개선
- `test` 테스트 추가/수정
- `build` 빌드/의존성 (gradle 등)
- `ci` CI 설정
- `chore` 그 외 잡일

### scope (선택)

도메인/모듈 이름 (예: `auth`, `user`, `member`). 애매하면 생략한다.

## 예시

```
feat(auth): 카카오 OAuth 로그인 엔드포인트 추가

카카오 소셜 로그인 1차 플로우 구현. LLD-0003 기준으로 토큰 발급까지 처리한다.

Refs #12
Co-Authored-By: Codex <codex@openai.com>
```

## 하지 말 것

- 어떤 이슈에 연결할지 임의로 정하기 → 모르면 묻는다.
- 미완성/실패하는 테스트를 "통과한다"고 본문에 쓰기.
- 관련 없는 변경 묶기.
- 사용자 변경이나 생성물을 임의로 되돌리기.

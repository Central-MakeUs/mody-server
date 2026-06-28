---
name: pr
description: mody-server에서 GitHub PR을 생성하거나 갱신한다. 사용자가 "PR 만들어줘", "풀리퀘 올려줘", "PR 올려줘" 등을 요청할 때 사용.
---

# pr

mody-server의 Pull Request를 생성하거나 갱신한다. PR 본문은 실제 diff, 관련 이슈, LLD/ADR가 있으면 해당 문서를 기준으로 작성한다.

## 핵심 원칙

- 추측하지 않는다. 실제 diff, 이슈, 문서에 없는 내용은 쓰지 않는다.
- PR 제목은 한글로 작성한다. Conventional Commit 형식의 type/scope는 영문으로 유지한다.
- PR 생성 전 `git status`, 현재 브랜치, 원격 push 상태를 확인한다.
- 이슈를 완전히 해결하는 PR이면 `Closes #N`, 일부만 다루면 `Refs #N`를 사용한다.
- base 브랜치는 기본적으로 `main`이다. PR 체이닝이 필요하면 현재 작업의 기반 PR 브랜치를 base로 사용하고 본문에 명시한다.
- `.agents/`와 `docs/policy/`는 사용자가 명시하지 않으면 PR에 포함하지 않는다.

## 사전 확인

- 작업 브랜치는 `feature/{issue-number}` 형식을 우선한다.
- 현재 브랜치가 `main`이면 PR을 만들지 말고 새 브랜치 필요 여부를 사용자에게 확인한다.
- 아직 커밋되지 않은 변경이 있으면 PR 생성 전에 커밋 필요 여부를 확인한다.
- 원격에 브랜치가 없으면 `git push -u origin <branch>` 후 PR을 만든다.

## 절차

1. `git status --short --branch`와 `git log --oneline --decorate -5`로 상태를 확인한다.
2. `gh issue view <N>`로 관련 이슈를 읽는다. 브랜치명에서 이슈 번호를 추론할 수 있으면 사용한다.
3. base를 결정한다. `main`과 최신화되지 않았거나 체이닝 PR이면 사용자 맥락을 우선한다.
4. `git diff <base>...HEAD --stat`와 필요한 파일 diff를 읽는다.
5. 관련 LLD/ADR가 있으면 읽고, 없으면 diff와 이슈 기준으로만 쓴다.
6. 아래 템플릿으로 본문을 작성한다.
7. `gh pr create --base <base> --head <branch> --title "<title>" --body-file <tmpfile>`로 생성한다. 이미 PR이 있으면 `gh pr edit`로 갱신한다.

## PR 본문 템플릿

```markdown
## 개요
<이번 PR이 해결하는 문제와 범위>

## 관련 이슈
Closes #<N>

## 변경 사항
<diff 기반 사실만 bullet로 작성>

## 문서
- LLD: <있으면 경로>
- ADR: <있으면 경로>

## 테스트
- `<실행한 명령>`: 통과/실패

## 비고
<미결정 사항, 후속 작업, 배포 영향이 있으면 작성>
```

## 제목 예시

```
feat(member): 회원 가입 플로우 구현
docs(api): Swagger 명세 정리
fix(auth): OAuth 토큰 요청 수정
```

## 하지 말 것

- 문서나 diff에 없는 설계/결정을 본문에 서술하기.
- 테스트를 안 했는데 통과했다고 쓰기.
- 사용자가 원하지 않은 변경 파일을 커밋/PR에 포함하기.
- 영어 제목으로 PR 만들기.

# LLD-0035 관리자 로그인 및 전체 인증 피드

- 관련 이슈: #223
- 상태: 디자인 승인 및 구현 완료. 실제 Google 로그인 활성화는 서버 설정 후 별도 검증 필요.
- 브랜치: `feature/223-admin-console`
- 최초 허용 계정: `kminseok5167@gmail.com`

## 화면 및 기술 범위

기존 Spring Boot 서버가 `/admin`의 정적 HTML/CSS/JavaScript를 제공한다.
React/Next.js, 별도 프론트 서버, 별도 빌드 파이프라인은 추가하지 않는다.
기존 `/admin/challenges` 그룹·챌린지 관리 기능을 유지하고 인증 피드를 첫 화면으로 추가한다.
2026-10-11 사용자 승인 레이아웃을 유지하고 브랜드 색상을 적용했다.
메인 색상은 `#FBD406`, `#FFE24A`, `#FFEE92`, 세컨더리는 `#6E7CFF`, `#A8B0FF`다.

1. 관리자 Google 로그인
2. 식사·운동·챌린지 사진 인증을 모아 보는 카드형 피드
3. 기간, 인증 유형, 닉네임/인증 내용 및 회원/그룹/그룹 챌린지 ID 필터
4. 원본 이미지, 작성자, 업로드 시각, 연결 그룹, 챌린지 및 크롭 좌표 상세
5. 환경(로컬/개발/운영) 구분 및 모바일 대응

삭제된 인증은 노출하지 않는다. 걸음수 등 사진 없는 인증은 이번 범위에 포함하지 않는다.
탈퇴 회원의 남아 있는 인증은 `탈퇴한 회원`으로 표시한다.
카드의 이미지 썸네일은 CSS 중앙 크롭, 상세 이미지는 원본이다. 사용자 저장 크롭 좌표는 상세에서 확인한다.
목록의 개수는 **불러온 개수**이며 전체 건수로 표현하지 않는다.

## 관리자 인증

- 앱 회원 계정 생성 흐름을 사용하지 않는다. 관리자 계정은 `admin_account`로 분리한다.
- Google 공식 ID token verifier로 서명, issuer, audience, 만료를 확인하고 세션 nonce를 확인한다.
- 최초 관리자 생성은 검증된 Gmail 또는 Google Workspace 이메일의 명시적 허용 목록만 사용한다.
- 로그인 이후 식별자는 이메일이 아닌 Google `sub`다. 비활성화된 계정을 자동으로 복구하지 않는다.
- 역할은 `VIEWER`, `OPERATOR`, `ADMIN`. 세션 기반 변경 요청은 CSRF 검증과 쓰기 권한 확인이 필요하다.
- 세션은 HttpOnly/Secure/SameSite=Lax. 로그인 시 ID 및 CSRF token을 회전한다.
- 최대 로그인 유효기간 4시간, 유휴 세션 기본 30분. 각 요청에서 DB의 계정 활성 여부와 역할을 다시 확인한다.
- 자동화 클라이언트의 `X-Admin-Api-Key` 계약은 유지한다. 브라우저에서는 API 키를 입력·저장하지 않는다.
- 관리자 응답은 no-store. 변경 작업 로그에 관리자 ID, 메서드, 경로 및 결과 상태만 남긴다.
- 최신 main의 단일 인스턴스 수동 배포 구성을 따른다. 배포 스크립트 및 워크플로는 변경하지 않는다.
- 메모리 세션이라 서버 재시작·배포 시 재로그인해야 한다. 다중 인스턴스 세션 공유는 후속 범위다.

## 조회 API 및 페이지네이션

- `GET /api/v1/admin/auth/config`: 웹 client ID, 환경, CSRF token 및 일회용 로그인 nonce
- `POST /api/v1/admin/auth/google`: Google credential로 관리자 세션 생성
- `GET /api/v1/admin/auth/me`: 현재 관리자 및 CSRF token
- `POST /api/v1/admin/auth/logout`: 세션 무효화
- `GET /api/v1/admin/feed`: 통합 사진 인증 조회
- `GET /api/v1/admin/feed/{type}/{id}`: 인증 상세

피드 필터: `from`, `to`, `type`, `memberId`, `groupId`, `groupChallengeId`, `keyword`, `cursor`, `size`.
기본 날짜는 한국시간 최근 7일, 범위는 최대 366일, 페이지 크기는 1~100(기본 24).
ID는 새 응답에서 문자열로 직렬화한다.

`activity_record`와 `challenge_proof`를 UNION ALL로 합친다. 그룹 공유 매핑은 EXISTS로 확인해
같은 활동 기록이 여러 카드로 늘어나지 않게 한다. 매핑이 없는 이전 기록에만 기존 group_id를 적용한다.
각 소스에 limit을 적용한 뒤 합쳐 최종 limit을 적용한다.
정렬은 `(uploaded_at DESC, type DESC, id DESC)`, 커서는 같은 세 항목으로 구성한다.
시간이 같은 인증과 두 소스 간 ID 중복에도 누락되지 않는다. 필터를 바꾸면 커서를 초기화한다.
수정/삭제와 동시 조회에 대한 스냅샷 일관성은 제공하지 않는다.

V18은 관리자 계정 및 각 인증 소스의 `(uploaded_at, id)` 인덱스를 추가한다.
다른 브랜치가 V18을 추가하는 경우 merge 전 충돌 확인 및 번호 조정이 필요하다.
main의 #226 단일 인스턴스 구성에 관리자 세션 설정만 추가했다.
graceful shutdown 설정은 유지하며 제거된 스케줄 색상 게이트는 다시 추가하지 않는다.

## 검증 및 운영 적용

디자인 미리보기는 별도의 루프백 샘플 서버로만 실행했다.
실제 DB, 실제 관리자 인증 또는 운영 데이터를 사용하지 않았다. 인증 우회 코드는 애플리케이션에 추가하지 않는다.
스크린샷은 샘플 사진·계정으로 디자인만 확인하는 자료다.

- [x] 사용자 디자인 승인 및 브랜드 색상 반영
- [x] 기존 그룹·챌린지 관리 화면의 API 키 입력을 관리자 세션으로 전환
- [x] 관리자 인증/CSRF/권한/차단 테스트 및 통합 피드 쿼리 테스트
- [x] MySQL 8 임시 로컬 컨테이너에서 인증/피드 통합 테스트 및 V18 DDL 적용·인덱스 확인
- [ ] 실제 Google 웹 client 및 서버 환경 설정 (PR 코드와 별도)
- [ ] 배포 및 실제 허용 계정 로그인 확인

이미지 URL은 기존 저장소 정책을 유지한다. 관리자 화면 보호가 기존 공개 이미지 URL의 접근 제어를 변경하지는 않는다.

## 미결정 사항 (Open Questions)

코드 구현에 필요한 미결정 정책은 없다. 실제 Google Web Client 생성 및 환경 주입은 배포 전 설정 작업이다.
신규 관리자 초대/역할 편집 UI, 걸음수 피드, 신고 조치 및 회원 제재는 이번 PR 범위가 아니다.

## 참고

- [Google ID token 검증 가이드](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token)
- [Google 로그인 버튼 가이드](https://developers.google.com/identity/gsi/web/guides/display-button)

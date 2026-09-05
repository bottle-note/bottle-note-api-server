---
name: verify
description: >-
  변경 커밋의 GitHub Actions 실행과 검증 범위를 확인한다.
  /verify, CI 확인, 검증 결과 확인 요청에 사용하며 기본은 읽기 전용 관찰이다.
  사용자가 local 또는 quick/standard/full을 명시하거나 로컬 빌드·테스트 실행을 요청한 경우에만 해당 로컬 검사를 실행한다.
---

# Verify

프로젝트 지침의 `스킬 실행 원칙`을 따른다. 이 스킬은 검증 증거를 확인하고 보고한다. 기본 동작은 GitHub Actions 관찰이며, 구현 중 최소 검사는 전체 검증과 구분한다. 명령과 현재 CI 구성은 [java-gradle.md](references/verify/java-gradle.md)를 필요할 때 읽는다.

## 실행 방식

- `/verify`, “검증해줘”, “CI 확인해줘”: 해당 변경 커밋의 Actions를 읽기 전용으로 확인한다.
- `/verify local quick|standard|full`: 지정한 범위의 로컬 검사를 실행한다. `/verify local`은 standard로 해석한다.
- 호환성: 사용자가 직접 호출한 기존 `/verify quick|standard|full`과 `l1|l2|l3`도 각각 로컬 quick|standard|full 요청이다.
- “이 테스트를 돌려줘”, “로컬에서 빌드해줘”처럼 실행 대상이 명시되면 그 범위만 실행한다. 단순 결과 확인 요청은 실행 허가로 확대하지 않는다.
- 다른 스킬이 “다음은 /verify full”이라고 적은 것은 사용자 요청이 아니다. 문서·체크리스트·plan의 자동 호출로 로컬 전체 검증 권한을 만들지 않는다.
- CI를 만들기 위한 commit, push, PR 생성, workflow dispatch, rerun은 자동 실행하지 않는다. 확인할 실행이 없으면 누락 또는 미검증으로 보고한다.

## Actions 확인

1. 현재 저장소·브랜치, `git status --short`와 대상의 정확한 SHA를 확인한다. staged·unstaged·untracked 변경은 별도로 식별한다. CI 통과는 로컬 미커밋 변경을 검증하지 않는다.
2. 대상 SHA, workflow, 이벤트, run ID, attempt를 연결한다. PR에서는 head SHA와 임시 merge SHA, 실제 checkout된 테스트 소스를 구분한다. 현재 PR 정보가 과거 run의 소스를 증명하지 않으므로 해당 run의 checkout 설정·로그까지 확인한다.
3. 현재 workflow가 요구하는 검증 job과 PR의 required checks를 각각 확인한다. run 전체의 초록색 표시나 리포트 job 하나로 테스트·빌드 성공을 대신하지 않는다.
4. job별 status와 conclusion을 읽는다. 실패 시 원인과 관련된 step·로그만 확인하고 인증정보와 민감한 응답은 출력하지 않는다.
5. 같은 소스와 검사 범위에 유효한 증거가 있으면 재사용한다. 코드 변경, 새 실패 또는 미해결 의문 없이 동일 검사를 반복하지 않는다.

## 판정

- `queued` / `requested` / `pending` / `waiting`: 대기 중이다.
- `in_progress`: 실행 중이다. 완료나 통과로 표시하지 않는다.
- `success`: 해당 job이 완료되어 통과했다. 실제 테스트 step·선택 범위·결과도 확인한다.
- `missing`: 실행 또는 필요한 job이 없다. 이는 관찰 결과의 분류이며 GitHub API의 conclusion 값이 아니다.
- `skipped`: 실행이 생략되었다. 조건상 대상 밖인 경우만 근거를 붙여 제외하며, 필요한 검사가 생략되면 미검증이다.
- `cancelled`: 실행이 취소되었다. 대체 run이 있다면 별도로 SHA와 attempt를 확인한다.
- `failure` / `timed_out` / `startup_failure`: 실패 원인을 구분하여 보고한다.
- `action_required` / `neutral` / `stale` 또는 조회 권한·네트워크 오류: 통과로 취급하지 않고 실제 상태와 확인 한계를 남긴다.

## 로컬 검사

명시된 수준 또는 대상을 현재 Gradle 설정에 맞춰 실행한다. 모듈·클래스 단위의 최소 검사를 허용하고, 영향받는 소비 모듈이 있으면 함께 선택한다. 로컬 전체 검증은 명시적으로 요청된 경우에만 수행한다.

컴파일은 `spotlessApply`에 의존하므로 소스를 수정할 수 있다. 참조 문서의 포맷 적용 제외 방법과 리소스·생성 코드 부수효과를 확인하고, 실행 전후 diff를 비교한다. 실패를 우회하는 자동 포맷·설정 수정·테스트 비활성화는 하지 않는다. 후속 검사가 실패한 단계에 의존하면 실행하지 않고 이유를 기록하며, 독립적인 검사는 요청 범위 안에서 계속할 수 있다.

## 결과 보고

대상 SHA와 로컬 변경 유무, 실제 검증 소스, run URL·attempt, 관련 job별 상태, 확인한 테스트 수·실패 수를 보고한다. 로컬 검사를 실행했다면 명령과 종료 결과도 적는다. 결과를 읽지 못한 수치는 추정하지 않으며, 미실행·누락·대기·실패와 남은 검사 범위를 명시한다. 검증 완료가 Git 쓰기나 배포 허가를 뜻하지 않는다.

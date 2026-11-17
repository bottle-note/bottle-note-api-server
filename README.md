
![123](https://github.com/user-attachments/assets/a6256292-33d9-4801-9b9d-78f11d9dea13)

> Bottle note 프로젝트의 API 서버입니다.

<img src="https://github.com/bottle-note/.github/assets/97773895/fa515094-a490-4b7b-9bca-026752fe3de1" alt="Image 1" width="49.5%"/>
<img src="https://github.com/bottle-note/.github/assets/97773895/98a69d6d-af4f-4365-ac6c-b3e7bdd6aed1" alt="Image 4" width="49.5%"/>
<img src="https://github.com/bottle-note/.github/assets/97773895/5877ab4b-af23-4767-aebc-5e97a35a89ba" alt="Image 3" width="49.5%"/>
<img src="https://github.com/bottle-note/.github/assets/97773895/039cf3df-7169-46ed-a76b-72de6f1a1217" alt="Image 2" width="49.5%"/>
<br/>

## *bottle note*

---

**📄*내 입맛에 딱 맞는 한병을 찾아가는 여정노트***

보틀노트는 위스키 애호가들이 모여 서로의 의견을 공유하고, 더욱 즐겁고 유익한 시음 경험을 할 수 있도록 도와줍니다. <br/>
위스키에 대한 열정을 함께 나눠보세요! 쉽고 편리하게 위스키 평가와 리뷰를 공유할 수 있어요!

---

**🥃 위스키 별점 및 리뷰 작성**

위스키를 시음한 후 별점을 부여하고, 상세한 리뷰를 작성해 보세요! <br/>
맛, 향, 느낌 등 다양한 측면을 기록하고 공유할 수 있습니다. <br/>
가격을 입력하고, 시음한 장소를 태그하여 좋은 경험한 장소를 기록해 보는건 어때요?<br/>
<br/>

**🥃 위스키 정보 및 리뷰 보기**

위스키의 상세 정보와 다른 사용자의 리뷰를 쉽게 확인할 수 있습니다. <br/>
다양한 의견을 통해 다른 사람들이 위스키를 어떻게 평가했는지 알 수 있고 <br/>
새로운 위스키를 시도해볼 수 있어요!<br/>
<br/>

**🥃 찜하기 기능으로 위스키 목록 관리**

관심 있는 위스키를 찜하기 기능을 통해 목록에 추가하고 <br/>
찜한 위스키 목록을 통해 나만의 위스키 컬렉션을 만들 수 있어요!<br/>
<br/>
**🥃 다른 유저와 팔로우 및 별점 보기**

다른 사용자를 팔로우하여 그들의 별점과 리뷰를 볼 수 있어요! <br/>
나의 팔로워들은 어떤 평가를 했을까요? <br/>
팔로워가 나와 취향이 비슷하다면, 내 입맛에 맞는 새로운 위스키를 알 수 있을지도?<br/>
<br/>

**🥃 나의 평가 및 별점 기록 관리**

내가 평가한 위스키와 부여한 별점을 한눈에 볼 수 있어요! <br/>
나만의 위스키 평가 기록을 통해 나의 취향을 분석하고 다음 시음을 계획해보세요!<br/>
<br/>

---

## 아키텍처

현재 `settings.gradle`에는 16개 모듈이 있다. `bottlenote-mono`의 소스와 build 설정은 제거했다. Gradle 프로젝트명에는 `bottlenote-` 접두사를 붙인다.

| 모듈 | 소유하는 코드 |
|---|---|
| product-api / admin-api / batch | 기존 3개 실행 앱. Product·Admin의 화면별 복합 조회와 응답 조합도 각 앱이 소유한다 |
| foundation | 업무 타입과 업무 테이블을 모르는 공통 기반, 공통 예외·페이징·JPA 설정·외부 연동 |
| user | 사용자·인증·약관·차단·감사자·마스킹 |
| alcohol | 주류·속성·기존 큐레이션(AdminCurationService)·수입 정보와 주류 병합 계약 |
| storage | 파일·이미지·리소스 기록 |
| community | 리뷰·별점·좋아요·찜과 관련 웹 변환 설정 |
| history | 활동 이력과 Redis 주류 조회 이력 |
| support | 문의와 일일 리포트 |
| notification | 알림과 수신 설정 |
| analytics | 방문·통계 |
| content | 배너·캠페인·스펙 기반 큐레이션(AdminSpecBasedCurationService) |
| access-control | IP 접근 제어 |
| observability | 관측 공통 기능 |
| test-support | 업무를 모르는 컨테이너 설정·데이터 정리·webhook 테스트 대역 |

실행 앱 3개는 루트에 둔다. 업무 모듈 10개는 `module-domains/{이름}`, foundation·observability·test-support는 `module-shared/{이름}`에 둔다. `settings.gradle`에서 각 폴더를 기존 Gradle 프로젝트명에 `projectDir`로 연결한다.

모듈명은 패키지명과 같지 않을 수 있다. 파일 위치와 build 의존을 함께 확인한다. `query`와 `web-support` 모듈은 만들지 않았다. 조회는 사용하는 앱에 두고, 인증과 마스킹은 user에, 별점 규칙과 웹 변환 설정은 community에 두었다.

Product·Admin·Batch 세 앱과 기존 DB를 유지합니다. 여러 업무를 묶는 화면 조회는 사용하는 앱이 소유하며 업무 간 호출은 공개 계약을 따릅니다. community가 활동 이벤트를 발행하고 history와 notification이 받습니다. 모듈 방향은 업무의 중요도를 뜻하지 않습니다.

Gradle 의존 선언은 컴파일 경로를 정합니다. public 내부 클래스 접근과 조회 쓰기 제한은 규칙 검사와 리뷰가 함께 맡습니다. 업무 Factory·Fake는 소유 모듈의 testFixtures에서 선택하고 test-support는 중립 인프라만 제공합니다.

상세 구성과 비용은 [모듈 ADR](<adr/2026.11.17 업무 경계를 Gradle 모듈로 분리하고 기존 배포 단위는 유지한다.md>), [협력 ADR](<adr/2026.11.17 모듈 간 협력은 공개 계약과 단방향 의존으로 구성한다.md>), [fixture ADR](<adr/2026.11.17 업무 Fake와 fixture는 소유 모듈에 두고 테스트 인프라는 공유한다.md>)을 따릅니다.

---

## 이슈 관리

기능 요청, 버그 리포트, 작업 항목 등 모든 이슈는 [bottle-note/workspace](https://github.com/bottle-note/workspace) 레포지토리에서 관리합니다. <br/>
이슈의 SSOT는 workspace이며, PR 작성 시 본문에 `bottle-note/workspace#N` 형식으로 관련 이슈를 링크해 주세요.

---

## Git hook

Java 21 환경에서 [Lefthook](https://lefthook.dev/)을 설치하고 저장소 루트에서 `lefthook install`을 실행합니다.

- `pre-commit`: `./gradlew spotlessCheck`
- `commit-msg`: `type(scope): 제목` 형식과 제목 50자 제한 검사
- 허용 type: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `deps`, `perf`, `build`, `ci`, `revert`

---

보틀노트는 위스키 애호가들이 모여 서로의 의견을 공유하고 <br/>
더욱 즐겁고 유익한 시음 경험을 할 수 있도록 도와줍니다.

![boton](https://github.com/user-attachments/assets/d8750770-1e6a-4133-86a4-298f795420a6)

## RestDocs 관련 가이드입니다.

# RestDocs

> Spring Rest Docs를 사용하여 API 문서를 생성하는 방법을 설명합니다.

## RestDocs란?

> Spring Rest Docs는 Spring MVC 테스트 또는 WebTestClient를 사용하여 API 문서를 생성하는 도구입니다.<br>
> RestDocs는 테스트 코드를 작성하면서 API 문서를 생성할 수 있어서<br>
> API 문서가 항상 최신 상태를 유지할 수 있습니다.<br>




**프로젝트 의존성**

```groovy
asciidoctorExt 'org.springframework.restdocs:spring-restdocs-asciidoctor'
testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
```

- asciidoctorExt: Asciidoctor를 사용하여 문서를 생성하는 의존성입니다.
- testImplementation: Spring Rest Docs MockMvc를 사용하기 위한 의존성입니다.

**문서화 동작 순서**

1. 테스트 코드 작성 ( Spring 의존성은 추가 안된 환경으로 MockMvc를 활용)
2. gradle build
    1. test
    2. snippets 생성 -> 문서들 생성 (build/generated-snippets/...)
    3. asciidoctor 스크립트 실행 ->  문서 생성 (build/docs/asciidoc/index.html)
    4. copyRestDocs 스크립트 실행 -> 문서를 project-root/docs로 복사 (docs/index.html)
    5. build 실행
3. github push
4. github pages 배포 action 실행 (자동)

> 전체적으로 api 최초 구현 후 asciidoc에 대한 조립만 하면 나머지는 자동적으로 진행합니다.

----

## Snippets 생성 방법

### 최초 생성

```src/docs/asciidoc/``` 하위에 적절한 경로에 **adoc** 파일 생성 후 ```build```를 통해 생성된 snippets를 이용하여 문서를 작성합니다.

```asciidoc
== api 이름 (api 영어 이름)

좀더 큰 부분의 영역을 해당 문서에 적습니다

에를 들어 주문과 관련된 모든 API는 모두 이 파일에서 작성합니다.

=== 주문 검색

위 부분이 주문에 관련된 헤더라면 이 영역은 예를 들어 주문의 조회에 관련된 내용을 작성합니다.

// 이 부분은 request에 대한 내용을 작성합니다. 요청 파라미터와 같은 것
==== 요청 값 ( request )  

// 이 부분은 스닙펫을 확인 후 적절한 파일은 include하여 사용합니다.
include::{snippets}/common/rest-docs/http-request.adoc[]
// request-fieldssms의 경우 === 헤더가 있어 이를 목차에서 제외하기 위해 사용 (위에서 이미 ==== 요청 값 ( request )를 적용했기 때문에.)
[discrete]
include::{snippets}/common/rest-docs/request-fields.adoc[]

// 위와 동일한 응답 값
==== 응답 값 ( response )

include::{snippets}/common/rest-docs/http-response.adoc[]
[discrete]
include::{snippets}/common/rest-docs/response-fields.adoc[]

=== 주문 등록

이 부분은 등록에 관련된 내용을 작성합니다.

위와 동일하게 요청값과 응답값을 작성합니다.

```

- 파일을 보면 알 수 있듯이 하나의 파일은 하나의 걔념을 다루는 것이 적절합니다.
- asciidoc의 문법은 조금 독특하기 때문에 별도의 자료를 참조하거나 다른 파일을 참조하여 작성하는 것이 좋습니다. 

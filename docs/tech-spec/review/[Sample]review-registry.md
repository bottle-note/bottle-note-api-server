## 리뷰 등록 기능 테크 스펙

### 기능 설명

- 사용자가 상품이나 서비스에 대한 리뷰를 등록할 수 있는 기능을 구현합니다.
- 리뷰 등록 기능은 별점 평가, 텍스트 기반 리뷰, 사진 업로드 옵션을 포함합니다.
- 사용자 인증 후에만 리뷰를 등록할 수 있으며, 리뷰는 사용자 프로필에 연결됩니다.

----

### 필요성

- 우리 서비스의 가장 핵심 기능 중 하나를 담당합니다.
- 사용자의 피드백을 수집하여 서비스 개선에 필수적인 인사이트를 제공합니다.
- 커뮤니티성의 리뷰 보다는 본인의 히스토리를 관리하는 듯한 느낌의 리뷰를 제공합니다.

----

### 구현 방법

> 이 부분의 작성은 최대한 자유롭게 해보는걸 추천합니다.
> 각기 다른 느낌의 작성방식을 보고, 좀 더 나은 방법을 찾아보는 과정이 필요할듯 합니다.

#### **코드의 구현:** Spring Boot와 JPA를 사용하여 리뷰 데이터를 관리하고 처리합니다.

#### **성능:** 대량의 리뷰 데이터 처리를 위해, 캐싱과 데이터베이스 인덱싱 전략을 적용합니다.

#### **확장성:** 리뷰 기능을 다른 모듈과 독립적으로 유지하여, 향후 다양한 상품이나 서비스에 쉽게 적용할 수 있습니다.

#### **디자인패턴:** Repository 패턴을 사용하여, 데이터 소스와 비즈니스 로직 사이의 결합도를 낮춥니다.

#### **라이브러리 사용:** Amazon S3를 사용하여 리뷰에 첨부된 이미지를 저장하고 관리합니다.

#### **통신 프로토콜:** 사용자의 리뷰 데이터 보호를 위해 HTTPS 프로토콜을 사용합니다.

#### **요청 및 응답 값:** 리뷰 등록 요청에는 사용자 ID, 별점, 텍스트 리뷰, 이미지 파일(선택 사항)이 포함됩니다. 응답 값에는 리뷰 등록 성공 여부와 관련 메시지가 포함됩니다.

----

### 협업 및 피드백

- 리뷰 이미지 업로드 시의 파일 크기 제한과 형식에 대한 가이드라인이 필요합니다. 프론트엔드 개발팀과 협업하여 사용자 인터페이스에서 적절한 안내를 제공해야 합니다.
- 리뷰 데이터의 검증 로직에 대한 피드백이 필요합니다. 보안 팀으로부터 입력 값 검증에 대한 베스트 프랙티스를 공유받을 수 있습니다.
- 사용자 경험 개선을 위한 리뷰 등록 프로세스에 대한 UI/UX 팀의 피드백을 요청합니다. 리뷰 등록 과정의 단계별 사용자 피드백을 통해 개선 사항을 도출할 수 있습니다.
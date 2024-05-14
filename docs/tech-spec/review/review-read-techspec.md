## 요약(Summary)

> 이 문서는 리뷰 조회 기능을 구현하기 위해 작성된 테크스펙입니다.<br>
> 유저는 alcohol에 리뷰를 달 수 있습니다. 리뷰는 기본적으로 등록, 수정, 조회, 삭제, 댓글 기능이 제공됩니다. <br>
---------

## 목표(Goals)

- 리뷰 조회 API를 구현한다.
- 리뷰 조회 테스트코드를 구현한다.

---------

## 계획(Plan)

### 리뷰조회 API 설계

> 해당 API는 alcohol에 대한 리뷰를 조회하는 기능을 제공합니다.

해당 이슈에서는 리뷰 조회에 대한 API를 설계하고, 이를 테스트하는 코드를 작성합니다.

- API  :  `GET /api/v1/review/{alcoholsId}/pageble`

### **Request**

- PathVariable로 alcohols의 id를 전달합니다.<br>
- QueryString으로 Pagination 관련 파라미터와 Sort 파라미터를 전달합니다.

### **Response**

- 리뷰 목록
  - 유저 아이디
  - 유저 프로필(썸네일)
  - 유저 닉네임

  - 리뷰 아이디
  - 리뷰가 준 별점
  - 리뷰의 가격 정보
  - 보틀 / 잔 여부
  - 가격 (정수로만 , 기호 없이)
  - 리뷰 좋아요 수
  - 리뷰 댓글 수

  - 나의 코멘트 여부
  - 공개 여부
  - 좋아요 여부
  - 댓글 여부

  - 썸네일
  - 작성일

### **Error Code**

- 400 : Bad Request
  - 잘못된 요청 , 필수 파라미터 누락
- 401 : Unauthorized
  - 인증되지 않은 사용자
- 403 : Forbidden

- 404 : Not Found
  - 해당 id의 위스키가 존재하지 않음
- 500 : Internal Server Error
  - 서버 에러 ( 서버 내부 로직 에러 )

------

### 체크 해야하는 이슈 사항

- validate
  - [ ] alcoholsId 는 Long 타입이어야 한다.

- 비지니스 로직
  - [ ] QueryDSl의 DTO Injection 기능을 사용
  - [ ] Review 엔티티(Driving) Alcohol 엔티티(Driven) Left Inner Join 하여 알코올 별 리뷰 조회
  - [ ] Select 절 서브쿼리로 좋아요 수 Count 집계
  - [ ] 서브 쿼리 불가한 것들은 서비스 비즈니스 로직으로 처리
  - [ ] 커서 방식 Pagination 구현

---------

## 이외 고려 사항들(Other Considerations)

``

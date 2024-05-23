## 요약(Summary)

> 이 문서는 리뷰 등록 기능을 구현하기 위해 작성된 테크스펙입니다.<br>
> 유저는 alcohol에 리뷰를 달 수 있습니다. 리뷰는 기본적으로 등록, 수정, 조회, 삭제, 댓글 기능이 제공됩니다. <br>
---------

## 목표(Goals)

- 리뷰 등록 API를 구현한다.
- 리뷰 등록 테스트코드를 구현한다.

---------

## 계획(Plan)

### 리뷰등록 API 설계

> 해당 API는 alcohol에 대한 리뷰를 등록하는 기능을 제공합니다.

해당 이슈에서는 리뷰 등록에 대한 API를 설계하고, 이를 테스트하는 코드를 작성합니다.

- API  :  `POST /api/v1/reviews`

### **Request**

    - RequestBody로 전달되는 매개변수

- 위스키 아이디
- 별점
- 공개여부
- 내용
- 잔/보틀 여부
- 가격
- 장소
  - 우편번호
  - 주소
  - 상세주소
- 이미지 정보
  - 업로드된 이미지 경로들
- 테이스팅 태그
  - 작성된 태그 목록

### **Response**

- 작성된 리뷰 ID

### **Error Code**

- 400 : Bad Request
  - 잘못된 요청 , 필수 파라미터 누락
- 401 : Unauthorized
  - 인증되지 않은 사용자
- 404 : Not Found
  - 해당 id의 위스키가 존재하지 않음
  - 해당 id의 유저가 존재하지 않음
- 500 : Internal Server Error
  - 서버 에러 ( 서버 내부 로직 에러 )

------

### 체크 해야하는 이슈 사항

- validate
  - [ ] alcoholsId 는 Null일 수 없다
  - [ ] userId는 Null일 수 없다
  - [ ] content는 Null일 수 없다
  - [ ] Enum @JsonCreator 유효성 검사
  - [ ] RatingPoint 0.0 ~ 5.0인지
  - [ ] 테이스팅 태그 최대 12자, 총 개수 10개 이하인지

- 비지니스 로직
  - [ ] 파라미터로 전달받은 alcoholId로 위스키 조회(존재하지 않는다면 Exception)
  - [ ] currentUserId로 유저 조회(존재하지 않는다면 Exception)
  - [ ] ReqestDto로 Review 엔티티 객체 생성
  - [ ] RequestDto에 ratingPoint가 존재한다면 Rating 엔티티 save
  - [ ] Review 엔티티 save
  - [ ] ReviewCreateResponse DTO 반환

---------

## 이외 고려 사항들(Other Considerations)

``

## 요약(Summary)

> 이 문서는 내가 작성한 리뷰 조회 기능을 구현하기 위해 작성된 테크스펙입니다.<br>
> 로그인 한 유저는 자신이 작성한 리뷰 목록을 조회할 수 있습니다. <br>
---------

## 목표(Goals)

- 내가 작성한 리뷰 목록을 조회할 수 있다.
- 테스트코드를 구현한다.

---------

## 요구사항 정의

- 사용자는 반드시 로그인 한 상태여야 한다.
  - 로그인 하지 않으면 조회하고자 하는 사용자의 ID를 알 수 없기 때문에 조회가 불가능하다.
  - SecurityContextHolder에서 로그인 한 유저의 ID를 추출한다.
  - 유저의 ID가 존재하지 않으면 `사용자 찾을 수 없음` 예외가 발생한다.
  - Authorization 헤더에 액세스 토큰을 첨부하지 않으면`토큰이 존재하지 않습니다` 에러 메시지 출력 (400)
  - Authorization 헤더에 첨부한 토큰이 잘못 된 토큰이면 `잘못된 토큰입니다` 라는 에러 메시지 출력 (401)
  - Authorizatioin 헤더에 첨부한 토큰이 만료 된 토큰이면 `만료된 토큰입니다` 라는 에러 메시지 출력 (403)

- 작성한 리뷰를 조회할 위스키가 실제 DB에 존재하는 위스키여야 한다.
  - 위스키에 등록된 리뷰 중 내가 작성한 리뷰를 필터링해서 조회한다.
  - 즉, 존재하지 않는 위스키에 대해서는 `내가 작성한 리뷰` 조회가 불가능하다.

### API EndPoint

- `GET /api/v1/reviews/me/{alcoholId}`

## 어떤 것을 요청해야 할까

- 내가 작성한 리뷰를 조회할 위스키 ID

## 어떤 것을 응답해야 할까

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

## 어떤 것을 검증해야 할까

- 내가 작성한 리뷰를 조회하려고 하는 유저가 로그인 한 유저인지?
- user id가 Long 타입인지

### **Error Code**

- 400 : Bad Request
  - 잘못된 요청 , 필수 파라미터 누락
- 401 : Unauthorized
  - 인증되지 않은 사용자
- 404 : Not Found
  - 해당 id의 유저가 존재하지 않음
- 500 : Internal Server Error
  - 서버 에러 ( 서버 내부 로직 에러 )

------

---------

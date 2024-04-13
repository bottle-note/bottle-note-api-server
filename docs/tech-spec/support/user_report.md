## 요약(Summary)

> 이 문서는 유저 신고 기능을 구현하기 위한 테크 스펙을 작성한 문서입니다.<br>
> 유저 신고 기능은 사용자가 다른 사용자를 신고할 수 있는 기능을 의미합니다.<br>
> 이를 통해 사용자는 다른 사용자의 부적절한 행동을 신고할 수 있으며, 관리자는 신고된 사용자에 대한 조치를 취할 수 있습니다.<br>

---------

## 목표(Goals)

- ser report api 를 구현한다.
- nit test code를 작성한다.
- ser report api 를 사용하여 신고된 사용자에 대한 조치를 취할 수 있도록 한다.

---------

## 계획(Plan)

### 신고 API 설계

> 해당 API는 단순하게 특정한 사용자를 신고하는 기능을 제공합니다.
> 신고된 사용자는 관리자에 의해 조치를 취할 수 있습니다.
> 신고된 사용자는 신고된 횟수에 따라 제재를 받을 수 있습니다.

해당 이슈에서는 신고에 대한 API를 설계하고, 이를 테스트하는 코드를 작성합니다.

- API  :  `post /api/v1/report/user`

### **Request**

```json
{
  "user_id": 1,
  "report_user_id": 3,
  "type": "SPAM",  //( SPAM / INAPPROPRIATE_CONTENT / FRAUD / COPYRIGHT_INFRINGEMENT / OTHER )
  "content": "광고입니다."
}
```

### **Response**

```json
{
  "success": true,
  "code": 200,
  "data": {
    message: "신고가 접수되었습니다.",
    report_user_id: 3,
    report_user_name: "신고 대상 이름"
  }
}
```

### **Error Code**

- 400 : Bad Request
    - 잘못된 요청 , 필수 파라미터 누락
- 401 : Unauthorized
    - 인증되지 않은 사용자
- 403 : Forbidden
    - 권한이 없는 사용자
    - 이미 신고한 사용자 ( 동일 유저에 대한 중복 신고)
        - 동일 유저에 대해 일단 최대 한건의 신고만 가능하도록 제한
    - 일일 신고 횟수 제한
        - 일일 신고 횟수 제한은 5회로 제한 (user 와 review 중복 카운트)
- 404 : Not Found
    - 신고 대상 사용자를 찾을 수 없음
- 500 : Internal Server Error
    - 서버 에러 ( 서버 내부 로직 에러 )

------

### 체크 해야하는 이슈 사항

- validate
    - [ ] user_id 는 Long 타입이어야 한다.
    - [ ] report_user_id 는 Long 타입이어야 한다.
    - [ ] type 은 spam, inappropriate, harassment 중 하나여야 한다.
    - [ ] content 는 300자 이내여야 한다.
    - [ ] report_user_id 와 user_id 가 동일하지 않아야 한다.
- 비지니스 로직
    - [ ] 신고 대상 사용자가 존재하는지 확인
    - [ ] 신고 대상 사용자가 신고자와 동일한 사용자인지 확인
    - [ ] 신고 대상 사용자가 이미 신고된 사용자인지 확인
    - [ ] 신고 대상 사용자가 일일 신고 횟수 제한을 초과하는지 확인
    - [ ] 신고 대상 사용자에 대한 신고 내용이 적절한지 확인 ( ex. 100자 이내 )
    - [ ] 신고 대상 사용자에 대한 신고 타입이 적절한지 확인
    - [ ] 신고 대상 사용자에 대한 신고 내용이 중복되는지 확인

---------

## 이외 고려 사항들(Other Considerations)

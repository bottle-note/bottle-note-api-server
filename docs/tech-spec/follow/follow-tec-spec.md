## 요약(Summary)
사용자 간의 팔로우/팔로잉 관계를 구현하는 방법에 대해 설명합니다.
이 기능은 사용자가 다른 사용자들을 팔로우할 수 있습니다.


---

## 목표(Goals)

- 유저는 다른 사용자를 팔로우할 수 있습니다.
- 유저는 다른 사용자를 언팔로우할 수 있습니다.


---

## 계획(Plan)

- 요구사항 정의
- 사용자
  - 사용자는 한명의 유저 대상으로 하나의 팔로잉만 할 수 있다.
  - 사용자는 여러명의 유저를 대상으로 팔로잉을 할 수 있다.
  - 사용자는 여러명의 팔로워들을 가질 수 있다.
- 팔로우
  - 유저와 유저간 1:1로 성립된다.
  - 동일한 유저간 팔로우 관계는 유일해야한다.(중복불가)
    - A -> B / B -> A 유저 상호간 관계는 가능
  - 상태값은 팔로우중 / 언팔로우 / 차단됨 / 숨김
- 팔로워
  - 차단한 대상이 팔로우 할 경우 팔로우가 “숨김처리"됩니다.
  - 차단/숨김의 상태 값은 별도의 API를 통해 처리 합니다.


---

## 모델링

### User Entity
  - 팔로우한 사용자 목록을 조회할 수 있는 메소드를 추가합니다.
      - OneToMany 관계를 통해 팔로우한 사용자 목록을 조회합니다.
      - 중복방지를 위해 팔로우 한 사용자 목록을 HashSet을 사용합니다.
  - 팔로워 목록을 조회할 수 있는 메소드를 추가합니다.
      - OneToMany 관계를 통해 팔로워 목록을 조회합니다.
      - 중복방지를 위해 팔로워 목록을 HashSet을 사용합니다.
###  Follow Entity
  - 팔로잉 하는 유저와 팔로잉 대상 유저의 아이디를 가집니다.
  - 팔로우 UserFollow 도메인 이벤트가 추가합니다.
    - 다른 유저를 팔로우를 추가할 때 사용합니다.
    - 유저관련 valid check가 필요합니다.
      - nonNull을 사용하여 UserId, FollowUserId는 필수입니다.
- 팔로우 UserUnFollow 도메인 이벤트가 추가합니다.
  - 다른 유저를 팔로우를 삭제할 때 사용합니다.
  - nonNull을 사용하여 UserId, FollowUserId는 필수입니다.
- Builder 패턴을 사용하여 객체를 생성합니다.
    - FollowId, UserId(로그인유저), FollowUserId
### Request 
  - Dto는 Record를 사용하여 생성합니다.
### Rsponese
  - 성공/실패 매세지, 팔로우/언팔로우 UserId 를 반환합니다.
  - Message는 Enum을 사용하여 생성합니다.
    - FOLLOW_SUCCESS("성공적으로 팔로우 처리했습니다.")
    - UNFLLOW_SUCCESS("성공적으로 언팔로우 처리했습니다.")
- follow 관련 Custom exception을 생성합니다.

---

## 비지니스 로직

### 팔로우
  - 팔로우하려는 사용자가 존재하는지 확인합니다.
  - 사용자가 자기 자신을 팔로우하려는지 확인합니다. (자기 자신을 팔로우할 수 없습니다)
  - 이미 팔로우하고 있는 사용자인지 확인합니다. (중복 팔로우 방지)
  - 사용자가 탈퇴한 회원인지 확인합니다.
  - 사용자가 차단한 회원인지 확인합니다.
  - 사용자가 차단당한 회원인지 확인합니다.
  - 팔로우 성공 메시지와 팔로우한 팔로우 대상 사용자의 ID를 반환합니다.
### 언팔로우
  - 언팔로우하려는 사용자가 존재하는지 확인합니다.
  - 실제로 팔로우하고 있는 사용자인지 확인합니다.
  - 언팔로우 성공 메시지와 언팔로우한 언팔로우 대상 사용자의 ID를 반환합니다.

---

## 예외

- FollowExceptionCode :
  - 팔로우할 사용자를 찾을 수 없습니다.
  - 언팔로우할 사용자를 찾을 수 없습니다.
  - 이미 팔로우한 사용자입니다.
  - 자신을 팔로우할 수 없습니다.
  - 탈퇴한 회원은 팔로우 할 수 없습니다.
  - 내가 차단한 회원은 팔로우 할 수 없습니다.
  - 해당 회원에게 차단되어 팔로우 할 수 없습니다.

---

## API 명세 
- 팔로우 / 언팔로우
  - Endpoint: POST /follow
  - Request:
    ~~~
    {
        "userId": "로그인 유저 ID" (backend token util 에서 사용)
        "followUserId": "팔로우 대상 유저 ID"
        "action": "FOLLOW" or "UNFOLLOW"
    }
    ~~~

  - resonse 
      ~~~
      {
        "message": "FOLLOW_SUCCESS", or "UNFLLOW_SUCCESS"
        "followUserId": "팔로우 대상 유저 ID"
      }
      ~~~











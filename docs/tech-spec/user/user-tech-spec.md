## 요약(Summary)

> 이 문서는 OAuth2 소셜 로그인 기능을 위해 작성 된 문서입니다.<br>
> 카카오, 구글, 네이버 로그인 기능을 구현합니다.<br>

----

## 목표(Goals)

- 다양한 소셜 로그인을 구현한다.
- 간편한 로그인으로 인해 사용자의 초기 진입 장벽을 낮춘다.

---------

## 계획(Plan)

### API 설계

- Request :
    1. email(Not blank)
    2. socialType(Not null)(KAKAO, NAVER, GOOGLE, APPLE)
    3. gender(Nullable)
    4. age(Nullable)

```json
{
  "email": "1123@naver.com",
  "gender": "남",
  "age": 48,
  "socialType": "KAKAO"
}
```

- Response :
  accessToken, RefreshToken

```json
{
  "success": true,
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMTIzQG5hdmVyLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIiwidXNlcklkIjoyLCJpYXQiOjE3MTQzODgyNDcsImV4cCI6MTcxNDM4OTE0N30.ZZsiOFgHtTVAMdD-lWXcjUG-H1LglNdmUBng3Dm664mLLYgpY-vK3_ZiBVlmv_lWEh9Th3WlQ5Pixw4WHKTVUg",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMTIzQG5hdmVyLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIiwidXNlcklkIjoyLCJpYXQiOjE3MTQzODgyNDcsImV4cCI6MTcxNTU5Nzg0N30.9eyrz4INSMol47AttmQGFUCTRDI6OrU6WcL59rZSUXtOUeXS4hJCmA3AcCe_s34KoL0G_f-jSvAOfhIfjDmtew"
  },
  "errors": [],
  "meta": {
    "serverVersion": "1.0.0",
    "serverEncoding": "UTF-8",
    "serverResponseTime": "2024-04-29T19:57:27.718252",
    "serverPathVersion": "v1"
  }
}
```

- API-EndPoint  :  `POST /api/v1/oauth/login`

---

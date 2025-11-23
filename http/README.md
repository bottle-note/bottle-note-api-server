# HTTP API 테스트 파일 가이드

IntelliJ IDEA HTTP Client를 사용한 API 테스트 파일 모음입니다.

## 📁 폴더 구조 및 분류 기준

### 사용자 여정(User Journey) 기반 구조

```
http/
├── 01_회원관리/      # 사용자 계정 생애주기 (가입 → 사용 → 탈퇴)
├── 02_위스키탐색/    # 위스키 발견 및 정보 탐색
├── 03_리뷰_평가/     # 사용자 피드백 및 평가
├── 04_소셜/          # 사용자 간 상호작용
├── 05_지원/          # 고객 지원 및 비즈니스 문의
└── 99_공통/          # 전역 기능 (파일, 앱정보 등)
```

## 📝 새 HTTP 파일 추가 가이드

### 1. 적절한 폴더 찾기

**질문으로 분류하기:**
- "사용자가 언제 이 기능을 사용하나?" → 사용자 여정 단계 파악
- "이 기능의 주요 목적은?" → 도메인 카테고리 결정

**예시:**
```
새 기능: "위스키 추천 받기"
└─ 질문: 위스키를 탐색하는 중? → 02_위스키탐색/
   └─ 세부: 위스키를 찾는 기능? → 위스키찾기/
      └─ 파일명: 추천조회.http
```

### 2. 파일 네이밍 규칙

**기본 원칙:**
- ✅ 한글 사용 (직관성 우선)
- ✅ 명사형 (동작보다 대상 중심)
- ✅ 간결하게 (2-4단어)

**패턴별 네이밍:**

| 패턴 | 파일명 예시 | 설명 |
|------|------------|------|
| **조회만** | `마이페이지.http` | 단순 조회 |
| **CRUD 통합** | `문의관리.http` | 생성/조회/수정/삭제 모두 포함 |
| **특정 작업** | `리뷰작성_수정_삭제.http` | CUD만 분리 |
| **관련 기능** | `카테고리_지역.http` | 관련 조회 통합 |

**예시:**
```
❌ create-review.http          # 영문
❌ 리뷰를_작성한다.http         # 서술형
❌ 리뷰_등록_수정_삭제_조회.http # 너무 김
✅ 리뷰작성_수정_삭제.http      # 명확하고 간결
```

### 3. 파일 내용 작성 규칙

**기본 템플릿:**
```http
### [기능 설명]
[METHOD] {{host}}/api/v1/[path]
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "key": "value"
}
```

**변수 사용:**
```http
### 위스키 상세 조회
@alcoholId = 123
GET {{host}}/api/v1/alcohols/{{alcoholId}}
Authorization: Bearer {{accessToken}}
```

**여러 테스트 케이스:**
```http
### 정상 케이스
POST {{host}}/api/v1/reviews
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "alcoholId": 1,
  "content": "좋습니다"
}

### 실패 케이스 - 필수값 누락
POST {{host}}/api/v1/reviews
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "alcoholId": 1
}
```

## 🔧 환경 변수 설정

**http-client.env.json 사용:**
```json
{
  "local": {
    "host": "http://localhost:8080",
    "accessToken": ""
  },
  "dev": {
    "host": "https://api.development.bottle-note.com",
    "accessToken": ""
  }
}
```

**파일에서 사용:**
```http
GET {{host}}/api/v1/users/current
Authorization: Bearer {{accessToken}}
```

## 📂 폴더별 상세 설명

### 01_회원관리
```
가입_로그인/    # 계정 생성 및 인증
내정보/         # 프로필, 마이보틀 등 개인 정보
계정관리/       # 토큰, 탈퇴 등 계정 유지관리
```

### 02_위스키탐색
```
위스키찾기/     # 검색, 추천 등 발견 기능
위스키정보/     # 상세, 둘러보기 등 정보 확인
나만의위스키/   # 찜하기, 조회기록 등 개인화
```

### 03_리뷰_평가
```
리뷰/          # 리뷰 CRUD 및 좋아요
댓글/          # 리뷰 댓글 관리
별점/          # 별점 등록 및 조회
신고/          # 부적절한 콘텐츠 신고
```

### 04_소셜
```
[루트]         # 팔로우, 차단 등 기본 상호작용
히스토리/      # 사용자 활동 기록
```

### 05_지원
```
문의관리/       # 고객 문의 및 지원
비즈니스지원/   # B2B 문의 및 제휴
```

### 99_공통
```
전역적으로 사용되는 기능
- 파일 업로드
- 앱 정보 조회
- 푸시 알림
```

## 💡 작성 팁

### 토큰 자동 저장
```http
### 토큰 발급
POST {{host}}/api/v1/oauth/login
Content-Type: application/json

{
  "email": "test@example.com"
}

> {%
    client.global.set("accessToken", response.body.data.accessToken);
%}
```

### 주석 활용
```http
### 리뷰 작성
# @no-cookie-jar  # 쿠키 저장 방지
POST {{host}}/api/v1/reviews
```

### 실패 케이스 명시
```http
### 실패 케이스 - 권한 없음
DELETE {{host}}/api/v1/reviews/999
Authorization: Bearer {{accessToken}}
```

## 📋 체크리스트

새 HTTP 파일 추가 시 확인사항:

- [ ] 올바른 폴더 위치인가?
- [ ] 파일명이 명확하고 간결한가?
- [ ] `{{host}}`, `{{accessToken}}` 환경변수를 사용하는가?
- [ ] 각 요청에 설명(`###`)이 있는가?
- [ ] 필수 헤더(Authorization, Content-Type)가 포함되어 있는가?
- [ ] 요청 body가 실제 API 스펙과 일치하는가?

## 🔗 참고 링크

- [IntelliJ HTTP Client 공식 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [프로젝트 API 문서](../docs/api/) (존재하는 경우)

---

## 현재 폴더 구조

```
http/
├── http-client.env.json
├── _인증.http
│
├── 01_회원관리/
│   ├── 가입_로그인/
│   │   ├── 소셜회원가입.http
│   │   ├── 일반토큰발급.http
│   │   ├── 게스트토큰발급.http
│   │   ├── 인증v2.http
│   │   └── OAuth레거시.http
│   ├── 내정보/
│   │   ├── 마이페이지.http
│   │   ├── 프로필수정.http
│   │   ├── 마이보틀.http
│   │   └── 마이보틀세부조회.http
│   └── 계정관리/
│       ├── 토큰검증.http
│       ├── 디바이스토큰.http
│       ├── 유저리포트.http
│       └── 회원탈퇴.http
│
├── 02_위스키탐색/
│   ├── 위스키찾기/
│   │   ├── 통합검색.http
│   │   ├── 큐레이션검색.http
│   │   └── 인기위스키.http
│   ├── 위스키정보/
│   │   ├── 상세보기.http
│   │   ├── 둘러보기.http
│   │   └── 카테고리_지역.http
│   └── 나만의위스키/
│       ├── 찜하기.http
│       └── 조회기록.http
│
├── 03_리뷰_평가/
│   ├── 리뷰/
│   │   ├── 리뷰보기.http
│   │   ├── 리뷰작성_수정_삭제.http
│   │   └── 리뷰좋아요.http
│   ├── 댓글/
│   │   └── 댓글관리.http
│   ├── 별점/
│   │   └── 별점관리.http
│   └── 신고/
│       └── 신고관리.http
│
├── 04_소셜/
│   ├── 팔로우관리.http
│   ├── 차단관리.http
│   └── 히스토리/
│       └── 히스토리조회.http
│
├── 05_지원/
│   ├── 문의관리/
│   │   └── 문의관리.http
│   └── 비즈니스지원/
│       └── 비즈니스지원.http
│
└── 99_공통/
    ├── 파일업로드.http
    ├── 앱정보.http
    └── 푸시알림.http
```

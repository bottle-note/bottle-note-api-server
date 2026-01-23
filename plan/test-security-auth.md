# 보안/인증 테스트 추가

## 0. 왜 보안/인증 테스트인가

### 보안의 특수성

보안은 일반 기능과 근본적으로 다릅니다:
- 일반 기능 버그: 사용자 불편, 데이터 오류
- 보안 버그: 시스템 전체 침해, 개인정보 유출, 법적 책임

한 번 뚫리면 복구가 매우 어렵고 비용이 막대합니다.

### 보안 코드는 "잘 되는 것처럼 보이기" 쉽다

```java
public String generateToken(User user) {
    return jwtBuilder.build(user);
}
```

이 코드는 항상 토큰을 생성합니다. "잘 되네요!"라고 생각하기 쉽습니다.

하지만:
- 만료 시간이 제대로 설정되었나?
- 토큰 서명이 올바른가?
- 리프레시 토큰과 액세스 토큰의 차이가 있나?
- 만료된 토큰을 거부하나?

테스트 없이는 이런 질문에 답할 수 없습니다.

### 테스트 = 보안 점검표

보안 테스트는 단순한 코드 검증이 아닙니다:
- 정상 케이스: "로그인이 되는가?"
- 공격 케이스: "만료 토큰으로 접근하면 거부되는가?"
- 위조 케이스: "서명이 틀린 토큰으로 접근하면 거부되는가?"

각 테스트는 하나의 보안 점검 항목입니다.

### 규제 및 감사 대비

개인정보보호법, 금융권 보안 감사 등에서 요구하는 것:
- "인증 로직이 검증되었습니까?"
- "보안 테스트 결과를 제출하세요"

테스트가 없으면 이에 답할 수 없습니다.

### 라이브러리 업데이트의 안전망

보안 라이브러리는 자주 업데이트됩니다:
- JWT 라이브러리 취약점 발견 → 업데이트 필요
- 테스트 없음 → "업데이트하면 뭐가 깨질까?" → 못 함
- 테스트 있음 → 업데이트 후 테스트 실행 → 안전하게 확인

## 1. 현황 분석

### JWT 관련 컴포넌트

| 컴포넌트 | 위치 | 책임 | 테스트 상태 |
|---------|------|------|------------|
| JwtTokenProvider | bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/ | 토큰 생성 | 0% |
| JwtTokenValidator | bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/ | 토큰 검증 | 0% |
| AppleTokenValidator | bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/ | Apple 토큰 검증 | 0% |
| JwtAuthenticationFilter | bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/ | 요청 필터링 | 0% |
| JwtAuthenticationManager | bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/ | 인증 관리 | 0% |
| JwtAuthenticationEntryPoint | bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/ | 인증 실패 처리 | 0% |

### OAuth 인증 서비스

| 컴포넌트 | 위치 | 책임 | 테스트 상태 |
|---------|------|------|------------|
| KakaoAuthService | bottlenote-mono/src/main/java/app/bottlenote/user/service/ | 카카오 OAuth | 0% |
| AppleAuthService | bottlenote-mono/src/main/java/app/bottlenote/user/service/ | Apple OAuth | 0% |
| NonceService | bottlenote-mono/src/main/java/app/bottlenote/user/service/ | Nonce 관리 | 0% |
| KakaoFeignClient | bottlenote-mono/src/main/java/app/bottlenote/user/client/ | 카카오 API 호출 | 0% |

### Security 설정

| 컴포넌트 | 위치 | 책임 | 테스트 상태 |
|---------|------|------|------------|
| CustomUserDetailsService | bottlenote-mono/src/main/java/app/bottlenote/global/security/ | 사용자 로드 | 0% |
| SecurityContextUtil | bottlenote-mono/src/main/java/app/bottlenote/global/security/ | 컨텍스트 관리 | 0% |
| SecurityConfig | bottlenote-mono/src/main/java/app/bottlenote/global/security/ | 보안 설정 | 0% |

### 커버리지 현황

- 전체 보안 컴포넌트: 12개
- 테스트 존재: 0개
- Fake 구현체 존재: 일부 (테스트용)
- **현재 커버리지: 0%**
- **목표 커버리지: 90%**

### 목표 설정 근거

**왜 90%인가?**
- 보안은 한 번 뚫리면 치명적이므로 높은 커버리지 필수
- 핵심 인증 로직 (JwtTokenProvider, JwtTokenValidator)은 95% 목표
- 일부 설정 코드 및 예외 처리 경로는 제외 가능
- 100%는 과도하며 실질적 가치를 초과하는 노력 필요 (업계 표준 참조)

자세한 근거는 `0-개요.md`의 "커버리지 표준 및 근거" 참조

### 위험도 평가

현재 상태는 다음과 같은 위험을 내포합니다:
- JWT 만료 검증이 올바른지 확인 불가
- OAuth 인증 흐름의 정확성 확인 불가
- 보안 설정 변경 시 영향 범위 파악 불가
- 라이브러리 업데이트 시 안전성 검증 불가

## 2. 우선순위

### P0: 최우선 작성 대상

**JwtTokenProvider**
- 이유: 모든 인증의 시작점, 토큰 생성 로직
- 위험도: 매우 높음 (잘못된 토큰 생성 시 전체 인증 실패)
- 복잡도: 중간

**JwtTokenValidator**
- 이유: 토큰 검증의 핵심, 보안의 마지막 방어선
- 위험도: 매우 높음 (검증 실패 시 무단 접근 허용)
- 복잡도: 높음

**AppleTokenValidator**
- 이유: Apple OAuth의 핵심, 외부 의존성
- 위험도: 높음 (Apple 인증 사용자 접근 불가)
- 복잡도: 높음 (외부 API 의존)

### P1: 중요 작성 대상

**KakaoAuthService**
- 이유: 카카오 OAuth 핵심 로직
- 위험도: 높음
- 복잡도: 중간

**AppleAuthService**
- 이유: Apple OAuth 핵심 로직
- 위험도: 높음
- 복잡도: 중간

**CustomUserDetailsService**
- 이유: Spring Security 사용자 로드
- 위험도: 중간
- 복잡도: 낮음

### P2: 보통 작성 대상

**NonceService**
- 이유: Nonce 관리
- 위험도: 중간
- 복잡도: 낮음

**SecurityContextUtil**
- 이유: 컨텍스트 관리 유틸
- 위험도: 낮음
- 복잡도: 낮음

## 3. 구체적 테스트 시나리오

### JwtTokenProvider

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/JwtTokenProvider.java`

#### 액세스 토큰 생성

**시나리오 1: 정상적인 액세스 토큰 생성**
- 시나리오: 유효한 사용자 정보로 액세스 토큰을 생성할 때 올바른 토큰이 생성되어야 한다
- 왜: 기본 동작 검증, 토큰 생성의 정확성 보장
- 테스트 방법: 사용자 정보 전달 후 토큰 생성, 토큰 파싱하여 클레임 검증

**시나리오 2: 만료 시간 설정**
- 시나리오: 생성된 액세스 토큰의 만료 시간이 24시간으로 설정되어야 한다
- 왜: 토큰 수명 정책 준수 확인
- 테스트 방법: 토큰 생성 후 만료 시간 클레임 확인

**시나리오 3: 사용자 정보 포함**
- 시나리오: 토큰에 사용자 ID, 권한 등 필수 정보가 포함되어야 한다
- 왜: 토큰으로부터 사용자 식별 가능성 보장
- 테스트 방법: 토큰 파싱 후 사용자 정보 클레임 검증

**시나리오 4: null 사용자 정보 처리**
- 시나리오: null 사용자 정보로 토큰 생성 시도 시 예외가 발생해야 한다
- 왜: 잘못된 입력 방어
- 테스트 방법: null 전달, 예외 발생 확인

#### 리프레시 토큰 생성

**시나리오 1: 정상적인 리프레시 토큰 생성**
- 시나리오: 유효한 사용자 정보로 리프레시 토큰을 생성할 때 올바른 토큰이 생성되어야 한다
- 왜: 리프레시 토큰 기능 검증
- 테스트 방법: 사용자 정보 전달 후 리프레시 토큰 생성, 검증

**시나리오 2: 만료 시간 설정**
- 시나리오: 생성된 리프레시 토큰의 만료 시간이 30일로 설정되어야 한다
- 왜: 리프레시 토큰 수명 정책 준수 확인
- 테스트 방법: 토큰 생성 후 만료 시간 클레임 확인

**시나리오 3: 액세스 토큰과 구분**
- 시나리오: 리프레시 토큰과 액세스 토큰이 명확히 구분되어야 한다
- 왜: 토큰 타입 혼동 방지
- 테스트 방법: 두 토큰 생성 후 타입 클레임 또는 만료 시간으로 구분 확인

### JwtTokenValidator

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/JwtTokenValidator.java`

#### 토큰 검증

**시나리오 1: 유효한 토큰 검증 성공**
- 시나리오: 올바르게 생성된 토큰을 검증할 때 성공해야 한다
- 왜: 정상 케이스 검증
- 테스트 방법: Provider로 토큰 생성 후 Validator로 검증, 성공 확인

**시나리오 2: 만료된 토큰 거부**
- 시나리오: 만료된 토큰을 검증할 때 실패해야 한다
- 왜: 보안의 핵심, 만료 토큰으로 접근 방지
- 테스트 방법: 만료 시간이 과거인 토큰 생성 후 검증, 실패 확인

**시나리오 3: 서명이 틀린 토큰 거부**
- 시나리오: 서명이 변조된 토큰을 검증할 때 실패해야 한다
- 왜: 위조 토큰 방어
- 테스트 방법: 토큰 서명 부분 변조 후 검증, 실패 확인

**시나리오 4: 잘못된 형식의 토큰 거부**
- 시나리오: JWT 형식이 아닌 문자열을 검증할 때 실패해야 한다
- 왜: 잘못된 입력 방어
- 테스트 방법: "invalid-token" 같은 문자열로 검증, 실패 확인

**시나리오 5: null 또는 빈 토큰 거부**
- 시나리오: null 또는 빈 문자열 토큰을 검증할 때 실패해야 한다
- 왜: null 안전성 보장
- 테스트 방법: null, "" 등으로 검증, 실패 확인

**시나리오 6: 사용자 정보 추출**
- 시나리오: 유효한 토큰에서 사용자 정보를 정확히 추출해야 한다
- 왜: 토큰으로부터 인증 정보 획득
- 테스트 방법: 토큰 검증 후 사용자 ID, 권한 등 추출, 원본과 일치 확인

#### 토큰 갱신

**시나리오 1: 리프레시 토큰으로 액세스 토큰 갱신**
- 시나리오: 유효한 리프레시 토큰으로 새 액세스 토큰을 발급받을 수 있어야 한다
- 왜: 토큰 갱신 기능 검증
- 테스트 방법: 리프레시 토큰으로 갱신 요청, 새 액세스 토큰 발급 확인

**시나리오 2: 만료된 리프레시 토큰으로 갱신 거부**
- 시나리오: 만료된 리프레시 토큰으로 갱신 시도 시 실패해야 한다
- 왜: 만료된 리프레시 토큰 방어
- 테스트 방법: 만료된 리프레시 토큰으로 갱신, 실패 확인

**시나리오 3: 액세스 토큰으로 갱신 시도 거부**
- 시나리오: 액세스 토큰으로 갱신 시도 시 실패해야 한다
- 왜: 토큰 타입 혼동 방지
- 테스트 방법: 액세스 토큰으로 갱신 요청, 실패 확인

### AppleTokenValidator

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/global/security/jwt/AppleTokenValidator.java`

#### Apple ID 토큰 검증

**시나리오 1: 유효한 Apple ID 토큰 검증**
- 시나리오: Apple에서 발급한 유효한 ID 토큰을 검증할 때 성공해야 한다
- 왜: Apple 인증 기본 동작 검증
- 테스트 방법: Mock Apple 토큰으로 검증, 성공 확인

**시나리오 2: 만료된 Apple 토큰 거부**
- 시나리오: 만료된 Apple ID 토큰을 검증할 때 실패해야 한다
- 왜: 만료 토큰 방어
- 테스트 방법: 만료된 토큰으로 검증, 실패 확인

**시나리오 3: Apple 공개키로 서명 검증**
- 시나리오: Apple 공개키로 토큰 서명을 검증해야 한다
- 왜: 위조 토큰 방지
- 테스트 방법: 잘못된 서명의 토큰으로 검증, 실패 확인

**시나리오 4: nonce 검증**
- 시나리오: 토큰의 nonce가 요청 시 전달한 nonce와 일치해야 한다
- 왜: 재생 공격 방지
- 테스트 방법: 다른 nonce로 검증, 실패 확인

**시나리오 5: audience 검증**
- 시나리오: 토큰의 audience가 우리 앱의 client ID와 일치해야 한다
- 왜: 다른 앱용 토큰 거부
- 테스트 방법: 다른 audience의 토큰으로 검증, 실패 확인

### KakaoAuthService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/user/service/KakaoAuthService.java`

#### 카카오 인증

**시나리오 1: 카카오 액세스 토큰으로 사용자 정보 조회**
- 시나리오: 유효한 카카오 액세스 토큰으로 사용자 정보를 조회할 수 있어야 한다
- 왜: 카카오 인증 기본 기능 검증
- 테스트 방법: Mock 카카오 API 응답 설정 후 조회, 사용자 정보 확인

**시나리오 2: 유효하지 않은 토큰 처리**
- 시나리오: 유효하지 않은 카카오 토큰으로 조회 시 예외가 발생해야 한다
- 왜: 잘못된 토큰 방어
- 테스트 방법: 잘못된 토큰으로 조회, 예외 발생 확인

**시나리오 3: 카카오 API 오류 처리**
- 시나리오: 카카오 API가 오류를 반환할 때 적절히 처리해야 한다
- 왜: 외부 서비스 장애 대응
- 테스트 방법: Mock API 오류 응답 설정 후 조회, 예외 처리 확인

**시나리오 4: 사용자 정보 매핑**
- 시나리오: 카카오 응답을 우리 User 엔티티로 정확히 매핑해야 한다
- 왜: 데이터 정확성 보장
- 테스트 방법: Mock 카카오 응답으로 조회, 매핑 결과 검증

### AppleAuthService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/user/service/AppleAuthService.java`

#### Apple 인증

**시나리오 1: Apple ID 토큰으로 사용자 정보 조회**
- 시나리오: 유효한 Apple ID 토큰으로 사용자 정보를 조회할 수 있어야 한다
- 왜: Apple 인증 기본 기능 검증
- 테스트 방법: Mock Apple 토큰으로 조회, 사용자 정보 확인

**시나리오 2: 토큰 검증 실패 처리**
- 시나리오: Apple 토큰 검증 실패 시 예외가 발생해야 한다
- 왜: 잘못된 토큰 방어
- 테스트 방법: 검증 실패하도록 설정 후 조회, 예외 확인

**시나리오 3: 사용자 정보 매핑**
- 시나리오: Apple 토큰의 클레임을 우리 User 엔티티로 정확히 매핑해야 한다
- 왜: 데이터 정확성 보장
- 테스트 방법: Mock Apple 토큰으로 조회, 매핑 결과 검증

### NonceService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/user/service/NonceService.java`

#### Nonce 관리

**시나리오 1: Nonce 생성**
- 시나리오: 새로운 Nonce를 생성할 때 유일한 값이어야 한다
- 왜: 재생 공격 방지
- 테스트 방법: 여러 번 생성 후 모두 다른 값 확인

**시나리오 2: Nonce 검증 성공**
- 시나리오: 생성된 Nonce를 검증할 때 성공해야 한다
- 왜: 정상 케이스 검증
- 테스트 방법: 생성 후 즉시 검증, 성공 확인

**시나리오 3: 사용된 Nonce 재사용 거부**
- 시나리오: 이미 사용된 Nonce를 다시 사용할 때 실패해야 한다
- 왜: 재생 공격 방지
- 테스트 방법: 검증 후 재검증, 실패 확인

**시나리오 4: 만료된 Nonce 거부**
- 시나리오: 만료된 Nonce를 검증할 때 실패해야 한다
- 왜: 오래된 요청 거부
- 테스트 방법: 시간 경과 후 검증, 실패 확인

### CustomUserDetailsService

**파일 위치**: `bottlenote-mono/src/main/java/app/bottlenote/global/security/CustomUserDetailsService.java`

#### 사용자 로드

**시나리오 1: 사용자 ID로 UserDetails 로드**
- 시나리오: 존재하는 사용자 ID로 UserDetails를 로드할 수 있어야 한다
- 왜: Spring Security 연동 기본 기능
- 테스트 방법: 사용자 ID로 loadUserByUsername 호출, UserDetails 반환 확인

**시나리오 2: 존재하지 않는 사용자 처리**
- 시나리오: 존재하지 않는 사용자 ID로 로드 시 예외가 발생해야 한다
- 왜: 잘못된 사용자 방어
- 테스트 방법: 존재하지 않는 ID로 호출, UsernameNotFoundException 확인

**시나리오 3: 권한 매핑**
- 시나리오: User 엔티티의 role을 GrantedAuthority로 정확히 매핑해야 한다
- 왜: 권한 기반 접근 제어 보장
- 테스트 방법: 로드 후 권한 목록 확인

## 4. 체크리스트

### JwtTokenProvider

- [ ] 액세스 토큰 정상 생성
- [ ] 액세스 토큰 만료 시간 24시간 설정
- [ ] 액세스 토큰에 사용자 정보 포함
- [ ] null 사용자 정보 처리
- [ ] 리프레시 토큰 정상 생성
- [ ] 리프레시 토큰 만료 시간 30일 설정
- [ ] 액세스 토큰과 리프레시 토큰 구분

### JwtTokenValidator

- [ ] 유효한 토큰 검증 성공
- [ ] 만료된 토큰 거부
- [ ] 서명이 틀린 토큰 거부
- [ ] 잘못된 형식의 토큰 거부
- [ ] null 또는 빈 토큰 거부
- [ ] 토큰에서 사용자 정보 정확히 추출
- [ ] 리프레시 토큰으로 액세스 토큰 갱신
- [ ] 만료된 리프레시 토큰으로 갱신 거부
- [ ] 액세스 토큰으로 갱신 시도 거부

### AppleTokenValidator

- [ ] 유효한 Apple ID 토큰 검증
- [ ] 만료된 Apple 토큰 거부
- [ ] Apple 공개키로 서명 검증
- [ ] nonce 검증
- [ ] audience 검증

### KakaoAuthService

- [ ] 카카오 액세스 토큰으로 사용자 정보 조회
- [ ] 유효하지 않은 토큰 처리
- [ ] 카카오 API 오류 처리
- [ ] 사용자 정보 매핑

### AppleAuthService

- [ ] Apple ID 토큰으로 사용자 정보 조회
- [ ] 토큰 검증 실패 처리
- [ ] 사용자 정보 매핑

### NonceService

- [ ] Nonce 생성 - 유일성
- [ ] Nonce 검증 성공
- [ ] 사용된 Nonce 재사용 거부
- [ ] 만료된 Nonce 거부

### CustomUserDetailsService

- [ ] 사용자 ID로 UserDetails 로드
- [ ] 존재하지 않는 사용자 처리
- [ ] 권한 매핑

### 테스트 파일 생성

- [ ] JwtTokenProviderTest.java 생성
- [ ] JwtTokenValidatorTest.java 생성
- [ ] AppleTokenValidatorTest.java 생성
- [ ] KakaoAuthServiceTest.java 생성
- [ ] AppleAuthServiceTest.java 생성
- [ ] NonceServiceTest.java 생성
- [ ] CustomUserDetailsServiceTest.java 생성

### 통합 테스트

- [ ] JWT 전체 흐름 통합 테스트 (생성 → 검증 → 갱신)
- [ ] 카카오 OAuth 통합 테스트
- [ ] Apple OAuth 통합 테스트
- [ ] 인증 필터 통합 테스트

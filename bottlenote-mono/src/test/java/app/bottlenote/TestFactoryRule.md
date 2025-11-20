# TestFactory 작성 규칙

> 순수하게 **영속화된 엔티티만 생성**하여 **완전한 상태로 반환**하는 테스트 전용 유틸리티

## 핵심 철학 (5가지)

1. **단일 책임**: 엔티티 생성만
2. **격리**: 메서드 밖에서 즉시 사용 가능 (ID 할당 완료)
3. **순수성**: EntityManager만 사용
4. **명시성**: @NotNull/@Nullable 필수
5. **응집성**: 하나의 Factory = 하나의 애그리거트

## 필수 규칙

- `@Component` 선언
- `EntityManager`만 주입 (Repository 금지 ❌)
- 다른 Factory 주입 금지 ❌
- `@NotNull`/`@Nullable` 모든 파라미터/반환값에 명시
- `persist{Entity}` 명명 패턴
- `@Transactional` + `em.flush()` 필수 (격리 보장)

## 애그리거트 경계

- UserTestFactory: User, Follow
- AlcoholTestFactory: Alcohol, Region, Distillery
- ReviewTestFactory: Review, ReviewReply, ReviewImage
- RatingTestFactory: Rating
- BusinessSupportTestFactory: BusinessSupport

## 조합 방법

- 테스트에서 여러 Factory 직접 조합
- 복잡한 경우: scenario/ 패키지

## 예시

```java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Component
public class UserTestFactory {
    @Autowired private EntityManager em;

    @Transactional
    @NotNull
    public User persistUser() {
        User user = User.builder()
            .email("user@example.com")
            .nickName("사용자")
            .build();
        em.persist(user);
        em.flush(); // ✅ 격리: 메서드 내에서 완료
        return user; // 즉시 사용 가능
    }

    @Transactional
    @NotNull
    public User persistUser(
        @NotNull String email,
        @Nullable String nickName
    ) {
        String finalNickName = nickName != null ? nickName : "기본닉네임";
        User user = User.builder()
            .email(email)
            .nickName(finalNickName)
            .build();
        em.persist(user);
        em.flush();
        return user;
    }
}
```

## 체크리스트

팩토리 메서드 작성 시 확인:

- [ ] **단일 책임**: 엔티티만 생성하는가?
- [ ] **격리**: `em.flush()` 완료 후 반환하는가?
- [ ] **순수성**: EntityManager만 사용하는가?
- [ ] **명시성**: @NotNull/@Nullable이 모든 곳에 있는가?
- [ ] **응집성**: 다른 Factory를 주입하지 않았는가?

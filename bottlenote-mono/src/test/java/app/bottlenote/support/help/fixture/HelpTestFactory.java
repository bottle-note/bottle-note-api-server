package app.bottlenote.support.help.fixture;

import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.domain.Help;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Help 테스트 엔티티 팩토리
 *
 * <p>5가지 원칙: 1. 단일 책임: Help 엔티티 생성만 2. 격리: em.flush()로 즉시 반영 3. 순수성: EntityManager만 사용 4.
 * 명시성: @NotNull/@Nullable 명시 5. 응집성: 다른 팩토리 의존 없음
 */
@Component
@RequiredArgsConstructor
public class HelpTestFactory {

  private final EntityManager em;

  /** 기본 Help 생성 (최소 필드) */
  @Transactional
  @NotNull
  public Help persistHelp(@NotNull Long userId, @NotNull HelpType type) {
    Help help =
        Help.builder()
            .userId(userId)
            .type(type)
            .title("기본 문의 제목")
            .content("기본 문의 내용")
            .responseContent("")
            .build();
    em.persist(help);
    em.flush();
    return help;
  }

  /** 상세 정보를 포함한 Help 생성 */
  @Transactional
  @NotNull
  public Help persistHelp(
      @NotNull Long userId,
      @NotNull HelpType type,
      @NotNull String title,
      @NotNull String content) {
    Help help =
        Help.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .content(content)
            .responseContent("")
            .build();
    em.persist(help);
    em.flush();
    return help;
  }

  /** 완전한 Help 생성 (모든 필드) */
  @Transactional
  @NotNull
  public Help persistHelp(
      @NotNull Long userId,
      @NotNull HelpType type,
      @NotNull String title,
      @NotNull String content,
      @Nullable Long adminId,
      @Nullable String responseContent) {
    Help help =
        Help.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .content(content)
            .adminId(adminId)
            .responseContent(responseContent != null ? responseContent : "")
            .build();
    em.persist(help);
    em.flush();
    return help;
  }

  /** 여러 Help 생성 (특정 사용자) */
  @Transactional
  @NotNull
  public List<Help> persistMultipleHelpsByUser(
      @NotNull Long userId, @NotNull HelpType type, int count) {
    List<Help> helps = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      Help help =
          Help.builder()
              .userId(userId)
              .type(type)
              .title("문의 제목 " + i)
              .content("문의 내용 " + i)
              .responseContent("")
              .build();
      em.persist(help);
      helps.add(help);
    }
    em.flush();
    return helps;
  }
}

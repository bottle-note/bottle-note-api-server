package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.TastingTag;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class TastingTagTestFactory {

  private static final AtomicInteger counter = new AtomicInteger(0);

  @Autowired private EntityManager em;

  @Transactional
  @NotNull
  public TastingTag persistTastingTag() {
    TastingTag tag =
        TastingTag.builder()
            .korName("테스트 태그-" + generateRandomSuffix())
            .engName("test-tag-" + generateRandomSuffix())
            .description("테스트용 태그입니다")
            .build();
    em.persist(tag);
    em.flush();
    return tag;
  }

  @Transactional
  @NotNull
  public TastingTag persistTastingTag(@NotNull String korName, @NotNull String engName) {
    TastingTag tag =
        TastingTag.builder().korName(korName).engName(engName).description("테스트용 태그입니다").build();
    em.persist(tag);
    em.flush();
    return tag;
  }

  @Transactional
  @NotNull
  public TastingTag persistTastingTagWithParent(@NotNull TastingTag parent) {
    TastingTag tag =
        TastingTag.builder()
            .korName("자식 태그-" + generateRandomSuffix())
            .engName("child-tag-" + generateRandomSuffix())
            .description("부모가 있는 태그입니다")
            .parentId(parent.getId())
            .build();
    em.persist(tag);
    em.flush();
    return tag;
  }

  @Transactional
  @NotNull
  public TastingTag persistTastingTagWithParent(
      @NotNull String korName, @NotNull String engName, @NotNull TastingTag parent) {
    TastingTag tag =
        TastingTag.builder()
            .korName(korName)
            .engName(engName)
            .description("부모가 있는 태그입니다")
            .parentId(parent.getId())
            .build();
    em.persist(tag);
    em.flush();
    return tag;
  }

  /** 3depth 트리 구조 생성 (대분류 -> 중분류 -> 소분류) */
  @Transactional
  @NotNull
  public List<TastingTag> persistTastingTagTree() {
    List<TastingTag> tags = new ArrayList<>();

    TastingTag root =
        TastingTag.builder()
            .korName("향-" + generateRandomSuffix())
            .engName("Aroma-" + generateRandomSuffix())
            .description("대분류 태그")
            .build();
    em.persist(root);
    tags.add(root);

    TastingTag middle =
        TastingTag.builder()
            .korName("달콤한-" + generateRandomSuffix())
            .engName("Sweet-" + generateRandomSuffix())
            .description("중분류 태그")
            .parentId(root.getId())
            .build();
    em.persist(middle);
    tags.add(middle);

    TastingTag leaf =
        TastingTag.builder()
            .korName("허니-" + generateRandomSuffix())
            .engName("Honey-" + generateRandomSuffix())
            .description("소분류 태그")
            .parentId(middle.getId())
            .build();
    em.persist(leaf);
    tags.add(leaf);

    em.flush();
    return tags;
  }

  @Transactional
  @NotNull
  public AlcoholsTastingTags linkAlcoholToTag(@NotNull Alcohol alcohol, @NotNull TastingTag tag) {
    AlcoholsTastingTags mapping = AlcoholsTastingTags.of(alcohol, tag);
    em.persist(mapping);
    em.flush();
    return mapping;
  }

  private String generateRandomSuffix() {
    return String.valueOf(counter.incrementAndGet());
  }
}

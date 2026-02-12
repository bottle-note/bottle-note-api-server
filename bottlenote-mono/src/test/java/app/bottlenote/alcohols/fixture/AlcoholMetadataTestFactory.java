package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.Region;
import app.bottlenote.alcohols.domain.TastingTag;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class AlcoholMetadataTestFactory {

  private static final AtomicInteger counter = new AtomicInteger(0);

  @Autowired private EntityManager em;

  // ========== TastingTag ==========

  @Transactional
  @NotNull
  public TastingTag persistTastingTag() {
    return persistTastingTag("테스트태그", "test-tag");
  }

  @Transactional
  @NotNull
  public TastingTag persistTastingTag(@NotNull String korName, @NotNull String engName) {
    TastingTag tag = TastingTag.builder().korName(korName).engName(engName).build();
    em.persist(tag);
    em.flush();
    return tag;
  }

  @Transactional
  @NotNull
  public List<TastingTag> persistTastingTags(@NotNull List<String[]> tagNames) {
    return tagNames.stream().map(names -> persistTastingTag(names[0], names[1])).toList();
  }

  @Transactional
  @NotNull
  public List<TastingTag> persistDefaultTastingTags() {
    return persistTastingTags(
        List.of(
            new String[] {"바닐라", "vanilla"},
            new String[] {"꿀", "honey"},
            new String[] {"스모키", "smoky"},
            new String[] {"피트", "peat"},
            new String[] {"오크", "oak"},
            new String[] {"카라멜", "caramel"},
            new String[] {"시트러스", "citrus"},
            new String[] {"초콜릿", "chocolate"}));
  }

  // ========== Region ==========

  @Transactional
  @NotNull
  public Region persistRegion() {
    return persistRegion("스코틀랜드", "Scotland");
  }

  @Transactional
  @NotNull
  public Region persistRegion(@NotNull String korName, @NotNull String engName) {
    Region region =
        Region.builder()
            .korName(korName + "-" + generateRandomSuffix())
            .engName(engName + "-" + generateRandomSuffix())
            .continent("Europe")
            .build();
    em.persist(region);
    em.flush();
    return region;
  }

  // ========== Distillery ==========

  @Transactional
  @NotNull
  public Distillery persistDistillery() {
    return persistDistillery("맥캘란", "Macallan");
  }

  @Transactional
  @NotNull
  public Distillery persistDistillery(@NotNull String korName, @NotNull String engName) {
    Distillery distillery =
        Distillery.builder()
            .korName(korName + "-" + generateRandomSuffix())
            .engName(engName + "-" + generateRandomSuffix())
            .logoImgPath("https://example.com/logo.jpg")
            .build();
    em.persist(distillery);
    em.flush();
    return distillery;
  }

  private String generateRandomSuffix() {
    return String.valueOf(counter.incrementAndGet());
  }
}

package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagCatalogItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupRefItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagGroupRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("테이스팅 태그 경량 조회 어드민 서비스")
class DefaultAdminTastingTagCatalogServiceTest {

  InMemoryTastingTagRepository tastingTagRepository;
  InMemoryAlcoholsTastingTagsRepository alcoholsTastingTagsRepository;
  InMemoryTastingTagGroupRepository tastingTagGroupRepository;
  DefaultAdminTastingTagCatalogService service;

  @BeforeEach
  void setUp() {
    tastingTagRepository = new InMemoryTastingTagRepository();
    alcoholsTastingTagsRepository = new InMemoryAlcoholsTastingTagsRepository();
    tastingTagGroupRepository = new InMemoryTastingTagGroupRepository();
    service =
        new DefaultAdminTastingTagCatalogService(
            tastingTagRepository, alcoholsTastingTagsRepository, tastingTagGroupRepository);
  }

  @Test
  @DisplayName("전체 태그를 조회할 때 한글 이름 순으로 위스키 수와 그룹을 반환한다")
  void 전체_태그를_한글_이름_순으로_조회할_수_있다() {
    TastingTag later = saveTag("나나", "Nana", null, null);
    TastingTag earlier = saveTag("가나", "Gana", null, null);
    TastingTag child = saveTag("가나다", "Ganada", earlier.getId(), null);
    alcoholsTastingTagsRepository.link(1L, later);
    alcoholsTastingTagsRepository.link(2L, later);
    alcoholsTastingTagsRepository.link(3L, child);
    TastingTagGroup group = TastingTagGroup.create("ALPHA", "알파", "A", null, 1);
    group.putTag(later.getId(), TastingTagGroupRole.CORE);
    tastingTagGroupRepository.save(group);

    List<AdminTastingTagCatalogItem> items = service.findAll();

    assertThat(items)
        .extracting(AdminTastingTagCatalogItem::korName)
        .containsExactly("가나", "가나다", "나나");
    assertThat(items)
        .extracting(AdminTastingTagCatalogItem::alcoholCount)
        .containsExactly(0L, 1L, 2L);
    assertThat(items.get(1).parentId()).isEqualTo(earlier.getId());
    assertThat(items.get(0).groups()).isEmpty();
    assertThat(items.get(2).groups()).singleElement().satisfies(this::assertCoreAlpha);
  }

  @Test
  @DisplayName("태그가 여러 그룹에 속할 때 그룹을 계열 순으로 모두 반환한다")
  void 여러_그룹에_속한_태그를_조회할_수_있다() {
    TastingTag tag = saveTag("피트", "Peat", null, null);
    TastingTagGroup beta = TastingTagGroup.create("BETA", "베타", "B", null, 1);
    beta.putTag(tag.getId(), TastingTagGroupRole.PERIPHERAL);
    tastingTagGroupRepository.save(beta);
    TastingTagGroup alpha = TastingTagGroup.create("ALPHA", "알파", "A", null, 2);
    alpha.putTag(tag.getId(), TastingTagGroupRole.CORE);
    tastingTagGroupRepository.save(alpha);

    List<AdminTastingTagCatalogItem> items = service.findAll();

    assertThat(items.get(0).groups())
        .extracting(AdminTastingTagGroupRefItem::groupCode)
        .containsExactly("ALPHA", "BETA");
    assertThat(items.get(0).groups())
        .extracting(group -> group.role().code())
        .containsExactly("CORE", "PERIPHERAL");
  }

  @Test
  @DisplayName("미소속 태그를 조회할 때 위스키 수가 많은 순으로 반환하고 소속 태그는 제외한다")
  void 미소속_태그를_위스키_수_내림차순으로_조회할_수_있다() {
    TastingTag grouped = saveTag("나나", "Nana", null, null);
    saveTag("가나", "Gana", null, null);
    TastingTag many = saveTag("다다", "Dada", null, null);
    alcoholsTastingTagsRepository.link(1L, grouped);
    alcoholsTastingTagsRepository.link(2L, many);
    alcoholsTastingTagsRepository.link(3L, many);
    TastingTagGroup group = TastingTagGroup.create("ALPHA", "알파", "A", null, 0);
    group.putTag(grouped.getId(), TastingTagGroupRole.CORE);
    tastingTagGroupRepository.save(group);

    List<AdminTastingTagCatalogItem> items = service.findUngrouped(null);

    assertThat(items).extracting(AdminTastingTagCatalogItem::korName).containsExactly("다다", "가나");
    assertThat(items).extracting(AdminTastingTagCatalogItem::alcoholCount).containsExactly(2L, 0L);
    assertThat(items).allSatisfy(item -> assertThat(item.groups()).isEmpty());
  }

  @Test
  @DisplayName("keyword가 한글 이름 일부일 때 그 미소속 태그만 반환한다")
  void 한글_이름_부분_일치로_미소속_태그를_조회할_수_있다() {
    saveTag("가나", "Honey", null, null);
    TastingTag match = saveTag("가나다", "Other", null, null);
    alcoholsTastingTagsRepository.link(1L, match);

    assertThat(service.findUngrouped(" 나다 "))
        .extracting(AdminTastingTagCatalogItem::korName)
        .containsExactly("가나다");
    assertThat(service.findUngrouped("Honey")).isEmpty();
    assertThat(service.findUngrouped("   ")).hasSize(2);
  }

  @Test
  @DisplayName("아이콘이 있거나 없을 때 그 값만 반환한다")
  void 아이콘을_조회할_수_있다() {
    TastingTag withIcon = saveTag("허니", "Honey", null, "QUJD");
    TastingTag withoutIcon = saveTag("오크", "Oak", null, null);

    assertThat(service.findIcon(withIcon.getId()).icon()).isEqualTo("QUJD");
    assertThat(service.findIcon(withoutIcon.getId()).icon()).isNull();
  }

  @Test
  @DisplayName("없는 태그의 아이콘을 조회할 때 예외를 던진다")
  void 없는_태그의_아이콘_조회를_거절할_수_있다() {
    assertThatThrownBy(() -> service.findIcon(999L))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(AlcoholExceptionCode.TASTING_TAG_NOT_FOUND);
  }

  private void assertCoreAlpha(AdminTastingTagGroupRefItem group) {
    assertThat(group.groupCode()).isEqualTo("ALPHA");
    assertThat(group.groupName()).isEqualTo("알파");
    assertThat(group.role())
        .isEqualTo(
            new AdminCodeLabelItem(
                TastingTagGroupRole.CORE.name(),
                TastingTagGroupRole.CORE.getKorName(),
                TastingTagGroupRole.CORE.getEngName(),
                TastingTagGroupRole.CORE.getDescription()));
  }

  private TastingTag saveTag(String korName, String engName, Long parentId, String icon) {
    return tastingTagRepository.save(
        TastingTag.builder()
            .korName(korName)
            .engName(engName)
            .parentId(parentId)
            .icon(icon)
            .build());
  }
}

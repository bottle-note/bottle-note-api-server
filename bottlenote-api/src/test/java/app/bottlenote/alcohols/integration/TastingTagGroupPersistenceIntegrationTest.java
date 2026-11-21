package app.bottlenote.alcohols.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.domain.TastingTagGroupRepository;
import app.bottlenote.alcohols.domain.TastingTagGroupTag;
import app.bottlenote.alcohols.fixture.TastingTagTestFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
@DisplayName("[integration] 맛 그룹 영속성")
class TastingTagGroupPersistenceIntegrationTest extends IntegrationTestSupport {

  @Autowired private TastingTagTestFactory tastingTagTestFactory;
  @Autowired private TastingTagGroupRepository tastingTagGroupRepository;

  @Test
  @DisplayName("그룹을 태그 연결과 함께 저장하고 코드로 다시 읽는다")
  void 그룹을_태그와_함께_저장하고_읽는다() {
    TastingTag peat = tastingTagTestFactory.persistTastingTag("피트", "peat");
    TastingTag salty = tastingTagTestFactory.persistTastingTag("솔티", "salty");
    TastingTagGroup group = TastingTagGroup.create("PEAT_SMOKY", "피트·스모키", "스카치", "이탄 연기", 1);
    group.putTag(peat.getId(), TastingTagGroupRole.CORE);
    group.putTag(salty.getId(), TastingTagGroupRole.PERIPHERAL);

    tastingTagGroupRepository.save(group);

    TastingTagGroup found = tastingTagGroupRepository.findByCode("PEAT_SMOKY").orElseThrow();
    assertThat(found.getName()).isEqualTo("피트·스모키");
    assertThat(found.getTags()).hasSize(2);
    assertThat(found.coreTagIds()).containsExactly(peat.getId());
    assertThat(found.findTag(salty.getId()))
        .map(TastingTagGroupTag::getRole)
        .contains(TastingTagGroupRole.PERIPHERAL);
    assertThat(tastingTagGroupRepository.existsByCode("PEAT_SMOKY")).isTrue();
    assertThat(tastingTagGroupRepository.existsByNameAndIdNot("피트·스모키", found.getId())).isFalse();
  }

  @Test
  @DisplayName("연결을 교체해 저장하면 이전 연결은 사라지고 전체 목록은 계열·순서로 정렬된다")
  void 연결을_교체하고_전체를_정렬해_읽는다() {
    TastingTag peat = tastingTagTestFactory.persistTastingTag("피트", "peat");
    TastingTag smoke = tastingTagTestFactory.persistTastingTag("스모키", "smoke");
    TastingTagGroup scotch =
        tastingTagGroupRepository.save(
            TastingTagGroup.create("PEAT_SMOKY", "피트·스모키", "스카치", null, 2));
    tastingTagGroupRepository.save(TastingTagGroup.create("SHERRY", "셰리·건과일", "스카치", null, 1));
    tastingTagGroupRepository.save(TastingTagGroup.create("AM_RYE", "스파이시·호밀", "아메리칸", null, 0));

    TastingTagGroup loaded = tastingTagGroupRepository.findById(scotch.getId()).orElseThrow();
    loaded.putTag(peat.getId(), TastingTagGroupRole.CORE);
    tastingTagGroupRepository.save(loaded);
    loaded = tastingTagGroupRepository.findById(scotch.getId()).orElseThrow();
    loaded.replaceTags(List.of(TastingTagGroupTag.of(smoke.getId(), TastingTagGroupRole.CORE)));
    tastingTagGroupRepository.save(loaded);

    List<TastingTagGroup> all = tastingTagGroupRepository.findAllWithTags();
    assertThat(all)
        .extracting(TastingTagGroup::getCode)
        .containsExactly("SHERRY", "PEAT_SMOKY", "AM_RYE");
    TastingTagGroup replaced = all.get(1);
    assertThat(replaced.coreTagIds()).containsExactly(smoke.getId());
    assertThat(replaced.findTag(peat.getId())).isEmpty();

    tastingTagGroupRepository.delete(replaced);
    assertThat(tastingTagGroupRepository.findByCode("PEAT_SMOKY")).isEmpty();
  }
}

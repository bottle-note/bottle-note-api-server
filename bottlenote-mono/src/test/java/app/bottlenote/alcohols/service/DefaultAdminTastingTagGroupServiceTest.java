package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_DUPLICATE_CODE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_DUPLICATE_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_HAS_TAGS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_INVALID_CODE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_TAG_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagRoleRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagsRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupListResponse;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupTagItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagGroupRepository;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

@Tag("unit")
@DisplayName("맛 그룹 어드민 서비스")
@SuppressWarnings("NonAsciiCharacters")
class DefaultAdminTastingTagGroupServiceTest {

  InMemoryTastingTagGroupRepository groupRepository;
  InMemoryTastingTagRepository tastingTagRepository;
  InMemoryAlcoholsTastingTagsRepository alcoholsTastingTagsRepository;
  DefaultAdminTastingTagGroupService service;

  @BeforeEach
  void setUp() {
    groupRepository = new InMemoryTastingTagGroupRepository();
    tastingTagRepository = new InMemoryTastingTagRepository();
    alcoholsTastingTagsRepository = new InMemoryAlcoholsTastingTagsRepository();
    service =
        new DefaultAdminTastingTagGroupService(
            groupRepository, tastingTagRepository, alcoholsTastingTagsRepository);
  }

  @Test
  @DisplayName("그룹이 없을 때 빈 목록과 빈 계열을 돌려준다")
  void 빈_목록을_조회할_수_있다() {
    AdminTastingTagGroupListResponse response = service.findAll();

    assertThat(response.groups()).isEmpty();
    assertThat(response.families()).isEmpty();
  }

  @Test
  @DisplayName("그룹을 포트 순서대로 내려주고 계열은 처음 나온 순서로 모은다")
  void 목록과_계열을_조회할_수_있다() {
    TastingTag peat = saveTag("피트");
    TastingTag vanilla = saveTag("바닐라");
    TastingTag oak = saveTag("오크");
    alcoholsTastingTagsRepository.link(1L, peat);
    alcoholsTastingTagsRepository.link(2L, peat);

    service.create(createRequest("SCOTCH_LATE", "늦은 스카치", "스카치", null, 2));
    service.addTags(
        "SCOTCH_LATE",
        new AdminTastingTagGroupTagsRequest(List.of(oak.getId()), TastingTagGroupRole.PERIPHERAL));
    service.create(createRequest("BOURBON", "버번", "버번", "달콤", 9));
    service.addTags(
        "BOURBON",
        new AdminTastingTagGroupTagsRequest(
            List.of(vanilla.getId(), peat.getId()), TastingTagGroupRole.PERIPHERAL));
    service.changeTagRole(
        "BOURBON", peat.getId(), new AdminTastingTagGroupTagRoleRequest(TastingTagGroupRole.CORE));
    service.create(createRequest("UNGROUPED", "계열 없음", null, null, 0));

    AdminTastingTagGroupListResponse response = service.findAll();

    assertThat(response.groups())
        .extracting(AdminTastingTagGroupItem::code)
        .containsExactly("BOURBON", "SCOTCH_LATE", "UNGROUPED");
    assertThat(response.families()).containsExactly("버번", "스카치");

    AdminTastingTagGroupItem bourbon = response.groups().get(0);
    assertThat(bourbon.tags())
        .extracting(AdminTastingTagGroupTagItem::korName)
        .containsExactly("피트", "바닐라");
    AdminTastingTagGroupTagItem core = bourbon.tags().get(0);
    assertThat(core.tastingTagId()).isEqualTo(peat.getId());
    assertThat(core.role().code()).isEqualTo("CORE");
    assertThat(core.role().korName()).isEqualTo("대표 태그");
    assertThat(core.provider().code()).isEqualTo("ADMIN");
    assertThat(core.provider().korName()).isEqualTo("관리자");
    assertThat(core.alcoholCount()).isEqualTo(2L);
    assertThat(bourbon.tags().get(1).alcoholCount()).isZero();
    assertThat(bourbon.tags().get(1).role().code()).isEqualTo("PERIPHERAL");
  }

  @Test
  @DisplayName("코드를 다듬어 그룹을 만들고 같은 코드·이름은 거부한다")
  void 그룹을_등록할_수_있다() {
    AdminResultResponse created =
        service.create(createRequest(" PEAT_SMOKY ", " 피트 ", " 스카치 ", "  ", 3));

    assertThat(created.code()).isEqualTo("TASTING_TAG_GROUP_CREATED");
    assertThat(created.targetId()).isNotNull();
    AdminTastingTagGroupItem detail = service.getDetail("PEAT_SMOKY");
    assertThat(detail.name()).isEqualTo("피트");
    assertThat(detail.family()).isEqualTo("스카치");
    assertThat(detail.description()).isNull();
    assertThat(detail.displayOrder()).isEqualTo(3);
    assertThat(detail.tags()).isEmpty();

    assertThatThrownBy(() -> service.create(createRequest("PEAT_SMOKY", "다른 이름", null, null, 0)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_DUPLICATE_CODE);
    assertThatThrownBy(() -> service.create(createRequest("OTHER", "피트", null, null, 0)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_DUPLICATE_NAME);
    assertThatThrownBy(() -> service.create(createRequest("bad", "이름", null, null, 0)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_INVALID_CODE);
  }

  @Test
  @DisplayName("없는 그룹을 조회할 때 찾을 수 없다고 한다")
  void 없는_그룹은_조회할_수_없다() {
    assertThatThrownBy(() -> service.getDetail("NO_SUCH"))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_NOT_FOUND);
  }

  @Test
  @DisplayName("이름만 바꾸고 코드와 생략한 노출 순서는 유지한다")
  void 그룹을_수정할_수_있다() {
    service.create(createRequest("PEAT", "피트", "스카치", "설명", 4));
    service.create(createRequest("SHERRY", "셰리", null, null, 1));

    AdminResultResponse updated =
        service.update("PEAT", new AdminTastingTagGroupUpdateRequest(" 피트 연기 ", "아일라", null, null));

    assertThat(updated.code()).isEqualTo("TASTING_TAG_GROUP_UPDATED");
    AdminTastingTagGroupItem detail = service.getDetail("PEAT");
    assertThat(detail.code()).isEqualTo("PEAT");
    assertThat(detail.name()).isEqualTo("피트 연기");
    assertThat(detail.family()).isEqualTo("아일라");
    assertThat(detail.description()).isNull();
    assertThat(detail.displayOrder()).isEqualTo(4);

    assertThatThrownBy(
            () ->
                service.update(
                    "PEAT", new AdminTastingTagGroupUpdateRequest("셰리", null, null, null)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_DUPLICATE_NAME);
    assertThat(service.getDetail("PEAT").name()).isEqualTo("피트 연기");
  }

  @Test
  @DisplayName("태그가 없으면 삭제하고 태그가 있으면 거부한다")
  void 그룹을_삭제할_수_있다() {
    TastingTag peat = saveTag("피트");
    AdminResultResponse created = service.create(createRequest("PEAT", "피트", null, null, 0));
    service.addTags(
        "PEAT",
        new AdminTastingTagGroupTagsRequest(List.of(peat.getId()), TastingTagGroupRole.CORE));

    assertThatThrownBy(() -> service.delete("PEAT"))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_HAS_TAGS);

    service.removeTag("PEAT", peat.getId());
    AdminResultResponse deleted = service.delete("PEAT");

    assertThat(deleted.code()).isEqualTo("TASTING_TAG_GROUP_DELETED");
    assertThat(deleted.targetId()).isEqualTo(created.targetId());
    assertThat(groupRepository.findByCode("PEAT")).isEmpty();
  }

  @Test
  @DisplayName("없는 태그는 연결하지 않고 이미 있는 태그는 역할만 바꾼다")
  void 태그를_연결할_수_있다() {
    TastingTag peat = saveTag("피트");
    service.create(createRequest("PEAT", "피트", null, null, 0));

    assertThatThrownBy(
            () ->
                service.addTags(
                    "PEAT",
                    new AdminTastingTagGroupTagsRequest(List.of(999L), TastingTagGroupRole.CORE)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_NOT_FOUND);
    assertThat(service.getDetail("PEAT").tags()).isEmpty();

    service.addTags(
        "PEAT",
        new AdminTastingTagGroupTagsRequest(List.of(peat.getId()), TastingTagGroupRole.CORE));
    AdminResultResponse changed =
        service.addTags(
            "PEAT",
            new AdminTastingTagGroupTagsRequest(
                List.of(peat.getId()), TastingTagGroupRole.PERIPHERAL));

    assertThat(changed.code()).isEqualTo("TASTING_TAG_GROUP_TAG_ADDED");
    assertThat(service.getDetail("PEAT").tags()).hasSize(1);
    assertThat(service.getDetail("PEAT").tags().get(0).role().code()).isEqualTo("PERIPHERAL");
  }

  @Test
  @DisplayName("그룹에 없는 태그의 역할은 바꾸지 못한다")
  void 없는_태그의_역할은_바꿀_수_없다() {
    service.create(createRequest("PEAT", "피트", null, null, 0));

    assertThatThrownBy(
            () ->
                service.changeTagRole(
                    "PEAT", 1L, new AdminTastingTagGroupTagRoleRequest(TastingTagGroupRole.CORE)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_TAG_NOT_FOUND);
  }

  @Test
  @DisplayName("그룹에 없는 태그를 제거할 때 찾을 수 없다고 한다")
  void 없는_태그는_제거할_수_없다() {
    service.create(createRequest("PEAT", "피트", null, null, 0));

    assertThatThrownBy(() -> service.removeTag("PEAT", 1L))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_TAG_NOT_FOUND);
  }

  private AdminTastingTagGroupCreateRequest createRequest(
      String code, String name, String family, String description, Integer displayOrder) {
    return new AdminTastingTagGroupCreateRequest(code, name, family, description, displayOrder);
  }

  @Test
  @DisplayName("저장 중 코드 유니크 키가 겹치면 코드 중복으로 거절한다")
  void 동시에_코드가_겹치면_중복_코드로_거절한다() {
    service =
        new DefaultAdminTastingTagGroupService(
            new DuplicateOnInsertRepository("uk_tasting_tag_groups_code"),
            tastingTagRepository,
            alcoholsTastingTagsRepository);

    assertThatThrownBy(() -> service.create(createRequest("PEAT", "피트", null, null, 0)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_DUPLICATE_CODE);
  }

  @Test
  @DisplayName("저장 중 이름 유니크 키가 겹치면 이름 중복으로 거절한다")
  void 동시에_이름이_겹치면_중복_이름으로_거절한다() {
    service =
        new DefaultAdminTastingTagGroupService(
            new DuplicateOnInsertRepository("uk_tasting_tag_groups_name"),
            tastingTagRepository,
            alcoholsTastingTagsRepository);

    assertThatThrownBy(() -> service.create(createRequest("PEAT", "피트", null, null, 0)))
        .isInstanceOf(AlcoholException.class)
        .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
        .isEqualTo(TASTING_TAG_GROUP_DUPLICATE_NAME);
  }

  private TastingTag saveTag(String korName) {
    return tastingTagRepository.save(
        TastingTag.builder().korName(korName).engName(korName).build());
  }

  /** 선검사를 통과한 뒤 저장에서만 유니크 위반을 낸다. */
  private static final class DuplicateOnInsertRepository extends InMemoryTastingTagGroupRepository {

    private final String constraintName;

    private DuplicateOnInsertRepository(String constraintName) {
      this.constraintName = constraintName;
    }

    @Override
    public TastingTagGroup save(TastingTagGroup group) {
      throw new DataIntegrityViolationException(
          "could not execute statement [Duplicate entry for key '" + constraintName + "']");
    }
  }
}

package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.dto.request.AdminDistilleryUpsertRequest;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.DistilleryTestFactory;
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository;
import app.bottlenote.global.dto.response.AdminResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DistilleryService 단위 테스트")
class DistilleryServiceTest {

  InMemoryDistilleryRepository distilleryRepository;
  AlcoholQueryRepository alcoholQueryRepository;
  DistilleryService distilleryService;

  @BeforeEach
  void setUp() {
    distilleryRepository = new InMemoryDistilleryRepository();
    alcoholQueryRepository = mock(AlcoholQueryRepository.class);
    distilleryService = new DistilleryService(distilleryRepository, alcoholQueryRepository);
  }

  @Nested
  @DisplayName("getDetail 메서드")
  class GetDetail {

    @Test
    @DisplayName("존재하는 ID로 조회하면 증류소 정보를 반환한다")
    void 존재하는_ID로_조회할_수_있다() {
      Distillery saved =
          distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));

      AdminDistilleryItem result = distilleryService.getDetail(saved.getId());

      assertThat(result.id()).isEqualTo(saved.getId());
      assertThat(result.korName()).isEqualTo("맥캘란");
      assertThat(result.engName()).isEqualTo("Macallan");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 DISTILLERY_NOT_FOUND 예외가 발생한다")
    void 존재하지_않는_ID로_조회하면_예외가_발생한다() {
      assertThatThrownBy(() -> distilleryService.getDetail(999L))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_NOT_FOUND.getMessage());
    }
  }

  @Nested
  @DisplayName("create 메서드")
  class Create {

    @Test
    @DisplayName("유효한 요청으로 생성하면 DISTILLERY_CREATED 응답을 반환한다")
    void 유효한_요청으로_생성할_수_있다() {
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest("토버모리", "Tobermory", null);

      AdminResultResponse result = distilleryService.create(request);

      assertThat(result.code()).isEqualTo("DISTILLERY_CREATED");
      assertThat(result.targetId()).isNotNull();
    }

    @Test
    @DisplayName("동일한 한글 이름이 존재하면 DISTILLERY_DUPLICATE_NAME 예외가 발생한다")
    void 한글_이름_중복_시_예외가_발생한다() {
      distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest("맥캘란", "Different", null);

      assertThatThrownBy(() -> distilleryService.create(request))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_DUPLICATE_NAME.getMessage());
    }

    @Test
    @DisplayName("동일한 영문 이름이 존재하면 DISTILLERY_DUPLICATE_NAME 예외가 발생한다")
    void 영문_이름_중복_시_예외가_발생한다() {
      distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest("다른이름", "Macallan", null);

      assertThatThrownBy(() -> distilleryService.create(request))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_DUPLICATE_NAME.getMessage());
    }
  }

  @Nested
  @DisplayName("update 메서드")
  class Update {

    @Test
    @DisplayName("유효한 요청으로 수정하면 DISTILLERY_UPDATED 응답을 반환한다")
    void 유효한_요청으로_수정할_수_있다() {
      Distillery saved =
          distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest("맥캘란 12", "Macallan 12", null);

      AdminResultResponse result = distilleryService.update(saved.getId(), request);

      assertThat(result.code()).isEqualTo("DISTILLERY_UPDATED");
      assertThat(result.targetId()).isEqualTo(saved.getId());
      assertThat(saved.getKorName()).isEqualTo("맥캘란 12");
      assertThat(saved.getEngName()).isEqualTo("Macallan 12");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 수정하면 DISTILLERY_NOT_FOUND 예외가 발생한다")
    void 존재하지_않는_ID로_수정하면_예외가_발생한다() {
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest("토버모리", "Tobermory", null);

      assertThatThrownBy(() -> distilleryService.update(999L, request))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("자기 자신을 제외한 한글 이름 중복이 있으면 DISTILLERY_DUPLICATE_NAME 예외가 발생한다")
    void 자기_자신_제외_한글_이름_중복_시_예외가_발생한다() {
      distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      Distillery target =
          distilleryRepository.save(DistilleryTestFactory.createDistillery("토버모리", "Tobermory"));
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest("맥캘란", "Tobermory", null);

      assertThatThrownBy(() -> distilleryService.update(target.getId(), request))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_DUPLICATE_NAME.getMessage());
    }

    @Test
    @DisplayName("자기 자신의 이름은 그대로 유지하며 수정할 수 있다")
    void 자기_자신의_이름은_유지하며_수정할_수_있다() {
      Distillery saved =
          distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      AdminDistilleryUpsertRequest request =
          new AdminDistilleryUpsertRequest(
              "맥캘란", "Macallan", "https://cdn.example.com/distillery/macallan.jpg");

      AdminResultResponse result = distilleryService.update(saved.getId(), request);

      assertThat(result.code()).isEqualTo("DISTILLERY_UPDATED");
      assertThat(saved.getImageUrl()).isEqualTo("https://cdn.example.com/distillery/macallan.jpg");
    }
  }

  @Nested
  @DisplayName("delete 메서드")
  class Delete {

    @Test
    @DisplayName("연관 위스키가 없으면 DISTILLERY_DELETED 응답을 반환한다")
    void 연관_위스키가_없으면_삭제할_수_있다() {
      Distillery saved =
          distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      when(alcoholQueryRepository.existsByDistilleryId(saved.getId())).thenReturn(false);

      AdminResultResponse result = distilleryService.delete(saved.getId());

      assertThat(result.code()).isEqualTo("DISTILLERY_DELETED");
      assertThat(result.targetId()).isEqualTo(saved.getId());
      assertThat(distilleryRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 삭제하면 DISTILLERY_NOT_FOUND 예외가 발생한다")
    void 존재하지_않는_ID로_삭제하면_예외가_발생한다() {
      assertThatThrownBy(() -> distilleryService.delete(999L))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("연관 위스키가 있으면 DISTILLERY_HAS_ALCOHOLS 예외가 발생한다")
    void 연관_위스키가_있으면_예외가_발생한다() {
      Distillery saved =
          distilleryRepository.save(DistilleryTestFactory.createDistillery("맥캘란", "Macallan"));
      when(alcoholQueryRepository.existsByDistilleryId(saved.getId())).thenReturn(true);

      assertThatThrownBy(() -> distilleryService.delete(saved.getId()))
          .isInstanceOf(AlcoholException.class)
          .hasMessageContaining(AlcoholExceptionCode.DISTILLERY_HAS_ALCOHOLS.getMessage());
    }
  }
}

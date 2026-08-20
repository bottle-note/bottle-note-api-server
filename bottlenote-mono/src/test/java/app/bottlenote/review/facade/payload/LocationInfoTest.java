package app.bottlenote.review.facade.payload;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.review.domain.ReviewLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LocationInfo")
class LocationInfoTest {

  @Test
  @DisplayName("모든 위치 값이 있으면 리뷰 위치를 응답 계약으로 투영한다")
  void 모든_위치_값이_있으면_리뷰_위치를_응답_계약으로_투영한다() {
    ReviewLocation reviewLocation =
        ReviewLocation.builder()
            .name("도시술")
            .zipCode("12345")
            .address("서울 송파구 송파대로 145")
            .detailAddress("2층")
            .category("음식점 > 술집")
            .mapUrl("https://example.com/place")
            .latitude("37.0000")
            .longitude("127.0000")
            .build();

    LocationInfo actual = LocationInfo.from(reviewLocation);

    assertThat(actual)
        .isEqualTo(
            new LocationInfo(
                "도시술",
                "12345",
                "서울 송파구 송파대로 145",
                "2층",
                "음식점 > 술집",
                "https://example.com/place",
                "37.0000",
                "127.0000"));
  }

  @Test
  @DisplayName("위치가 없거나 모든 위치 값이 비어 있으면 null로 정규화한다")
  void 위치가_없거나_모든_위치_값이_비어_있으면_null로_정규화한다() {
    assertThat(LocationInfo.from(null)).isNull();
    assertThat(LocationInfo.from(ReviewLocation.empty())).isNull();
  }

  @Test
  @DisplayName("일부 위치 값만 있으면 값과 null을 그대로 보존한다")
  void 일부_위치_값만_있으면_값과_null을_그대로_보존한다() {
    ReviewLocation reviewLocation = ReviewLocation.builder().name("도시술").address("서울 송파구").build();

    LocationInfo actual = LocationInfo.from(reviewLocation);

    assertThat(actual)
        .isEqualTo(
            new LocationInfo("도시술", null, "서울 송파구", null, null, null, null, null));
  }
}

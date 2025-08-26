package app.bottlenote.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class ReviewLocation {

  @Comment("상호 명")
  @Column(name = "location_name")
  private String name;

  @Comment("우편 번호")
  @Column(name = "zip_code")
  private String zipCode;

  @Comment("도로명 주소")
  @Column(name = "address")
  private String address;

  @Comment("상세 주소")
  @Column(name = "detail_address")
  private String detailAddress;

  @Comment("카테고리")
  @Column(name = "category")
  private String category;

  @Comment("지도 URL")
  @Column(name = "map_url")
  private String mapUrl;

  @Comment("위도(x좌표)")
  @Column(name = "latitude")
  private String latitude;

  @Comment("경도(y좌표)")
  @Column(name = "longitude")
  private String longitude;

  public static ReviewLocation empty() {
    return ReviewLocation.builder().build();
  }
}

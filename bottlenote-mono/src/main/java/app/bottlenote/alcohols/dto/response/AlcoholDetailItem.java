package app.bottlenote.alcohols.dto.response;

import app.bottlenote.global.data.serializers.CustomDeserializers.TagListDeserializer;
import app.bottlenote.global.data.serializers.CustomSerializers.TagListSerializer;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlcoholDetailItem {
  private Long alcoholId;
  private String alcoholUrlImg;
  private String korName;
  private String engName;
  private String korCategory;
  private String engCategory;
  private String korRegion;
  private String engRegion;
  private String cask;
  private String abv;
  private String korDistillery;
  private String engDistillery;
  private Double rating;
  private Long totalRatingsCount;
  private Double myRating;

  @JsonPropertyDescription("인증 사용자가 해당 알코올에 남긴 ACTIVE 리뷰 중 최신(id 최대) 1건의 별점, 없으면 0.0")
  private Double myAvgRating;

  private Boolean isPicked;
  private Long reviewCount;
  private Long pickCount;

  @JsonSerialize(using = TagListSerializer.class)
  @JsonDeserialize(using = TagListDeserializer.class)
  private String alcoholsTastingTags;
}

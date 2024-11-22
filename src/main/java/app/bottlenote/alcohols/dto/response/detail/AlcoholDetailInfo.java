package app.bottlenote.alcohols.dto.response.detail;

import app.bottlenote.global.data.serializers.CustomDeserializers.TagListDeserializer;
import app.bottlenote.global.data.serializers.CustomSerializers.TagListSerializer;
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
public class AlcoholDetailInfo {
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
	private Double myAvgRating;
	private Boolean isPicked;

	@JsonSerialize(using = TagListSerializer.class)
	@JsonDeserialize(using = TagListDeserializer.class)
	private String alcoholsTastingTags;
}

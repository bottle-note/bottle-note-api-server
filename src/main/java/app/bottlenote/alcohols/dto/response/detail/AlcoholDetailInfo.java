package app.bottlenote.alcohols.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
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
	private String avg;
	private String korDistillery;
	private String engDistillery;
	private Double rating;
	private Long totalRatings;
	private Double myRating;
	private Boolean isPicked;
	private List<String> tags;
}

package app.bottlenote.alcohols.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AlcoholDetailInfo {
	List<String> tags; // class java.util.List
	private Long alcoholId;   // class java.lang.Long,
	private String koreanCategory;   // class java.lang.String,
	private String englishCategory;   // class java.lang.String,
	private String thumbnailImage;   // class java.lang.String,
	private String koreanName;   // class java.lang.String,
	private String englishName;   // class java.lang.String,
	private String koreanRegion;   // class java.lang.String,
	private String englishRegion;   // class java.lang.String,
	private String englishCask;   // class java.lang.String,
	private String alcoholContent;   // % 기호 제거해야함.
	private String koreanDistillery;   // class java.lang.String,
	private String englishDistillery;   // class java.lang.String,
	private Double averageRating;   // class java.lang.Double,
	private Long totalRatings;   // class java.lang.Long,
	private Double myRating;   // class java.lang.Double,
	private Boolean isPicked;   // class java.lang.Boolean

	 public static class Tag{
		private String tag;

	 }
}

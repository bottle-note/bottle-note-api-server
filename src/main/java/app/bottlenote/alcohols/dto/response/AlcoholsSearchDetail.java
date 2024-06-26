package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AlcoholsSearchDetail {
	private Long alcoholId;
	private String korName;
	private String engName;
	private String korCategoryName;
	private String engCategoryName;
	private String imageUrl;
	private Double rating;
	private Long ratingCount;
	private Long reviewCount;
	private Long pickCount;
	private Boolean isPicked;
}

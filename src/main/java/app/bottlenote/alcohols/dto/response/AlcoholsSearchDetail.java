package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
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
	private Boolean picked;


	/**
	 * Dto Injection 을 위한 기본 생성자 사용 주의
	 */
	public AlcoholsSearchDetail() {
	}
}

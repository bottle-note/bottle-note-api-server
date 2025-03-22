package app.bottlenote.alcohols.dto.response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record AlcoholInfo(
	Long alcoholId,
	String korName,
	String engName,
	String korCategoryName,
	String engCategoryName,
	String imageUrl,
	Boolean isPicked
) {
	public static AlcoholInfo empty() {
		log.error("데이터가 불일치합니다. 데이터 정합성 확인이 필요합니다");
		return new AlcoholInfo(null, null, null,
			null, null, null, null);
	}
}

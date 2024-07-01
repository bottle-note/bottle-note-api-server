package app.bottlenote.alcohols.dto.response;

public record AlcoholInfo(
	Long alcoholId,
	String korName,
	String engName,
	String korCategoryName,
	String engCategoryName,
	String imageUrl,
	Boolean isPicked

) {

}

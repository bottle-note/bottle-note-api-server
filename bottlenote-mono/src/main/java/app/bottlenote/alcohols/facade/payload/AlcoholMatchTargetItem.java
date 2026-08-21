package app.bottlenote.alcohols.facade.payload;

/** MFDS 매칭 등 대량 비교용 알코올 요약. 비교에 필요한 필드만 담는다. */
public record AlcoholMatchTargetItem(
    Long alcoholId,
    String korName,
    String engName,
    String abv,
    String age,
    String korCategory,
    String engCategory,
    Long regionId,
    String korRegion,
    String engRegion,
    Long distilleryId,
    String korDistillery,
    String engDistillery,
    String imageUrl) {}

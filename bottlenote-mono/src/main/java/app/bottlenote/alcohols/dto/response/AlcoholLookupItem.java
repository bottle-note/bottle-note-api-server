package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;

public record AlcoholLookupItem(
    Long alcoholId,
    String korName,
    String engName,
    String korCategoryName,
    String engCategoryName,
    AlcoholCategoryGroup categoryGroup,
    Long regionId,
    String korRegion,
    String engRegion,
    Long distilleryId,
    String korDistillery,
    String engDistillery,
    String imageUrl) {}

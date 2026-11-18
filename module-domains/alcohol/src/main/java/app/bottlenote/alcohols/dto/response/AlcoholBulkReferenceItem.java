package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;

public record AlcoholBulkReferenceItem(
    Long alcoholId,
    String korName,
    String engName,
    String korCategory,
    String engCategory,
    AlcoholCategoryGroup categoryGroup,
    AlcoholType type,
    Long distilleryId,
    String abv,
    String volume) {}

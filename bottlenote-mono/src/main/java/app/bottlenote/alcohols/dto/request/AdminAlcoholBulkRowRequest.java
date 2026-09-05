package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.serializer.AlcoholBulkIdDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

public record AdminAlcoholBulkRowRequest(
    String clientRowId,
    String korName,
    String engName,
    String abv,
    String type,
    String korCategory,
    String engCategory,
    String categoryGroup,
    @JsonDeserialize(using = AlcoholBulkIdDeserializer.class) Long regionId,
    @JsonDeserialize(using = AlcoholBulkIdDeserializer.class) Long distilleryId,
    String age,
    String cask,
    String description,
    String volume,
    @JsonDeserialize(contentUsing = AlcoholBulkIdDeserializer.class) List<Long> tastingTagIds,
    String imageUrl) {}

package app.bottlenote.alcohols.dto.response;

import java.util.List;

public record AdminAlcoholAttributeBulkResponse(int created, int updated, List<Long> ids) {}

package app.bottlenote.curation.dto.response;

import java.util.List;

public record CurationFeedListResponse(List<ProductSpecBasedCurationFeedItemResponse> items) {}

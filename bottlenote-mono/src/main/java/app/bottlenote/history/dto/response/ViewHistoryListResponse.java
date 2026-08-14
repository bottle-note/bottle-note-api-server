package app.bottlenote.history.dto.response;

import app.bottlenote.alcohols.dto.response.ViewHistoryItem;
import java.util.List;

public record ViewHistoryListResponse(List<ViewHistoryItem> items) {}

package app.bottlenote.campaigncontent.dto.request;

public record AdminCampaignContentSearchRequest(
    String keyword, Boolean isActive, Integer page, Integer size) {

  public AdminCampaignContentSearchRequest {
    keyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    page = page != null && page >= 0 ? page : 0;
    size = size != null && size > 0 ? size : 20;
  }
}

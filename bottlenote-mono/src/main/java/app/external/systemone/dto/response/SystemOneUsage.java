package app.external.systemone.dto.response;

public record SystemOneUsage(long inputTokens, long outputTokens) {

  public static SystemOneUsage empty() {
    return new SystemOneUsage(0, 0);
  }
}

package app.bottlenote.rating.dto.response;

import lombok.Getter;

public record RatingRegisterResponse(String rating, String message) {

  public static RatingRegisterResponse success(double rating) {
    return new RatingRegisterResponse(String.valueOf(rating), Message.SUCCESS.getMessage());
  }

  public static RatingRegisterResponse fail() {
    return new RatingRegisterResponse(null, Message.FAIL.getMessage());
  }

  @Getter
  public enum Message {
    SUCCESS("별점 등록 성공"),
    FAIL("별점 등록 실패");

    private final String message;

    Message(String message) {
      this.message = message;
    }
  }
}

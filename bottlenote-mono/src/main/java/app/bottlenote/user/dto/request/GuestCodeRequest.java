package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GuestCodeRequest(@NotBlank(message = "REQUIRED_GUEST_CODE") String code) {
  public static GuestCodeRequest of(String code) {
    return new GuestCodeRequest(code);
  }
}

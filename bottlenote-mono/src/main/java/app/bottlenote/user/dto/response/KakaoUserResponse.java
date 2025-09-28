package app.bottlenote.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record KakaoUserResponse(
    Long id,
    @JsonProperty("connected_at") LocalDateTime connectedAt,
    @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
  public record KakaoAccount(
      @JsonProperty("profile_needs_agreement") Boolean profileNeedsAgreement,
      Profile profile,
      @JsonProperty("name_needs_agreement") Boolean nameNeedsAgreement,
      String name,
      @JsonProperty("email_needs_agreement") Boolean emailNeedsAgreement,
      String email,
      @JsonProperty("is_email_valid") Boolean isEmailValid,
      @JsonProperty("is_email_verified") Boolean isEmailVerified,
      @JsonProperty("age_range_needs_agreement") Boolean ageRangeNeedsAgreement,
      @JsonProperty("age_range") String ageRange,
      @JsonProperty("gender_needs_agreement") Boolean genderNeedsAgreement,
      String gender) {}

  public record Profile(
      String nickname,
      @JsonProperty("thumbnail_image_url") String thumbnailImageUrl,
      @JsonProperty("profile_image_url") String profileImageUrl,
      @JsonProperty("is_default_image") Boolean isDefaultImage) {}
}

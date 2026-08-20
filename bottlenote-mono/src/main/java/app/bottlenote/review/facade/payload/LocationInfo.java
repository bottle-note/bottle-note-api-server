package app.bottlenote.review.facade.payload;

import app.bottlenote.review.domain.ReviewLocation;
import jakarta.validation.constraints.Pattern;

public record LocationInfo(
    String locationName,
    @Pattern(regexp = "^\\d{5}$", message = "INVALID_ZIP_CODE_PATTERN") String zipCode,
    String address,
    String detailAddress,
    String category,
    String mapUrl,
    String latitude,
    String longitude) {

  public static LocationInfo from(ReviewLocation reviewLocation) {
    if (reviewLocation == null) {
      return null;
    }
    return of(
        reviewLocation.getName(),
        reviewLocation.getZipCode(),
        reviewLocation.getAddress(),
        reviewLocation.getDetailAddress(),
        reviewLocation.getCategory(),
        reviewLocation.getMapUrl(),
        reviewLocation.getLatitude(),
        reviewLocation.getLongitude());
  }

  public static LocationInfo of(
      String locationName,
      String zipCode,
      String address,
      String detailAddress,
      String category,
      String mapUrl,
      String latitude,
      String longitude) {
    if (locationName == null
        && zipCode == null
        && address == null
        && detailAddress == null
        && category == null
        && mapUrl == null
        && latitude == null
        && longitude == null) {
      return null;
    }
    return new LocationInfo(
        locationName, zipCode, address, detailAddress, category, mapUrl, latitude, longitude);
  }
}

package app.bottlenote.shared.review.payload;

import lombok.Builder;

@Builder
public record LocationInfo(
    String locationName,
    String zipCode,
    String address,
    String detailAddress,
    String category,
    String mapUrl,
    String latitude,
    String longitude) {}

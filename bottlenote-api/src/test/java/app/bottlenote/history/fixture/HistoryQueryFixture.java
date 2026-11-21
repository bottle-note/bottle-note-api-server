package app.bottlenote.history.fixture;

import app.bottlenote.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.dto.response.UserHistoryItem;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HistoryQueryFixture {

  public ViewHistoryItem getFixtureViewHistoryItem(Long alcoholId, String korName) {
    return ViewHistoryItem.builder()
        .alcoholId(alcoholId)
        .korName(korName)
        .engName("engName")
        .korCategory("싱글 몰트")
        .engCategory("single malt")
        .rating(Math.round((Math.random() * 10) / 2) * 0.5)
        .ratingCount(new Random().nextLong(100) + 1)
        .korCategory("싱글 몰트")
        .engCategory("single molt")
        .imageUrl("https://i.imgur.com/TE2nmYV.png")
        .isPicked(alcoholId % 2 == 0)
        .popularScore(Math.round(new Random().nextDouble()) / 100.0)
        .viewAt(LocalDateTime.now())
        .build();
  }

  public KeysetPageResponse<UserHistorySearchResponse> getUserHistorySearchResponse() {
    UserHistoryItem detail1 =
        UserHistoryItem.builder()
            .historyId(1L)
            .createdAt(LocalDateTime.now())
            .eventCategory(EventCategory.PICK)
            .eventType(EventType.IS_PICK)
            .alcoholId(1L)
            .alcoholName("소주")
            .dynamicMessage(null)
            .imageUrl("imageUrl")
            .redirectUrl("redirectUrl")
            .build();

    UserHistoryItem detail2 =
        UserHistoryItem.builder()
            .historyId(1L)
            .createdAt(LocalDateTime.now())
            .eventCategory(EventCategory.RATING)
            .eventType(EventType.REVIEW_CREATE)
            .alcoholId(1L)
            .alcoholName("소주")
            .imageUrl("imageUrl")
            .redirectUrl("redirectUrl")
            .dynamicMessage(Map.of("currentValue", "4.0"))
            .build();

    List<UserHistoryItem> details = List.of(detail1, detail2);
    UserHistorySearchResponse response = UserHistorySearchResponse.of(LocalDateTime.now(), details);
    return KeysetPageResponse.of(response, new KeysetPagination(true, "next"));
  }
}

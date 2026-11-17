package app.bottlenote.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.facade.FollowFacade;
import app.bottlenote.user.facade.payload.FriendItem;
import app.bottlenote.user.fixture.UserTestFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Tag("integration")
class TastingFriendsQueryIntegrationTest extends IntegrationTestSupport {

  @Autowired private FollowFacade followFacade;
  @Autowired private UserTestFactory userTestFactory;
  @Autowired private AlcoholTestFactory alcoholTestFactory;
  @Autowired private RatingTestFactory ratingTestFactory;

  @Test
  @DisplayName("시음 친구를 조회할 때 팔로잉과 별점을 확인하고 정렬과 페이지 범위를 적용한다")
  void 시음_친구의_조건과_페이지를_보존한다() {
    var owner = userTestFactory.persistUser();
    var alcohol = alcoholTestFactory.persistAlcohol();
    var otherAlcohol = alcoholTestFactory.persistAlcohol();
    List<Long> expectedIds = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      var friend = userTestFactory.persistUser();
      userTestFactory.persistFollow(owner, friend);
      ratingTestFactory.persistRating(friend, alcohol, 4);
      expectedIds.add(friend.getId());
    }
    var unfollowed = userTestFactory.persistUser();
    userTestFactory.persistFollow(
        Follow.builder()
            .userId(owner.getId())
            .targetUserId(unfollowed.getId())
            .status(FollowStatus.UNFOLLOW));
    ratingTestFactory.persistRating(unfollowed, alcohol, 4);
    var otherTaster = userTestFactory.persistUser();
    userTestFactory.persistFollow(owner, otherTaster);
    ratingTestFactory.persistRating(otherTaster, otherAlcohol, 4);
    var unrated = userTestFactory.persistUser();
    userTestFactory.persistFollow(owner, unrated);
    var stranger = userTestFactory.persistUser();
    ratingTestFactory.persistRating(stranger, alcohol, 4);
    Collections.reverse(expectedIds);
    var first =
        followFacade.getTastingFriendsInfoList(
            alcohol.getId(), owner.getId(), PageRequest.of(0, 6, Sort.by("id").descending()));
    var second =
        followFacade.getTastingFriendsInfoList(
            alcohol.getId(), owner.getId(), PageRequest.of(1, 6, Sort.by("id").descending()));
    assertThat(first)
        .extracting(FriendItem::userId)
        .containsExactlyElementsOf(expectedIds.subList(0, 6));
    assertThat(second)
        .extracting(FriendItem::userId)
        .containsExactlyElementsOf(expectedIds.subList(6, 8));
    assertThat(first).extracting(FriendItem::rating).containsOnly(4.0);
    assertThat(
            followFacade.getTastingFriendsInfoList(
                alcohol.getId(), owner.getId(), PageRequest.of(0, 6)))
        .hasSize(6);
  }
}

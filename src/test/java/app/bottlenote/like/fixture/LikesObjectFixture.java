package app.bottlenote.like.fixture;

import static app.bottlenote.review.constant.ReviewDisplayStatus.PUBLIC;

import app.bottlenote.review.domain.Review;
import app.bottlenote.user.domain.User;
import org.apache.commons.lang3.RandomStringUtils;

public class LikesObjectFixture {

  public static User createFixtureUser() {
    return User.builder()
        .email("Regular@google.com")
        .nickName(RandomStringUtils.randomAlphabetic(10))
        .age(30)
        .imageUrl(
            "https://mail.google.com/mail/u/0?ui=2&ik=c745dfa167&attid=0.1&permmsgid=msg-f:1804880468563143941&th=190c393936bf2d05&view=fimg&fur=ip&sz=s0-l75-ft&attbid=ANGjdJ-lGqPpqdkjNDJ5Ld0scIHREzEBd5a6ceSmKf9Px_wkdhxThpgeoxPQXLtHCjQ_4rn-G_FWM4uhnvbuPKYLTtEBN2uByVRVoVQd1i-dpEzLLf74BIPeoBDz5Cg&disp=emb&realattid=ii_lyp7nfzv0")
        .build();
  }

  public static Review createFixtureReview(Long userId, Long alcoholId) {
    return Review.builder()
        .userId(userId)
        .alcoholId(alcoholId)
        .status(PUBLIC)
        .content(RandomStringUtils.randomAlphabetic(80))
        .build();
  }
}

package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.domain.RatingRepository;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.domain.constant.FollowStatus;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.repository.FollowRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AlcoholTestFactory {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private FollowRepository followRepository;
	@Autowired
	private RatingRepository ratingRepository;

	public User createUser(Long id, String email, String nickName) {
		User user = User.builder()
			.id(id)
			.email(email)
			.age(20)
			.gender(GenderType.MALE.name())
			.nickName(nickName)
			.socialType(List.of(SocialType.KAKAO))
			.role(UserType.ROLE_USER)
			.build();
		return userRepository.save(user);
	}

	public void createRating(User user, Alcohol alcohol, int point) {
		Rating rating = Rating.builder()
			.id(RatingId.is(user.getId(), alcohol.getId()))
			.alcohol(alcohol)
			.user(user)
			.ratingPoint(RatingPoint.of(point))
			.build();
		ratingRepository.save(rating);
	}

	public void createFollow(User user, User followUser) {
		Follow follow = Follow.builder()
			.status(FollowStatus.FOLLOWING)
			.userId(user.getId())
			.followUser(followUser)
			.build();
		followRepository.save(follow);
	}


}

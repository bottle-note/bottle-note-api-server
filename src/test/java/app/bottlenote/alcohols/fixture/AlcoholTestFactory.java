package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.constant.AlcoholType;
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
	private AlcoholQueryRepository alcoholQueryRepository;

	public Alcohol createAlcohol() {
		Alcohol alcohol = Alcohol.builder()
			.id(1L)
			.korName("명작 위스키")
			.engName("Masterpiece Whisky")
			.abv("40%")
			.type(AlcoholType.WHISKY) // 타입 Enum 값 설정
			.korCategory("위스키")
			.engCategory("Whiskey")
			.categoryGroup(AlcoholCategoryGroup.SINGLE_MALT) // 그룹 Enum 값 설정
			.cask("American Oak")
			.imageUrl("https://example.com/image.jpg")
			.build();
		return alcoholQueryRepository.save(alcohol);
	}

	public User createUser(Long id, String email, String nickName) {
		User user = User.builder()
			.id(id)
			.email(email)
			.age(20)
			.gender(GenderType.MALE)
			.nickName(nickName)
			.socialType(List.of(SocialType.KAKAO))
			.role(UserType.ROLE_USER)
			.build();
		return userRepository.save(user);
	}

	public void createFollow(User user, User followUser) {
		Follow follow = Follow.builder()
			.status(FollowStatus.FOLLOWING)
			.userId(user.getId())
			.targetUserId(followUser.getId())
			.build();
		followRepository.save(follow);
	}
}

package app.bottlenote.user.domain;

import app.bottlenote.global.service.converter.JsonArrayConverter;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserStatus;
import app.bottlenote.user.domain.constant.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@ToString(of = {"id", "email", "nickName", "age", "socialType"})
@Comment("사용자 정보 테이블")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "users")
public class User {

	@Id
	@Comment("사용자 id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("사용자 이메일")
	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Comment("사용자 닉네임")
	@Column(name = "nick_name", nullable = false, unique = true)
	private String nickName;

	@Comment("사용자 연령")
	@Column(name = "age")
	private Integer age;

	@Comment("사용자 성별")
	@Column(name = "gender")
	private String gender;

	@Comment("사용자 프로필 썸네일")
	@Column(name = "image_url")
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Comment("사용자 권한 (ROLE_USER, ROLE_ADMIN)")
	@Column(name = "role", nullable = false)
	private UserType role;

	@Enumerated(EnumType.STRING)
	@Comment("사용자 상태(ACTIVE, DELETED)")
	@Column(name = "status", nullable = false)
	private UserStatus status = UserStatus.ACTIVE;

	@Convert(converter = JsonArrayConverter.class)
	@Comment("사용자 로그인 소셜타입 (GOOGLE, KAKAO, NAVER, APPLE")
	@Column(name = "social_type", nullable = false, columnDefinition = "json")
	private List<SocialType> socialType = new ArrayList<>();

	@Comment("사용자 리프레시토큰")
	@Column(name = "refresh_token", nullable = true)
	private String refreshToken;

	@Builder
	public User(Long id, String email, String nickName, Integer age, String gender, String imageUrl,
				UserType role, List<SocialType> socialType, String refreshToken) {
		this.id = id;
		this.email = email;
		this.nickName = nickName;
		this.age = age;
		this.gender = gender;
		this.imageUrl = imageUrl;
		this.role = role;
		this.socialType = socialType;
		this.refreshToken = refreshToken;
	}

	public void updateRefreshToken(String refreshToken) {
		Objects.requireNonNull(refreshToken, "refreshToken은 null이 될 수 없습니다.");
		this.refreshToken = refreshToken;
	}

	public void changeNickName(String nickName) {
		Objects.requireNonNull(nickName, "nickName은 null이 될 수 없습니다.");
		this.nickName = nickName;
	}

	public void withdrawUser() {
		this.status = UserStatus.DELETED;
	}

	public void changeProfileImage(String viewUrl) {
		this.imageUrl = viewUrl;
	}

	public void addSocialType(SocialType socialType) {
		if (!getSocialType().contains(socialType)) {
			this.socialType.add(socialType);
		}
	}
}

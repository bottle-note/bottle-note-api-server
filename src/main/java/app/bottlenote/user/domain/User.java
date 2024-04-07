package app.bottlenote.user.domain;

import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.domain.constant.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("사용자 정보 테이블")
@Entity(name = "users")
public class User {

	@Id
	@Comment("사용자 id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Comment("사용자 이메일")
	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Comment("사용자 닉네임")
	@Column(name = "nick_name", nullable = false, unique = true)
	private String nickName;

	@Comment("사용자 연령")
	@Column(name = "age", nullable = true)
	private Integer age;

	@Comment("사용자 성별")
	@Column(name = "gender", nullable = true)
	private String gender;

	@Comment("사용자 프로필 썸네일")
	@Column(name = "image_url", nullable = true)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Comment("사용자 권한 (ROLE_USER, ROLE_ADMIN)")
	@Column(name = "role", nullable = false)
	private UserType role;

	@Enumerated(EnumType.STRING)
	@Comment("사용자 로그인 소셜타입 (GOOGLE, KAKAO, NAVER, APPLE")
	@Column(name = "social_type", nullable = false)
	private SocialType socialType;

	@Comment("사용자 리프레시토큰")
	@Column(name = "refresh_token", nullable = true)
	private String refreshToken;

	@Builder
	public User(Long id , String email, String nickName, int age,String gender ,String imageUrl,
				 UserType role, SocialType socialType, String refreshToken) {
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
		if (refreshToken == null) {
			throw new IllegalArgumentException("refreshToken이 null입니다.");
		}
		this.refreshToken = refreshToken;
	}

}

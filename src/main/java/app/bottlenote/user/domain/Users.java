package app.bottlenote.user.domain;

import app.bottlenote.oauth.constant.SocialType;
import app.bottlenote.oauth.constant.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	@Comment("사용자 아이디")
	private String email;

	private String nickName;
	private String age;
	private String imageUrl;
	private UserType role;

	@Enumerated(EnumType.STRING)
	private SocialType socialType;
	private String refreshToken;

	@Builder
	public Users(String email, String nickName, String age, String imageUrl,
				 UserType role, SocialType socialType, String refreshToken) {
		this.email = email;
		this.nickName = nickName;
		this.age = age;
		this.imageUrl = imageUrl;
		this.role = role;
		this.socialType = socialType;
		this.refreshToken = refreshToken;
	}
}

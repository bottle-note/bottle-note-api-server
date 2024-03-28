package app.bottlenote.user.domain;

import app.bottlenote.oauth.constant.SocialType;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

@Entity(name = "users")
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
	private String role;

	@Enumerated(EnumType.STRING)
	private SocialType socialType;
	private String refreshToken;
}

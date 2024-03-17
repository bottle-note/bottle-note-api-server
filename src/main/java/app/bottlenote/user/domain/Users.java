package app.bottlenote.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
	private String socialType;
	private String refreshToken;
}

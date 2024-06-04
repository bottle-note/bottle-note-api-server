package app.bottlenote.history.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.history.domain.constant.UserHistoryAction;
import app.bottlenote.history.domain.constant.UserHistoryType;
import app.bottlenote.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity(name = "user_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohol_id")
	private Alcohol alcohol;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private UserHistoryType userHistoryType;

	@Enumerated(EnumType.STRING)
	@Column(name = "action")
	private UserHistoryAction userHistoryAction;

}

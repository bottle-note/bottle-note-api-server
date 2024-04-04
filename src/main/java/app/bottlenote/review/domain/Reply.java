package app.bottlenote.review.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Reply extends BaseEntity {

	//CREATE TABLE `reply` (
	//	`reply_id`	VARCHAR(255)	NOT NULL,
	//	`review_id`	VARCHAR(255)	NOT NULL,
	//	`user_id`	VARCHAR(255)	NOT NULL,
	//	`parent_reply_id`	VARCHAR(255)	NULL,
	//	`Field2`	VARCHAR(255)	NULL,
	//	`last_modify_at`	VARCHAR(255)	NOT NULL,
	//	`last_modify_by`	VARCHAR(255)	NOT NULL,
	//	`create_at`	VARCHAR(255)	NOT NULL,
	//	`create_by`	VARCHAR(255)	NOT NULL
	//);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_reply_id")
	private Reply superReply;

	@OneToMany(mappedBy = "parentReply", fetch = FetchType.LAZY)
	private List<Reply> replies = new ArrayList<>();

}

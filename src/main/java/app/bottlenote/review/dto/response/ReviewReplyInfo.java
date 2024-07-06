package app.bottlenote.review.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewReplyInfo {

	private Long userId;
	private String imageUrl;
	private String nickName;
	private Long reviewReplyId;
	private String reviewReplyContent;
	private LocalDateTime createAt;

	@Builder
	public ReviewReplyInfo(Long userId, String imageUrl, String nickName, Long reviewReplyId, String reviewReplyContent, LocalDateTime createAt) {
		this.userId = userId;
		this.imageUrl = imageUrl;
		this.nickName = nickName;
		this.reviewReplyId = reviewReplyId;
		this.reviewReplyContent = reviewReplyContent;
		this.createAt = createAt;
	}
}


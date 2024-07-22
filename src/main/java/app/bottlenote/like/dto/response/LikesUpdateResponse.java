package app.bottlenote.like.dto.response;

import app.bottlenote.like.domain.LikeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record LikesUpdateResponse(
	String message,
	Long likesId,
	Long reviewId,
	Long userId,
	String userNickName,
	LikeStatus status
) {
	public static LikesUpdateResponse of(
		Long likesId,
		Long reviewId,
		Long userId,
		String userNickName,
		LikeStatus status
	) {
		return new LikesUpdateResponse(
			status == LikeStatus.LIKE ? Message.LIKED.message : Message.DISLIKE.message,
			likesId,
			reviewId,
			userId,
			userNickName,
			status
		);
	}

	@Getter
	@AllArgsConstructor
	public enum Message {
		LIKED("정상적으로 좋아요 처리 되었습니다."),
		DISLIKE("정상적으로 좋아요 취소 처리 되었습니다.");

		private final String message;
	}
}

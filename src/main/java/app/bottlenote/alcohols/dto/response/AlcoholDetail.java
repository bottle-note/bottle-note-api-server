package app.bottlenote.alcohols.dto.response;

import lombok.Getter;

import java.util.List;

public class AlcoholDetail {


	private List<ReviewOfAlcoholDetail> bestReviews;
	private List<ReviewOfAlcoholDetail> reviews;

	@Getter
	public static class ReviewOfAlcoholDetail {
		private String user_id;
		private String image_url;
		private String nick_name;
		private String review_id;
		private String review_content;
		private String rating;
		private String size_type;
		private String price;
		private String view_count;
		private String like_count; // 좋아요 수;
		private String is_my_like; // 내가 좋아요 눌렀는지 여부;
		private String reply_count; // 댓글 수;
		private String is_my_reply; // 내가 댓글 달았는지 여부;
		private String status;
		private String review_image_url;
		private String create_at;
	}
}

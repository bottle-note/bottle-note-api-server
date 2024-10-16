package app.bottlenote.history.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {

	REVIEW_CREATE(EventCategory.REVIEW, "리뷰 등록"),
	REVIEW_LIKES(EventCategory.REVIEW, "리뷰 좋아요"),
	REVIEW_REPLY_CREATE(EventCategory.REVIEW, "리뷰 댓글 작성"),
	BEST_REVIEW_SELECTED(EventCategory.REVIEW, "베스트 리뷰 선정"),

	IS_PICK(EventCategory.PICK, "찜하기"),
	UNPICK(EventCategory.PICK, "찜하기 해제"),

	START_RATING(EventCategory.RATING, "첫 번째 별점"),
	RATING_MODIFY(EventCategory.RATING, "별점 수정"),
	RATING_DELETE(EventCategory.RATING, "별점 삭제");

	private final EventCategory eventCategory;
	private final String description;


//- 타입
//    - 리뷰 :
//		  - 리뷰 작성
//        - 리뷰 좋아요
//        - 리뷰 댓글 작성시
//        - 베스트 리뷰 선정시
//    - 찜 :
//		  - 찜하기 : isPick
//        - 찜해제 : unPick
//    - 별점 :
//		  - 첫 별점
//        - 별점 수정
//        - 별점 삭제

}

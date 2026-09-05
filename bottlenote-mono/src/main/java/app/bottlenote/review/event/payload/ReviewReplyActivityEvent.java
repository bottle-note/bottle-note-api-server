package app.bottlenote.review.event.payload;

public record ReviewReplyActivityEvent(
    Long reviewId,
    Long alcoholId,
    Long reviewAuthorId,
    Long replyUserId,
    Long replyId,
    Long parentReplyUserId,
    String content) {}

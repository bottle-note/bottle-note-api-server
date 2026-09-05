package app.bottlenote.like.event.payload;

public record ReviewLikeActivityEvent(
    Long likeId,
    Long reviewId,
    Long alcoholId,
    Long reviewAuthorId,
    Long actorId,
    String content,
    boolean activated) {}

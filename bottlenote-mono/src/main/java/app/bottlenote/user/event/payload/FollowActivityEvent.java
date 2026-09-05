package app.bottlenote.user.event.payload;

public record FollowActivityEvent(Long followId, Long actorId, Long targetUserId) {}

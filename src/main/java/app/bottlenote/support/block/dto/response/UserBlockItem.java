package app.bottlenote.support.block.dto.response;

import java.time.LocalDateTime;

public record UserBlockItem(Long userId, String userName, LocalDateTime blockedAt) {}

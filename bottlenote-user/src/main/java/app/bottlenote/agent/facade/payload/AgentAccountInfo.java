package app.bottlenote.agent.facade.payload;

public record AgentAccountInfo(
    String agentId, Long productUserId, Long adminUserId, String profileCode) {}

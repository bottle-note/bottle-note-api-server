package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AgentLoginRequest(@NotBlank(message = "AGENT_KEY_REQUIRED") String agentKey) {}

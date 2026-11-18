package app.bottlenote.agent.domain;

import app.bottlenote.agent.constant.AgentStatus;
import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface AgentRepository {

  Optional<Agent> findByApiKeyHashAndStatus(String apiKeyHash, AgentStatus status);

  Optional<Agent> findByAdminUserIdAndStatus(Long adminUserId, AgentStatus status);

  Agent save(Agent agent);
}

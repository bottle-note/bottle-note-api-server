package app.bottlenote.agent.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface AgentRepository {

  Optional<Agent> findBySecretHashAndIsActiveTrue(String secretHash);

  Agent save(Agent agent);
}

package app.bottlenote.agent.repository;

import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaAgentRepository extends AgentRepository, JpaRepository<Agent, String> {

  Optional<Agent> findBySecretHashAndIsActiveTrue(String secretHash);
}

package app.bottlenote.agent.fixture;

import app.bottlenote.agent.constant.AgentStatus;
import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryAgentRepository implements AgentRepository {

  private final Map<String, Agent> agentDatabase = new HashMap<>();

  @Override
  public Optional<Agent> findByApiKeyHashAndStatus(String apiKeyHash, AgentStatus status) {
    return agentDatabase.values().stream()
        .filter(agent -> agent.getApiKeyHash().equals(apiKeyHash))
        .filter(agent -> agent.getStatus() == status)
        .findFirst();
  }

  @Override
  public Optional<Agent> findByAdminUserIdAndStatus(Long adminUserId, AgentStatus status) {
    return agentDatabase.values().stream()
        .filter(agent -> agent.getAdminUserId().equals(adminUserId))
        .filter(agent -> agent.getStatus() == status)
        .findFirst();
  }

  @Override
  public Agent save(Agent agent) {
    agentDatabase.put(agent.getId(), agent);
    return agent;
  }

  public void clear() {
    agentDatabase.clear();
  }
}

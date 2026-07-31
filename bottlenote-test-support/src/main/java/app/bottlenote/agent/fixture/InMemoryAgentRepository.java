package app.bottlenote.agent.fixture;

import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryAgentRepository implements AgentRepository {

  private final Map<String, Agent> agentDatabase = new HashMap<>();

  @Override
  public Optional<Agent> findBySecretHashAndIsActiveTrue(String secretHash) {
    return agentDatabase.values().stream()
        .filter(agent -> agent.getSecretHash().equals(secretHash))
        .filter(Agent::isUsable)
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

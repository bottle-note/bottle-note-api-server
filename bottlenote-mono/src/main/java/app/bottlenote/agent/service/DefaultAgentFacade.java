package app.bottlenote.agent.service;

import static app.bottlenote.agent.constant.AgentStatus.ACTIVE;

import app.bottlenote.agent.domain.Agent;
import app.bottlenote.agent.domain.AgentRepository;
import app.bottlenote.agent.facade.AgentFacade;
import app.bottlenote.agent.facade.payload.AgentAccountInfo;
import app.bottlenote.agent.support.AgentKeyHasher;
import app.bottlenote.common.annotation.FacadeService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@FacadeService
@RequiredArgsConstructor
public class DefaultAgentFacade implements AgentFacade {

  private final AgentRepository agentRepository;

  @Override
  public Optional<AgentAccountInfo> findActiveAgentAccount(String rawAgentKey) {
    String apiKeyHash = AgentKeyHasher.validateAndHash(rawAgentKey);
    return agentRepository
        .findByApiKeyHashAndStatus(apiKeyHash, ACTIVE)
        .filter(Agent::isUsable)
        .map(
            agent ->
                new AgentAccountInfo(
                    agent.getProductUserId(), agent.getAdminUserId(), agent.getProfileCode()));
  }
}

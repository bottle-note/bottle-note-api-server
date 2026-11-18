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
        .map(this::toAccountInfo);
  }

  @Override
  public Optional<AgentAccountInfo> findActiveAgentByAdminUserId(Long adminUserId) {
    if (adminUserId == null) {
      return Optional.empty();
    }
    return agentRepository
        .findByAdminUserIdAndStatus(adminUserId, ACTIVE)
        .filter(Agent::isUsable)
        .map(this::toAccountInfo);
  }

  private AgentAccountInfo toAccountInfo(Agent agent) {
    return new AgentAccountInfo(
        agent.getId(), agent.getProductUserId(), agent.getAdminUserId(), agent.getProfileCode());
  }
}

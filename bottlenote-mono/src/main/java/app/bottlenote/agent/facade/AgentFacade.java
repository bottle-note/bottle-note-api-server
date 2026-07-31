package app.bottlenote.agent.facade;

import app.bottlenote.agent.facade.payload.AgentAccountInfo;
import java.util.Optional;

public interface AgentFacade {

  /**
   * 원문 에이전트 키를 정규화·해시하여 활성 에이전트를 조회한다.
   *
   * @throws IllegalArgumentException rawAgentKey가 UUID 형식이 아닌 경우
   */
  Optional<AgentAccountInfo> findActiveAgentAccount(String rawAgentKey);
}

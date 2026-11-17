package app.bottlenote.agent.facade;

import app.bottlenote.agent.facade.payload.AgentAccountInfo;
import java.util.Optional;

public interface AgentFacade {

  /**
   * 원문 에이전트 API Key의 형식을 검증·해시하여 활성 에이전트를 조회한다.
   *
   * @throws IllegalArgumentException rawAgentKey가 에이전트 API Key 형식이 아닌 경우
   */
  Optional<AgentAccountInfo> findActiveAgentAccount(String rawAgentKey);

  /** 인증된 Admin 사용자 ID로 매핑된 활성 에이전트를 조회한다. */
  Optional<AgentAccountInfo> findActiveAgentByAdminUserId(Long adminUserId);
}

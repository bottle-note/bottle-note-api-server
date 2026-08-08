package app.bottlenote.accesscontrol.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.dto.response.IpBanCommandResult;
import app.bottlenote.accesscontrol.dto.response.ProjectionStatus;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanEventRepository;
import app.bottlenote.accesscontrol.fixture.InMemoryIpBanRepository;
import app.bottlenote.agent.fixture.InMemoryAgentRepository;
import app.bottlenote.agent.service.DefaultAgentFacade;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DefaultIpBanFacade 단위 테스트")
class DefaultIpBanFacadeTest {

  @Test
  @DisplayName("DB 상태와 이벤트를 저장한 뒤 Redis projection을 반영한다")
  void ban_whenProjectionSucceeds_returnsAppliedWithLatestEvent() {
    InMemoryIpBanEventRepository eventRepository = new InMemoryIpBanEventRepository();
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    DefaultIpBanFacade facade = facade(eventRepository, store);

    IpBanCommandResult result = facade.ban("203.0.113.80", Duration.ofMinutes(5), "abuse", 1L);

    assertThat(result.projectionStatus()).isEqualTo(ProjectionStatus.APPLIED);
    assertThat(result.detail().events()).hasSize(1);
    assertThat(result.detail().events().getLast().id()).isEqualTo(1L);
    assertThat(result.detail().events().getLast().eventType()).isEqualTo(IpBanEventType.BAN);
    assertThat(store.isBanned("203.0.113.80")).isTrue();
  }

  @Test
  @DisplayName("Redis projection이 실패해도 DB 상태와 감사 이벤트를 보존하고 재조정을 대기한다")
  void ban_whenProjectionFails_preservesDatabaseResultAndReturnsPending() {
    InMemoryIpBanEventRepository eventRepository = new InMemoryIpBanEventRepository();
    DefaultIpBanFacade facade = facade(eventRepository, new FailingProjectionStore());

    IpBanCommandResult result = facade.ban("203.0.113.81", Duration.ofMinutes(5), "abuse", 1L);

    assertThat(result.projectionStatus()).isEqualTo(ProjectionStatus.PENDING_RECONCILE);
    assertThat(result.detail().events()).hasSize(1);
    assertThat(result.detail().events().getLast().id()).isEqualTo(1L);
    assertThat(eventRepository.findAll()).hasSize(1);
    assertThat(eventRepository.findAll().getFirst().getEventType()).isEqualTo(IpBanEventType.BAN);
  }

  private static DefaultIpBanFacade facade(
      InMemoryIpBanEventRepository eventRepository, AccessControlStore store) {
    IpBanService service =
        new IpBanService(
            new InMemoryIpBanRepository(),
            eventRepository,
            new DefaultAgentFacade(new InMemoryAgentRepository()),
            Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC));
    return new DefaultIpBanFacade(service, store);
  }

  private static final class FailingProjectionStore implements AccessControlStore {

    @Override
    public boolean isBanned(String ip) {
      return false;
    }

    @Override
    public void ban(String ip, Duration ttl, String reason) {}

    @Override
    public void unban(String ip) {}

    @Override
    public void projectBan(String ip, Duration ttl, String reason, long eventId) {
      throw new IllegalStateException("redis down");
    }

    @Override
    public BanInfo getBan(String ip) {
      return null;
    }

    @Override
    public List<BanInfo> listBans(int max) {
      return List.of();
    }

    @Override
    public ConsumeResult tryConsume(String key, int limit, Duration window) {
      return ConsumeResult.allow(limit);
    }
  }
}

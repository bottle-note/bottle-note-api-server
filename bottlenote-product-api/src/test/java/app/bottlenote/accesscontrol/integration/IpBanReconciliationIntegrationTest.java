package app.bottlenote.accesscontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.accesscontrol.constant.IpBanStatus;
import app.bottlenote.accesscontrol.dto.response.IpBanDetailResponse;
import app.bottlenote.accesscontrol.facade.IpBanFacade;
import app.bottlenote.accesscontrol.service.IpBanReconciliationService;
import app.bottlenote.accesscontrol.service.IpBanService;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@DisplayName("[integration] IP 차단 DB Redis 재조정")
@ActiveProfiles({"test", "access-control-it"})
class IpBanReconciliationIntegrationTest extends IntegrationTestSupport {

  @Autowired private IpBanFacade ipBanFacade;
  @Autowired private IpBanService ipBanService;
  @Autowired private IpBanReconciliationService reconciliationService;
  @Autowired private AccessControlStore accessControlStore;
  @Autowired private StringRedisTemplate stringRedisTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanAccessControlRedisState() {
    Set<String> keys = stringRedisTemplate.keys("bn:ac:*");
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
    }
  }

  @Test
  @DisplayName("Redis 유실 뒤 재조정은 DB ACTIVE 차단을 실제 Redis에 복원한다")
  void reconcile_whenRedisIsLost_restoresActiveDatabaseBan() {
    String ip = nextTestIp();
    ipBanFacade.ban(ip, Duration.ofMinutes(10), "redis-loss", null);

    clearRedis();
    assertThat(accessControlStore.isBanned(ip)).isFalse();

    reconciliationService.reconcile();

    assertThat(accessControlStore.isBanned(ip)).isTrue();
    assertThat(accessControlStore.getBan(ip).reason()).isEqualTo("redis-loss");
  }

  @Test
  @DisplayName("DB에서 해제된 차단은 재조정이 Redis projection을 제거한다")
  void reconcile_whenDatabaseBanIsUnbanned_removesRedisProjection() {
    String ip = nextTestIp();
    ipBanFacade.ban(ip, Duration.ofMinutes(10), "manual-unban", null);
    ipBanService.unban(ip, "reviewed", null);

    assertThat(accessControlStore.isBanned(ip)).isTrue();

    reconciliationService.reconcile();

    IpBanDetailResponse detail = ipBanFacade.findByIp(ip).orElseThrow();
    assertThat(detail.status()).isEqualTo(IpBanStatus.UNBANNED);
    assertThat(accessControlStore.isBanned(ip)).isFalse();
  }

  @Test
  @DisplayName("201번째 DB UNBANNED도 keyset 재조정으로 Redis에서 제거한다")
  void reconcile_whenInactiveBansExceedBatch_removesBeyondFirstDatabaseBatch() {
    String lastIp = null;
    for (int index = 1; index <= 201; index++) {
      String ip = "198.18." + (index / 250) + "." + (index % 250 + 1);
      ipBanFacade.ban(ip, Duration.ofMinutes(10), "stale-" + index, null);
      ipBanService.unban(ip, "reviewed-" + index, null);
      lastIp = ip;
    }

    assertThat(accessControlStore.isBanned(lastIp)).isTrue();

    reconciliationService.reconcile();

    assertThat(accessControlStore.isBanned(lastIp)).isFalse();
  }

  @Test
  @DisplayName("DB에서 만료된 활성 차단은 재조정이 EXPIRE 이력과 Redis 제거를 수행한다")
  void reconcile_whenDatabaseBanExpires_marksExpiredAndRemovesRedisProjection() {
    String ip = nextTestIp();
    IpBanDetailResponse ban = ipBanFacade.ban(ip, Duration.ofMinutes(10), "expired", null).detail();
    jdbcTemplate.update(
        """
        update ip_bans
        set effective_from = date_sub(now(6), interval 2 second),
            expires_at = date_sub(now(6), interval 1 second)
        where id = ?
        """,
        ban.id());

    reconciliationService.reconcile();

    IpBanDetailResponse detail = ipBanFacade.findByIp(ip).orElseThrow();
    assertThat(detail.status()).isEqualTo(IpBanStatus.EXPIRED);
    assertThat(detail.events())
        .extracting(event -> event.eventType())
        .contains(IpBanEventType.EXPIRE);
    assertThat(accessControlStore.isBanned(ip)).isFalse();
  }

  private void clearRedis() {
    Set<String> keys = stringRedisTemplate.keys("bn:ac:*");
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
    }
  }

  private static String nextTestIp() {
    int host = Math.floorMod(UUID.randomUUID().hashCode(), 250) + 1;
    return "198.18.0." + host;
  }
}

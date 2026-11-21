package app.global.admin;

import app.bottlenote.IntegrationTestSupport;
import org.springframework.test.context.TestPropertySource;

/**
 * admin API 흡수 검증의 공통 기반.
 *
 * <p>IP 접근 제어 관리 API(AdminAccessControlController)는 bottlenote.access-control.enabled=true일 때만
 * 등록된다. 운영 설정은 켜져 있고 테스트 공통 설정은 꺼져 있으므로, 운영과 같은 156개 라우트를 검사하려고 여기서 켠다. 한 테스트가 같은 IP로 수백 번 호출하므로
 * 한도는 검사에 영향을 주지 않게 크게 둔다.
 *
 * <p>admin 체인은 기존 admin 앱 설정(bottlenote.admin-access-control)으로 만든 별도 필터를 쓴다. 테스트 classpath는 운영
 * application.yml을 읽지 않으므로 같은 구조를 여기에 둔다. 한도는 product와 admin, 기본·경로 규칙·unknown-IP를 응답 헤더로 구분할 수 있게
 * 서로 다른 큰 값으로 둔다. 운영 값 자체는 AdminAccessControlFilterTest가 운영 yml로 검증한다.
 *
 * <p>테스트 공통 설정은 운영 datasource 설정을 읽지 않아 open-in-view가 기본값(true)이다. admin 경로만 OSIV를 켜는 운영 동작을 그대로
 * 검사하려고 운영 값(false)을 넣는다.
 */
@TestPropertySource(
    properties = {
      "bottlenote.access-control.enabled=true",
      "bottlenote.access-control.key-namespace=admin-absorb-it",
      "bottlenote.access-control.default-rate-limit.limit=1000000",
      "bottlenote.access-control.default-rate-limit.window-seconds=60",
      "bottlenote.access-control.unknown-ip-rate-limit.limit=1000000",
      "bottlenote.access-control.unknown-ip-rate-limit.window-seconds=60",
      "bottlenote.admin-access-control.key-namespace=admin-absorb-it-admin",
      "bottlenote.admin-access-control.default-rate-limit.limit=900000",
      "bottlenote.admin-access-control.default-rate-limit.window-seconds=60",
      "bottlenote.admin-access-control.unknown-ip-rate-limit.limit=800000",
      "bottlenote.admin-access-control.unknown-ip-rate-limit.window-seconds=60",
      "bottlenote.admin-access-control.path-rules[0].path-prefix=/v1/auth",
      "bottlenote.admin-access-control.path-rules[0].limit=700000",
      "bottlenote.admin-access-control.path-rules[0].window-seconds=60",
      "bottlenote.admin-access-control.management-path-prefixes[0]=/v1/access-control",
      "bottlenote.admin-access-control.excluded-path-prefixes[0]=/v1/openapi.admin.json",
      "spring.jpa.open-in-view=false"
    })
public abstract class AdminApiIntegrationTestSupport extends IntegrationTestSupport {}

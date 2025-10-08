package app.external.version.presentation;

import app.bottlenote.global.data.response.GlobalResponse;
import app.external.version.config.AppInfoConfig;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/app-info")
@RequiredArgsConstructor
public class AppInfoController {
  private final AppInfoConfig info;

  @GetMapping
  public ResponseEntity<?> getAppInfo() {
    Map<String, Object> infoMap = new HashMap<>();
    infoMap.put("serverName", info.getServerName());
    infoMap.put("environment", info.getEnvironment());
    infoMap.put("gitBranch", info.getGitBranch());
    infoMap.put("gitCommitHash", info.getGitCommit().substring(0, 7));
    infoMap.put("gitCommitFullHash", info.getGitCommit());

    String buildTime = info.getGitBuildTime();
    try {
      buildTime =
          ZonedDateTime.parse(buildTime)
              .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
              .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    } catch (Exception ignored) {
      log.warn("deploy time parse error but not critical");
    }
    infoMap.put("gitBuildTime", buildTime);

    return GlobalResponse.ok(infoMap);
  }
}

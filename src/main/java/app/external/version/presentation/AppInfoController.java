package app.external.version.presentation;

import app.bottlenote.global.data.response.GlobalResponse;
import app.external.version.config.AppInfoConfig;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    infoMap.put("gitCommit", info.getGitCommit());
    infoMap.put("gitBuildTime", info.getGitBuildTime());
    return GlobalResponse.ok(infoMap);
  }
}

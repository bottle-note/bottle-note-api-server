package app.bottlenote.statistics.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bottlenote.statistics")
public class StatisticsProperties {

  private Exclusion exclusion = new Exclusion();

  public Exclusion getExclusion() {
    return exclusion;
  }

  public void setExclusion(Exclusion exclusion) {
    this.exclusion = exclusion == null ? new Exclusion() : exclusion;
  }

  public static class Exclusion {
    private List<String> deviceTypes = new ArrayList<>();
    private List<String> ipPrefixes = new ArrayList<>();

    public List<String> getDeviceTypes() {
      return deviceTypes;
    }

    public void setDeviceTypes(List<String> deviceTypes) {
      this.deviceTypes = deviceTypes == null ? new ArrayList<>() : deviceTypes;
    }

    public List<String> getIpPrefixes() {
      return ipPrefixes;
    }

    public void setIpPrefixes(List<String> ipPrefixes) {
      this.ipPrefixes = ipPrefixes == null ? new ArrayList<>() : ipPrefixes;
    }
  }
}

package app.bottlenote.global.security.accesscontrol;

import com.google.common.net.InetAddresses;
import jakarta.servlet.http.HttpServletRequest;

/** Gateway가 재기록한 X-Forwarded-For 첫 유효 IP를 사용한다. */
public final class ClientIpResolver {

  private ClientIpResolver() {}

  public static String normalize(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    if (!InetAddresses.isInetAddress(trimmed)) {
      return null;
    }
    return InetAddresses.toAddrString(InetAddresses.forString(trimmed));
  }

  public static String resolve(HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null) {
      for (String candidate : forwardedFor.split(",")) {
        String trimmed = candidate.trim();
        if (InetAddresses.isInetAddress(trimmed)) {
          return InetAddresses.toAddrString(InetAddresses.forString(trimmed));
        }
      }
    }

    String remoteAddress = request.getRemoteAddr();
    if (remoteAddress != null && InetAddresses.isInetAddress(remoteAddress)) {
      return InetAddresses.toAddrString(InetAddresses.forString(remoteAddress));
    }
    return null;
  }
}

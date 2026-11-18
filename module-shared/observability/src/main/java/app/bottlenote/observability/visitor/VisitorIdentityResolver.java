package app.bottlenote.observability.visitor;

@FunctionalInterface
public interface VisitorIdentityResolver {
  String resolve(String clientIp, String userAgent);
}

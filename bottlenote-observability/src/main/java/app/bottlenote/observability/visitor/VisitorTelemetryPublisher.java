package app.bottlenote.observability.visitor;

public interface VisitorTelemetryPublisher {

  void publish(VisitorTelemetry telemetry);
}

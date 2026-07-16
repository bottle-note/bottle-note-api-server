package app.bottlenote.observability.visitor;

interface VisitorTelemetryPublisher {

  void publish(VisitorTelemetry telemetry);
}

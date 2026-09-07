package app.bottlenote.global.timeseries;

public record TimeSeriesDescriptor(
    String key, String label, TimeSeriesUnit unit, TimeSeriesFill fill) {}

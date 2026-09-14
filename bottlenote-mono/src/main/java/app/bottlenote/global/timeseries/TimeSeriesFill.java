package app.bottlenote.global.timeseries;

import app.bottlenote.global.annotation.ExcludeRule;

@ExcludeRule
public enum TimeSeriesFill {
  ZERO,
  NULL,
  PREVIOUS
}

package app.bottlenote.shared;

import java.util.TimeZone;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Const {
  public static final String APP_NAME = "Bottle_Note";
  public static final String APP_VERSION = "1.0.0";
  public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

  public static TimeZone KOR_TIME_ZONE = TimeZone.getTimeZone("Asia/Seoul");
}

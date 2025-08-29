package app.bottlenote.shared;

import lombok.NoArgsConstructor;

import java.util.TimeZone;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Const {
	public static final String APP_NAME = "Bottle_Note";
	public static final String APP_VERSION = "1.0.0";
	public static TimeZone TIMEZONE = TimeZone.getTimeZone("Asia/Seoul");
	public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}

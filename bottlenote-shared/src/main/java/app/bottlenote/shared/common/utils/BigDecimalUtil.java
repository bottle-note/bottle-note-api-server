package app.bottlenote.shared.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class BigDecimalUtil {

	private static final int DEFAULT_SCALE = 1;
	private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
	private static final NumberFormat KOREAN_CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.KOREA);
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.0");

	public static BigDecimal of(double value) {
		return BigDecimal.valueOf(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
	}

	public static BigDecimal of(String value) {
		return new BigDecimal(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
	}

	public static BigDecimal add(BigDecimal a, BigDecimal b) {
		return a.add(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
	}

	public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
		return a.subtract(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
	}

	public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
		return a.multiply(b).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
	}

	public static BigDecimal divide(BigDecimal a, BigDecimal b) {
		return a.divide(b, DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
	}

	public static String formatCurrency(BigDecimal value) {
		return KOREAN_CURRENCY_FORMAT.format(value);
	}

	public static String formatDecimal(BigDecimal value) {
		return DECIMAL_FORMAT.format(value);
	}

	public static BigDecimal nullSafe(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(DEFAULT_SCALE) : value;
	}

	public static boolean isZero(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) == 0;
	}

	public static boolean isPositive(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) > 0;
	}

	public static boolean isNegative(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) < 0;
	}
}

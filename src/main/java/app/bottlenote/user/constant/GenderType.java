package app.bottlenote.user.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Stream;

public enum GenderType {
	MALE,
	FEMALE;

	@JsonCreator
	public static GenderType parsing(String inputValue) {
		if (inputValue == null || inputValue.isEmpty()) {
			return null;
		}

		return Stream.of(GenderType.values())
			.filter(genderType -> genderType.name().equals(inputValue))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("성별입력은 남,여 만됩니다. " + GenderType.class.getCanonicalName() + "." + inputValue));
	}
}

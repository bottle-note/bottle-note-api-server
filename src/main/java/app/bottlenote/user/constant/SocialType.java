package app.bottlenote.user.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.stream.Stream;

public enum SocialType {
	KAKAO,
	NAVER,
	GOOGLE,
	APPLE,
	BASIC,
	NONE;

	@JsonCreator
	public static SocialType parsing(String inputValue) {
		if (inputValue == null || inputValue.isEmpty()) {
			throw new IllegalArgumentException("SocialType이 없습니다.");
		}

		return Stream.of(SocialType.values())
				.filter(socialType -> socialType.name().equals(inputValue.toUpperCase()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("소셜타입이 없습니다. : " + SocialType.class.getCanonicalName() + "." + inputValue));
	}
}

package app.bottlenote.user.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



/**
 * 닉네임 생성기
 *
 * 닉네임 생성기는 다음과 같은 방식으로 닉네임을 생성합니다.
 * 1. 현재 시간을 기반으로 닉네임을 생성합니다.
 * 2. 현재 시간의 연도의 마지막 두 자리를 더한 값을 구합니다. -> 유니크함은 유지하지하면서 닉네임 길이를 줄일 수 있음.
 * 3. 현재 시간의 월을 기반으로 알파벳을 매핑합니다. (1월: J, 2월: F, ... 11월: N, 12월: D) -> 유니크함은 유지하지하면서 닉네임 길이를 줄일 수 있음.
 * 4. 현재 시간의 일, 시, 분 값을 이어붙여 닉네임의 시간 부분을 생성
 * 5. 랜덤으로 4개의 알파벳을 생성합니다.
 * 		- 랜덤 알파벳이 3개일 때와 4개일 때 생성되는 가능한 조합의 수 차이
 * 		- 3개일 때: 26 * 26 * 26 = 17,576
 * 		- 4개일 때: 26 * 26 * 26 * 26 = 456,976
 * 7. 생성된 랜덤 부분과 시간 부분을 이어붙여 최종 닉네임을 생성합니다.
 *
*/
public class NicknameGenerator {

	private static final Map<Integer, String> MONTH_INITIALS = Map.ofEntries(
		Map.entry(1, "J"),
		Map.entry(2, "F"),
		Map.entry(3, "M"),
		Map.entry(4, "A"),
		Map.entry(5, "M"),
		Map.entry(6, "J"),
		Map.entry(7, "J"),
		Map.entry(8, "A"),
		Map.entry(9, "S"),
		Map.entry(10, "O"),
		Map.entry(11, "N"),
		Map.entry(12, "D")
	);
	private static final int RANDOM_LETTERS_COUNT = 4;
	private static NicknameGenerator instance;
	private final SecureRandom secureRandom;

	private NicknameGenerator() {
		secureRandom = new SecureRandom();
	}

	public static NicknameGenerator getInstance() {
		if (instance == null) {
			instance = new NicknameGenerator();
		}
		return instance;
	}

	public String generate() {
		return shuffle(generateRandomLetters(RANDOM_LETTERS_COUNT)) + createTimeString();
	}

	private String generateRandomLetters(int count) {
		return IntStream.range(0, count)
			.mapToObj(i -> {
				char base = secureRandom.nextBoolean() ? 'A' : 'a';
				return (char) (base + secureRandom.nextInt(26));
			})
			.map(String::valueOf)
			.collect(Collectors.joining());
	}

	private String shuffle(String string) {
		List<String> letters = Arrays.asList(string.split(""));
		Collections.shuffle(letters);
		return String.join("", letters);
	}

	private String createTimeString() {
		LocalDateTime now = LocalDateTime.now();
		int yearSum = (now.getYear() % 100) / 10 + now.getYear() % 10;
		String monthStr = MONTH_INITIALS.getOrDefault(now.getMonthValue(), "X");
		return yearSum + monthStr + now.getDayOfMonth() + now.getHour() + now.getMinute();
	}
}

package app.bottlenote.support.report.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum UserReportType {
	SPAM("스팸"),
	INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
	FRAUD("사기"),
	COPYRIGHT_INFRINGEMENT("저작권 침해"),
	OTHER("기타");

	private final String status;

	@JsonCreator
	public static UserReportType parsing(String inputValue) {
		if (inputValue == null || inputValue.isEmpty()) {
			return null;
		}
		return Stream.of(UserReportType.values())
			.filter(genderType -> genderType.toString().equals(inputValue.toUpperCase()))
			.findFirst()
			.orElse(null);
	}
}

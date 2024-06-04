package app.bottlenote.history.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserHistoryAction {

	CREATE("생성"),
	UPDATE("수정"),
	DELETE("삭제");

	private final String description;
}

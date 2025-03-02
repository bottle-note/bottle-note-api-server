package app.bottlenote.picks.dto.response;

import app.bottlenote.picks.domain.PicksStatus;

public record PicksUpdateResponse(String message, PicksStatus status) {

	public static PicksUpdateResponse of(PicksStatus status) {
		String message = status == PicksStatus.PICK ? Message.PICKED.message() : Message.UNPICKED.message();
		return new PicksUpdateResponse(message, status);
	}

	public enum Message {
		PICKED("정상적으로 찜하기 처리 되었습니다."),
		UNPICKED("정상적으로 찜하기 취소 처리 되었습니다.");

		private final String message;

		Message(String message) {
			this.message = message;
		}

		public String message() {
			return message;
		}
	}
}
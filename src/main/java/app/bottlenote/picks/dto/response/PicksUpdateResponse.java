package app.bottlenote.picks.dto.response;


import app.bottlenote.picks.domain.PicksStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class PicksUpdateResponse {

	private final String message;
	private final PicksStatus status;

	private PicksUpdateResponse(PicksStatus status) {
		this.message = status.equals(PicksStatus.PICK) ? Message.PICKED.message : Message.UNPICKED.message;
		this.status = status;
	}

	public static PicksUpdateResponse of(PicksStatus status) {
		return new PicksUpdateResponse(status);
	}

	@Getter
	@AllArgsConstructor
	public enum Message {
		PICKED("정상적으로 찜하기 처리 되었습니다."),
		UNPICKED("정상적으로 찜하기 취소 처리 되었습니다.");

		private final String message;
	}
}

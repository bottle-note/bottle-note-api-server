package app.bottlenote.support.business.constant;

import lombok.Getter;

@Getter
public enum BusinessSupportType {
	EVENT("이벤트 관련 문의"),
	ADVERTISEMENT("광고 관련 문의"),
	ETC("기타 문의");

	private final String description;

	BusinessSupportType(String description) {
		this.description = description;
	}
}

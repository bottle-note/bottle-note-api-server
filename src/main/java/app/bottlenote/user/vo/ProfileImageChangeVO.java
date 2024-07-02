package app.bottlenote.user.vo;

import lombok.Getter;

@Getter
public class ProfileImageChangeVO {

	private String imageUrl;

	public ProfileImageChangeVO(String imageUrl) {
		this.imageUrl = imageUrl;
	}


}

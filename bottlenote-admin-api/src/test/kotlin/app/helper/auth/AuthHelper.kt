package app.helper.auth

import app.bottlenote.user.dto.response.TokenItem
import org.apache.commons.lang3.RandomStringUtils

object AuthHelper {

	fun createTokenItem(): TokenItem {
		return TokenItem(
			RandomStringUtils.randomAlphanumeric(32),
			RandomStringUtils.randomAlphanumeric(32)
		)
	}
}
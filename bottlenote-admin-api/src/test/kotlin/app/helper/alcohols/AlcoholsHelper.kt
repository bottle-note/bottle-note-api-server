package app.helper.alcohols

import app.bottlenote.alcohols.dto.response.AdminAlcoholItem
import app.bottlenote.global.data.response.GlobalResponse
import java.time.LocalDateTime

object AlcoholsHelper {

	fun createAdminAlcoholItem(
		id: Long = 1L,
		korName: String = "테스트 위스키",
		engName: String = "Test Whisky",
		korCategoryName: String = "싱글몰트",
		engCategoryName: String = "Single Malt",
		imageUrl: String = "https://example.com/image.jpg",
		createdAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
		modifiedAt: LocalDateTime = LocalDateTime.of(2024, 6, 1, 0, 0)
	): AdminAlcoholItem = AdminAlcoholItem(
		id, korName, engName, korCategoryName, engCategoryName, imageUrl, createdAt, modifiedAt
	)

	fun createAdminAlcoholItems(count: Int = 2): List<AdminAlcoholItem> =
		(1..count).map { i ->
			createAdminAlcoholItem(
				id = i.toLong(),
				korName = "테스트 위스키 $i",
				engName = "Test Whisky $i",
				createdAt = LocalDateTime.of(2024, i, 1, 0, 0),
				modifiedAt = LocalDateTime.of(2024, i + 5, 1, 0, 0)
			)
		}

	fun createPageResponse(
		items: List<Any>,
		page: Int = 0,
		size: Int = 20,
		totalElements: Long = items.size.toLong()
	): GlobalResponse {
		val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
		val hasNext = page < totalPages - 1

		return GlobalResponse.builder()
			.success(true)
			.code(200)
			.data(items)
			.errors(emptyList<String>())
			.meta(
				mapOf(
					"page" to page,
					"size" to size,
					"totalElements" to totalElements,
					"totalPages" to totalPages,
					"hasNext" to hasNext,
					"serverVersion" to "1.0.0",
					"serverEncoding" to "UTF-8",
					"serverResponseTime" to LocalDateTime.now().toString(),
					"serverPathVersion" to "v1"
				)
			)
			.build()
	}
}

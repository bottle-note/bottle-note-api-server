package app.helper.alcohols

import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse.TastingTagInfo
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem
import app.bottlenote.alcohols.dto.response.AdminRegionItem
import app.bottlenote.alcohols.dto.response.AdminTastingTagItem
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import java.time.LocalDateTime

object AlcoholsHelper {

	/** 1x1 투명 PNG 이미지 (테스트용) */
	const val VALID_BASE64_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

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

	fun createAdminAlcoholDetailResponse(
		alcoholId: Long = 1L,
		korName: String = "글렌피딕 12년",
		engName: String = "Glenfiddich 12 Year"
	): AdminAlcoholDetailResponse = AdminAlcoholDetailResponse(
		alcoholId,
		korName,
		engName,
		"https://example.com/image.jpg",
		"WHISKY",
		"싱글몰트",
		"Single Malt",
		"SINGLE_MALT",
		"40%",
		"12",
		"오크",
		"700ml",
		"스코틀랜드의 대표적인 싱글몰트 위스키",
		1L,
		"스코틀랜드",
		"Scotland",
		1L,
		"글렌피딕",
		"Glenfiddich",
		listOf(
			TastingTagInfo(1L, "바닐라", "Vanilla"),
			TastingTagInfo(2L, "꿀", "Honey")
		),
		4.2,
		150L,
		45L,
		200L,
		LocalDateTime.of(2024, 1, 1, 0, 0),
		LocalDateTime.of(2024, 6, 1, 0, 0)
	)

	fun createAdminTastingTagItems(count: Int = 3): List<AdminTastingTagItem> =
		(1..count).map { i ->
			AdminTastingTagItem(
				i.toLong(),
				"태그$i",
				"Tag$i",
				"icon$i.png",
				"테이스팅 태그 설명 $i",
				null,
				LocalDateTime.of(2024, 1, i, 0, 0),
				LocalDateTime.of(2024, 6, i, 0, 0)
			)
		}

	fun createAdminRegionItems(count: Int = 3): List<AdminRegionItem> =
		(1..count).map { i ->
			AdminRegionItem(
				i.toLong(),
				listOf("스코틀랜드", "아일랜드", "일본")[i - 1],
				listOf("Scotland", "Ireland", "Japan")[i - 1],
				listOf("유럽", "유럽", "아시아")[i - 1],
				"지역 설명 $i",
				LocalDateTime.of(2024, 1, i, 0, 0),
				LocalDateTime.of(2024, 6, i, 0, 0)
			)
		}

	fun createAdminDistilleryItems(count: Int = 3): List<AdminDistilleryItem> =
		(1..count).map { i ->
			AdminDistilleryItem(
				i.toLong(),
				listOf("글렌피딕", "맥캘란", "야마자키")[i - 1],
				listOf("Glenfiddich", "Macallan", "Yamazaki")[i - 1],
				"https://example.com/logo$i.png",
				LocalDateTime.of(2024, 1, i, 0, 0),
				LocalDateTime.of(2024, 6, i, 0, 0)
			)
		}

	fun createListResponse(items: List<Any>): GlobalResponse =
		GlobalResponse.builder()
			.success(true)
			.code(200)
			.data(items)
			.errors(emptyList<String>())
			.meta(
				mapOf(
					"serverVersion" to "1.0.0",
					"serverEncoding" to "UTF-8",
					"serverResponseTime" to LocalDateTime.now().toString(),
					"serverPathVersion" to "v1"
				)
			)
			.build()

	fun createAdminResultResponse(
		code: AdminResultResponse.ResultCode = AdminResultResponse.ResultCode.ALCOHOL_CREATED,
		targetId: Long = 1L
	): AdminResultResponse = AdminResultResponse.of(code, targetId)

	fun createAlcoholUpsertRequestMap(
		korName: String = "테스트 위스키",
		engName: String = "Test Whisky",
		abv: String = "40%",
		type: AlcoholType = AlcoholType.WHISKY,
		korCategory: String = "싱글 몰트",
		engCategory: String = "Single Malt",
		categoryGroup: AlcoholCategoryGroup = AlcoholCategoryGroup.SINGLE_MALT,
		regionId: Long = 1L,
		distilleryId: Long = 1L,
		age: String = "12",
		cask: String = "American Oak",
		imageUrl: String = "https://example.com/test.jpg",
		description: String = "테스트 설명",
		volume: String = "700ml"
	): Map<String, Any> = mapOf(
		"korName" to korName,
		"engName" to engName,
		"abv" to abv,
		"type" to type.name,
		"korCategory" to korCategory,
		"engCategory" to engCategory,
		"categoryGroup" to categoryGroup.name,
		"regionId" to regionId,
		"distilleryId" to distilleryId,
		"age" to age,
		"cask" to cask,
		"imageUrl" to imageUrl,
		"description" to description,
		"volume" to volume
	)
}

package app.helper.banner

import app.bottlenote.banner.constant.BannerType
import app.bottlenote.banner.constant.TextPosition
import app.bottlenote.banner.dto.response.AdminBannerDetailResponse
import app.bottlenote.banner.dto.response.AdminBannerListResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import java.time.LocalDateTime

object BannerHelper {

    fun createAdminBannerListResponse(
        id: Long = 1L,
        name: String = "테스트 배너",
        bannerType: BannerType = BannerType.CURATION,
        sortOrder: Int = 0,
        isActive: Boolean = true,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        createdAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)
    ): AdminBannerListResponse = AdminBannerListResponse(
        id, name, bannerType, sortOrder, isActive, startDate, endDate, createdAt
    )

    fun createAdminBannerListResponses(count: Int = 3): List<AdminBannerListResponse> =
        (1..count).map { i ->
            createAdminBannerListResponse(
                id = i.toLong(),
                name = "배너 $i",
                sortOrder = i - 1,
                createdAt = LocalDateTime.of(2024, i, 1, 0, 0)
            )
        }

    fun createAdminBannerDetailResponse(
        id: Long = 1L,
        name: String = "테스트 배너",
        nameFontColor: String = "#ffffff",
        descriptionA: String? = "배너 설명A",
        descriptionB: String? = "배너 설명B",
        descriptionFontColor: String = "#ffffff",
        imageUrl: String = "https://example.com/banner.jpg",
        textPosition: TextPosition = TextPosition.RT,
        isExternalUrl: Boolean = false,
        targetUrl: String? = null,
        bannerType: BannerType = BannerType.CURATION,
        sortOrder: Int = 0,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        isActive: Boolean = true,
        createdAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
        modifiedAt: LocalDateTime = LocalDateTime.of(2024, 6, 1, 0, 0)
    ): AdminBannerDetailResponse = AdminBannerDetailResponse(
        id, name, nameFontColor, descriptionA, descriptionB, descriptionFontColor,
        imageUrl, textPosition, isExternalUrl, targetUrl, bannerType, sortOrder,
        startDate, endDate, isActive, createdAt, modifiedAt
    )

    fun createBannerCreateRequest(
        name: String = "새 배너",
        nameFontColor: String = "#ffffff",
        descriptionA: String? = "배너 설명A",
        descriptionB: String? = "배너 설명B",
        descriptionFontColor: String = "#ffffff",
        imageUrl: String = "https://example.com/banner.jpg",
        textPosition: String = "RT",
        isExternalUrl: Boolean = false,
        targetUrl: String? = null,
        bannerType: String = "CURATION",
        sortOrder: Int = 0,
        startDate: String? = null,
        endDate: String? = null
    ): Map<String, Any?> = mapOf(
        "name" to name,
        "nameFontColor" to nameFontColor,
        "descriptionA" to descriptionA,
        "descriptionB" to descriptionB,
        "descriptionFontColor" to descriptionFontColor,
        "imageUrl" to imageUrl,
        "textPosition" to textPosition,
        "isExternalUrl" to isExternalUrl,
        "targetUrl" to targetUrl,
        "bannerType" to bannerType,
        "sortOrder" to sortOrder,
        "startDate" to startDate,
        "endDate" to endDate
    )

    fun createBannerUpdateRequest(
        name: String = "수정된 배너",
        nameFontColor: String = "#000000",
        descriptionA: String? = "수정된 설명A",
        descriptionB: String? = "수정된 설명B",
        descriptionFontColor: String = "#000000",
        imageUrl: String = "https://example.com/updated.jpg",
        textPosition: String = "CENTER",
        isExternalUrl: Boolean = false,
        targetUrl: String? = null,
        bannerType: String = "CURATION",
        sortOrder: Int = 1,
        startDate: String? = null,
        endDate: String? = null,
        isActive: Boolean = true
    ): Map<String, Any?> = mapOf(
        "name" to name,
        "nameFontColor" to nameFontColor,
        "descriptionA" to descriptionA,
        "descriptionB" to descriptionB,
        "descriptionFontColor" to descriptionFontColor,
        "imageUrl" to imageUrl,
        "textPosition" to textPosition,
        "isExternalUrl" to isExternalUrl,
        "targetUrl" to targetUrl,
        "bannerType" to bannerType,
        "sortOrder" to sortOrder,
        "startDate" to startDate,
        "endDate" to endDate,
        "isActive" to isActive
    )

    fun createBannerStatusRequest(isActive: Boolean = true): Map<String, Any> =
        mapOf("isActive" to isActive)

    fun createBannerSortOrderRequest(sortOrder: Int = 1): Map<String, Any> =
        mapOf("sortOrder" to sortOrder)

    fun createAdminResultResponse(
        code: AdminResultResponse.ResultCode = AdminResultResponse.ResultCode.BANNER_CREATED,
        targetId: Long = 1L
    ): AdminResultResponse = AdminResultResponse.of(code, targetId)
}

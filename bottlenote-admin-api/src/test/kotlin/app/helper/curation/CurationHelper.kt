package app.helper.curation

import app.bottlenote.alcohols.dto.response.AdminCurationDetailResponse
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import java.time.LocalDateTime

object CurationHelper {

    fun createAdminCurationListResponse(
        id: Long = 1L,
        name: String = "테스트 큐레이션",
        alcoholCount: Int = 5,
        displayOrder: Int = 1,
        isActive: Boolean = true,
        createdAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)
    ): AdminCurationListResponse = AdminCurationListResponse(
        id, name, alcoholCount, displayOrder, isActive, createdAt
    )

    fun createAdminCurationListResponses(count: Int = 3): List<AdminCurationListResponse> =
        (1..count).map { i ->
            createAdminCurationListResponse(
                id = i.toLong(),
                name = "큐레이션 $i",
                alcoholCount = i * 2,
                displayOrder = i,
                createdAt = LocalDateTime.of(2024, i, 1, 0, 0)
            )
        }

    fun createAdminCurationDetailResponse(
        id: Long = 1L,
        name: String = "테스트 큐레이션",
        description: String = "큐레이션 설명입니다.",
        coverImageUrl: String = "https://example.com/cover.jpg",
        displayOrder: Int = 1,
        isActive: Boolean = true,
        alcoholIds: Set<Long> = setOf(1L, 2L, 3L),
        createdAt: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0),
        modifiedAt: LocalDateTime = LocalDateTime.of(2024, 6, 1, 0, 0)
    ): AdminCurationDetailResponse = AdminCurationDetailResponse(
        id, name, description, coverImageUrl, displayOrder, isActive, alcoholIds, createdAt, modifiedAt
    )

    fun createCurationCreateRequest(
        name: String = "새 큐레이션",
        description: String? = "큐레이션 설명",
        coverImageUrl: String? = "https://example.com/cover.jpg",
        displayOrder: Int = 0,
        alcoholIds: Set<Long> = emptySet()
    ): Map<String, Any?> = mapOf(
        "name" to name,
        "description" to description,
        "coverImageUrl" to coverImageUrl,
        "displayOrder" to displayOrder,
        "alcoholIds" to alcoholIds
    )

    fun createCurationUpdateRequest(
        name: String = "수정된 큐레이션",
        description: String? = "수정된 설명",
        coverImageUrl: String? = "https://example.com/updated.jpg",
        displayOrder: Int = 1,
        isActive: Boolean = true,
        alcoholIds: Set<Long> = setOf(1L, 2L)
    ): Map<String, Any?> = mapOf(
        "name" to name,
        "description" to description,
        "coverImageUrl" to coverImageUrl,
        "displayOrder" to displayOrder,
        "isActive" to isActive,
        "alcoholIds" to alcoholIds
    )

    fun createCurationStatusRequest(isActive: Boolean = true): Map<String, Any> =
        mapOf("isActive" to isActive)

    fun createCurationDisplayOrderRequest(displayOrder: Int = 1): Map<String, Any> =
        mapOf("displayOrder" to displayOrder)

    fun createCurationAlcoholRequest(alcoholIds: Set<Long> = setOf(1L, 2L)): Map<String, Any> =
        mapOf("alcoholIds" to alcoholIds)

    fun createAdminResultResponse(
        code: AdminResultResponse.ResultCode = AdminResultResponse.ResultCode.CURATION_CREATED,
        targetId: Long = 1L
    ): AdminResultResponse = AdminResultResponse.of(code, targetId)
}

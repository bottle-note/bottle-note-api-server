package app.bottlenote.mcp.service

import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem
import app.bottlenote.alcohols.service.AlcoholQueryService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mcp.dto.McpTastingTag
import app.bottlenote.mcp.dto.McpWhiskyDetail
import app.bottlenote.mcp.dto.McpWhiskySearchRequest
import app.bottlenote.mcp.dto.McpWhiskySearchResult
import app.bottlenote.mcp.dto.McpWhiskySummary
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

/**
 * MCP-facing whisky read model. Reuses domain query services; shapes payloads for agents.
 */
@Service
class AdminMcpWhiskyService(
	private val alcoholQueryService: AlcoholQueryService,
	private val objectMapper: ObjectMapper
) {
	fun search(request: McpWhiskySearchRequest): McpWhiskySearchResult {
		val page = (request.page ?: 0).coerceAtLeast(0)
		val size = (request.size ?: 20).coerceIn(1, 50)
		// Java record: positional args only from Kotlin
		val adminRequest =
			AdminAlcoholSearchRequest(
				request.keyword,
				null,
				request.regionId,
				null,
				null,
				page,
				size,
				false
			)
		// AlcoholQueryService.searchAdminAlcohols uses GlobalResponse.fromPage:
		// data = List content, meta.totalElements / meta.hasNext
		val global: GlobalResponse = alcoholQueryService.searchAdminAlcohols(adminRequest)
		val items = extractPageItems(global).map { it.toSummary() }
		val total = metaLong(global, "totalElements")
		val hasNext = metaBoolean(global, "hasNext") ?: (items.size >= size)
		return McpWhiskySearchResult(
			items = items,
			page = page,
			size = size,
			totalElements = total,
			hasNext = hasNext
		)
	}

	fun getDetail(alcoholId: Long): McpWhiskyDetail {
		val detail = alcoholQueryService.findAdminAlcoholDetailById(alcoholId)
		return detail.toMcpDetail()
	}

	private fun extractPageItems(global: GlobalResponse): List<AdminAlcoholItem> {
		val data = global.data ?: return emptyList()
		if (data is List<*>) {
			return data.mapNotNull { convertItem(it) }
		}
		return emptyList()
	}

	private fun metaLong(global: GlobalResponse, key: String): Long? {
		val value = global.meta?.get(key) ?: return null
		return when (value) {
			is Number -> value.toLong()
			is String -> value.toLongOrNull()
			else -> null
		}
	}

	private fun metaBoolean(global: GlobalResponse, key: String): Boolean? {
		val value = global.meta?.get(key) ?: return null
		return when (value) {
			is Boolean -> value
			is String -> value.toBooleanStrictOrNull()
			else -> null
		}
	}

	private fun convertItem(raw: Any?): AdminAlcoholItem? {
		if (raw == null) return null
		return when (raw) {
			is AdminAlcoholItem -> raw
			else ->
				runCatching {
					objectMapper.convertValue(raw, AdminAlcoholItem::class.java)
				}.getOrNull()
		}
	}

	private fun AdminAlcoholItem.toSummary(): McpWhiskySummary = McpWhiskySummary(
		alcoholId = alcoholId,
		korName = korName,
		engName = engName,
		korCategory = korCategoryName,
		engCategory = engCategoryName,
		imageUrl = imageUrl
	)

	private fun AdminAlcoholDetailResponse.toMcpDetail(): McpWhiskyDetail = McpWhiskyDetail(
		alcoholId = alcoholId,
		korName = korName,
		engName = engName,
		korCategory = korCategory,
		engCategory = engCategory,
		imageUrl = imageUrl,
		abv = abv,
		age = age,
		cask = cask,
		volume = volume,
		description = description,
		regionId = regionId,
		korRegion = korRegion,
		engRegion = engRegion,
		distilleryId = distilleryId,
		korDistillery = korDistillery,
		engDistillery = engDistillery,
		tastingTags =
		tastingTags.map { tag ->
			McpTastingTag(id = tag.id, korName = tag.korName, engName = tag.engName)
		}
	)
}

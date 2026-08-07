package app.bottlenote.mcp.dto

/**
 * MCP-optimized whisky payloads: compact fields for agent context.
 * Not a 1:1 dump of AdminAlcoholDetailResponse.
 */
data class McpWhiskySummary(
	val alcoholId: Long,
	val korName: String?,
	val engName: String?,
	val korCategory: String?,
	val engCategory: String?,
	val imageUrl: String?,
)

data class McpTastingTag(
	val id: Long,
	val korName: String?,
	val engName: String?,
)

data class McpWhiskyDetail(
	val alcoholId: Long,
	val korName: String?,
	val engName: String?,
	val korCategory: String?,
	val engCategory: String?,
	val imageUrl: String?,
	val abv: String?,
	val age: String?,
	val cask: String?,
	val volume: String?,
	val description: String?,
	val regionId: Long?,
	val korRegion: String?,
	val engRegion: String?,
	val distilleryId: Long?,
	val korDistillery: String?,
	val engDistillery: String?,
	val tastingTags: List<McpTastingTag>,
)

data class McpWhiskySearchResult(
	val items: List<McpWhiskySummary>,
	val page: Int,
	val size: Int,
	val totalElements: Long?,
	val hasNext: Boolean?,
)

data class McpWhiskySearchRequest(
	val keyword: String? = null,
	val regionId: Long? = null,
	val page: Int? = null,
	val size: Int? = null,
)

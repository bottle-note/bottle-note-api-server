package app.global.docs

import app.global.admin.AdminApi
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

// product 문서와 admin 문서를 하나의 OpenAPI 문서로 합친다. 두 문서는 수정하지 않고 복사본으로 만든다.
// 이름이 같은 components·tags·operationId는 내용이 같으면 하나로 합치고, 다르면 admin 쪽 이름을 결정적으로 바꾼 뒤 참조를 함께 고친다.
class UnifiedOpenApiMerger {

	fun merge(product: ObjectNode, admin: ObjectNode, info: ObjectNode): Result {
		require(product.path(OPENAPI) == admin.path(OPENAPI)) {
			"product와 admin 문서의 OpenAPI 버전이 다르다: ${product.path(OPENAPI)} / ${admin.path(OPENAPI)}"
		}
		val componentRenames = componentRenames(product.path(COMPONENTS), admin.path(COMPONENTS))
		val adminDoc = rewriteText(admin.deepCopy(), refMapping(componentRenames)) as ObjectNode
		renameSecurityRequirements(adminDoc, componentRenames)
		val tags = mergeTags(product.path(TAGS), adminDoc.path(TAGS))
		renameOperationTags(adminDoc, tags.renames)
		val operationIdRenames = renameOperationIds(product, adminDoc)
		val mergedComponents = mutableListOf<String>()

		val unified = FACTORY.objectNode()
		unified.set<JsonNode>(OPENAPI, product.path(OPENAPI).deepCopy())
		unified.set<JsonNode>(INFO, info)
		unified.set<JsonNode>(SERVERS, product.path(SERVERS).deepCopy())
		unified.set<JsonNode>(TAGS, tags.document)
		unified.set<JsonNode>(PATHS, mergePaths(product.path(PATHS), adminDoc.path(PATHS)))
		unified.set<JsonNode>(COMPONENTS, mergeComponents(product.path(COMPONENTS), adminDoc.path(COMPONENTS), componentRenames, mergedComponents))
		copyOtherFields(product, adminDoc, unified)

		val report =
			Report(
				mergedComponents = mergedComponents,
				renamedComponents = componentRenames.mapKeys { (key, _) -> key.toString() },
				mergedTags = tags.merged,
				renamedTags = tags.renames,
				renamedOperationIds = operationIdRenames
			)
		return Result(unified, report)
	}

	// 같은 이름의 component가 admin 쪽 참조를 고친 뒤에도 다르면 이름을 바꾼다. 이름을 바꾸면 그 component를 참조하는 다른 component도 달라질 수 있으므로 더 바뀌지 않을 때까지 반복한다.
	private fun componentRenames(productComponents: JsonNode, adminComponents: JsonNode): Map<ComponentKey, String> {
		val renames = linkedMapOf<ComponentKey, String>()
		do {
			val rewritten = rewriteText(adminComponents.deepCopy(), refMapping(renames))
			var changed = false
			adminComponents.fields().forEach { (type, entries) ->
				entries.fieldNames().forEach { name ->
					val key = ComponentKey(type, name)
					val productEntry = productComponents.path(type).get(name)
					if (key !in renames && productEntry != null && productEntry != rewritten.path(type).get(name)) {
						renames[key] =
							uniqueName(ADMIN_COMPONENT_PREFIX + name) { candidate ->
								productComponents.path(type).has(candidate) ||
									adminComponents.path(type).has(candidate) ||
									renames.any { (renamed, assigned) -> renamed.type == type && assigned == candidate }
							}
						changed = true
					}
				}
			}
		} while (changed)
		return renames
	}

	private fun refMapping(renames: Map<ComponentKey, String>): Map<String, String> = renames.entries.associate { (key, renamed) ->
		"$COMPONENTS_REF${key.type}/${key.name}" to "$COMPONENTS_REF${key.type}/$renamed"
	}

	// $ref와 discriminator mapping처럼 component를 가리키는 문자열 값만 정확히 일치할 때 바꾼다
	private fun rewriteText(node: JsonNode, mapping: Map<String, String>): JsonNode {
		if (mapping.isEmpty()) {
			return node
		}
		when (node) {
			is ObjectNode -> node.fields().forEach { entry -> entry.setValue(rewriteText(entry.value, mapping)) }
			is ArrayNode -> for (index in 0 until node.size()) {
				node.set(index, rewriteText(node.get(index), mapping))
			}
			is TextNode -> mapping[node.textValue()]?.let { return TextNode.valueOf(it) }
		}
		return node
	}

	// security requirement는 $ref가 아니라 securitySchemes 이름을 키로 쓰므로 따로 고친다
	private fun renameSecurityRequirements(adminDoc: ObjectNode, componentRenames: Map<ComponentKey, String>) {
		val schemeRenames = componentRenames.filterKeys { it.type == SECURITY_SCHEMES }.mapKeys { it.key.name }
		if (schemeRenames.isEmpty()) {
			return
		}
		renameRequirementKeys(adminDoc.path(SECURITY), schemeRenames)
		operations(adminDoc).forEach { operation -> renameRequirementKeys(operation.path(SECURITY), schemeRenames) }
	}

	private fun renameRequirementKeys(requirements: JsonNode, renames: Map<String, String>) {
		requirements.filterIsInstance<ObjectNode>().forEach { requirement ->
			val renamed = FACTORY.objectNode()
			requirement.fields().forEach { (name, scopes) -> renamed.set<JsonNode>(renames[name] ?: name, scopes) }
			requirement.removeAll()
			requirement.setAll<JsonNode>(renamed)
		}
	}

	private fun mergeTags(productTags: JsonNode, adminTags: JsonNode): MergedTags {
		val document = FACTORY.arrayNode()
		productTags.forEach { document.add(it.deepCopy<JsonNode>()) }
		val productByName = productTags.associateBy { it.path(NAME).asText() }
		val adminNames = adminTags.map { it.path(NAME).asText() }.toSet()
		val renames = linkedMapOf<String, String>()
		val merged = mutableListOf<String>()
		adminTags.forEach { tag ->
			val name = tag.path(NAME).asText()
			val productTag = productByName[name]
			when {
				productTag == null -> document.add(tag.deepCopy<JsonNode>())
				productTag == tag -> merged += name
				else -> {
					val renamed =
						uniqueName(ADMIN_TAG_PREFIX + name) { candidate ->
							candidate in productByName || candidate in adminNames || candidate in renames.values
						}
					renames[name] = renamed
					document.add((tag.deepCopy<JsonNode>() as ObjectNode).put(NAME, renamed))
				}
			}
		}
		return MergedTags(document, renames, merged)
	}

	private fun renameOperationTags(adminDoc: ObjectNode, renames: Map<String, String>) {
		if (renames.isEmpty()) {
			return
		}
		operations(adminDoc).forEach { operation ->
			val tags = operation.get(TAGS) as? ArrayNode ?: return@forEach
			for (index in 0 until tags.size()) {
				renames[tags.get(index).asText()]?.let { tags.set(index, TextNode.valueOf(it)) }
			}
		}
	}

	private fun renameOperationIds(product: ObjectNode, adminDoc: ObjectNode): Map<String, String> {
		val productIds = operations(product).mapNotNull { it.get(OPERATION_ID)?.asText() }.toSet()
		val adminIds = operations(adminDoc).mapNotNull { it.get(OPERATION_ID)?.asText() }.toSet()
		val renames = linkedMapOf<String, String>()
		operations(adminDoc).forEach { operation ->
			val id = operation.get(OPERATION_ID)?.asText() ?: return@forEach
			if (id in productIds) {
				val renamed =
					uniqueName(ADMIN_OPERATION_PREFIX + id.replaceFirstChar { it.uppercaseChar() }) { candidate ->
						candidate in productIds || candidate in adminIds || candidate in renames.values
					}
				renames[id] = renamed
				operation.put(OPERATION_ID, renamed)
			}
		}
		return renames
	}

	// admin 문서의 경로 키는 admin 앱 기준(/v1/..., /v2/...)이므로 mount를 붙여 실제 전체 경로로 만든다
	private fun mergePaths(productPaths: JsonNode, adminPaths: JsonNode): ObjectNode {
		val paths = FACTORY.objectNode()
		productPaths.fields().forEach { (path, item) -> paths.set<JsonNode>(path, item.deepCopy()) }
		adminPaths.fields().forEach { (path, item) ->
			require(path.startsWith("/")) { "admin 문서의 경로 키가 /로 시작하지 않는다: $path" }
			val unifiedPath = AdminApi.PATH_PREFIX + path
			check(!paths.has(unifiedPath)) { "product와 admin 문서의 경로가 겹친다: $unifiedPath" }
			paths.set<JsonNode>(unifiedPath, item)
		}
		return paths
	}

	private fun mergeComponents(
		productComponents: JsonNode,
		adminComponents: JsonNode,
		renames: Map<ComponentKey, String>,
		merged: MutableList<String>
	): ObjectNode {
		val components = FACTORY.objectNode()
		productComponents.fields().forEach { (type, entries) -> components.set<JsonNode>(type, entries.deepCopy()) }
		adminComponents.fields().forEach { (type, entries) ->
			val target = components.get(type) as? ObjectNode ?: components.putObject(type)
			entries.fields().forEach { (name, entry) ->
				val key = ComponentKey(type, name)
				val renamed = renames[key]
				when {
					renamed != null -> target.set<JsonNode>(renamed, entry)
					target.has(name) -> {
						check(target.get(name) == entry) { "같은 이름의 component를 하나로 합치려 했지만 내용이 다르다: $key" }
						merged += key.toString()
					}
					else -> target.set<JsonNode>(name, entry)
				}
			}
		}
		return components
	}

	// 위에서 다루지 않은 최상위 항목은 product 값을 쓰고, admin에만 있으면 옮긴다. 같은 키에 다른 값이 있으면 덮어쓰지 않고 실패한다.
	private fun copyOtherFields(product: ObjectNode, adminDoc: ObjectNode, unified: ObjectNode) {
		product.fields().forEach { (key, value) -> if (key !in HANDLED_FIELDS) unified.set<JsonNode>(key, value.deepCopy()) }
		adminDoc.fields().forEach { (key, value) ->
			if (key in HANDLED_FIELDS) return@forEach
			val existing = unified.get(key)
			check(existing == null || existing == value) { "product와 admin 문서의 최상위 항목 '$key'가 다르다" }
			unified.set<JsonNode>(key, value)
		}
	}

	private fun operations(document: JsonNode): List<ObjectNode> = document.path(PATHS).flatMap { item ->
		item.fields().asSequence().filter { (method, _) -> method in HTTP_METHODS }.mapNotNull { (_, operation) -> operation as? ObjectNode }.toList()
	}

	private fun uniqueName(base: String, taken: (String) -> Boolean): String {
		if (!taken(base)) {
			return base
		}
		var suffix = 2
		while (taken("${base}_$suffix")) {
			suffix++
		}
		return "${base}_$suffix"
	}

	data class Result(val document: ObjectNode, val report: Report)

	data class Report(
		val mergedComponents: List<String>,
		val renamedComponents: Map<String, String>,
		val mergedTags: List<String>,
		val renamedTags: Map<String, String>,
		val renamedOperationIds: Map<String, String>
	)

	private data class ComponentKey(val type: String, val name: String) {
		override fun toString(): String = "$type/$name"
	}

	private data class MergedTags(val document: ArrayNode, val renames: Map<String, String>, val merged: List<String>)

	companion object {
		const val ADMIN_COMPONENT_PREFIX = "Admin"
		const val ADMIN_TAG_PREFIX = "Admin "
		const val ADMIN_OPERATION_PREFIX = "admin"

		private const val OPENAPI = "openapi"
		private const val INFO = "info"
		private const val SERVERS = "servers"
		private const val TAGS = "tags"
		private const val PATHS = "paths"
		private const val COMPONENTS = "components"
		private const val SECURITY = "security"
		private const val SECURITY_SCHEMES = "securitySchemes"
		private const val NAME = "name"
		private const val OPERATION_ID = "operationId"
		private const val COMPONENTS_REF = "#/components/"

		private val HANDLED_FIELDS = setOf(OPENAPI, INFO, SERVERS, TAGS, PATHS, COMPONENTS)
		private val HTTP_METHODS = setOf("get", "put", "post", "delete", "options", "head", "patch", "trace")
		private val FACTORY = JsonNodeFactory.instance
	}
}

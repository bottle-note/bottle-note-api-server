package app.integration.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
class AlcoholCandidateOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {
	@Test fun documentsCandidateBodyPaginationAndConcreteResponse() {
		val spec = fetchSpec()
		val op = operation(spec, "POST /v1/attribute/groups/{groupId}/candidates")
		assertThat(op.security().any { it.has("bearerAuth") }).isTrue()
		assertThat(op.hasEmptyDataSchema()).isFalse()
		val request = spec.at("/components/schemas/AdminAlcoholCandidateRequest")
		assertThat(propertyNamesOf(request)).contains("include", "exclude", "nextQuestion", "page", "size", "abvFrom", "abvTo", "tastingGroupCodes")
		assertThat(propertyNamesOf(request)).doesNotContain("cursor")
		assertThat(request.at("/properties/size/maximum").asInt()).isEqualTo(100)
		assertThat(propertyNamesOf(spec.at("/components/schemas/AdminAlcoholCandidateResponse"))).contains("group", "attributeDefinitions", "items", "totalCount", "facets")
		assertThat(propertyNamesOf(spec.at("/components/schemas/CandidateAttributeValues"))).containsExactlyInAnyOrder("attributeId", "values")
		assertThat(propertyNamesOf(spec.at("/components/schemas/CandidateAttributeDefinition"))).contains("displayName", "datatype", "allowedValues")
	}
}

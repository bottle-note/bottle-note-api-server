package app.global.dto.response

import app.bottlenote.alcohols.constant.AlcoholAdminResultCode
import app.bottlenote.banner.constant.BannerAdminResultCode
import app.bottlenote.campaigncontent.constant.CampaignContentAdminResultCode
import app.bottlenote.curation.constant.CurationAdminResultCode
import app.bottlenote.global.dto.response.AdminResultCode
import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.mfds.constant.MfdsAdminResultCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Tag("unit")
class AdminResultCodeContractTest {
	private val expected = mapOf(
		"ALCOHOL_CREATED" to "위스키가 등록되었습니다.",
		"ALCOHOL_UPDATED" to "위스키가 수정되었습니다.",
		"ALCOHOL_DELETED" to "위스키가 삭제되었습니다.",
		"ALCOHOL_MERGED" to "위스키가 병합되었습니다.",
		"ALCOHOL_ATTRIBUTE_GROUP_DELETED" to "속성 그룹이 삭제되었습니다.",
		"ALCOHOL_ATTRIBUTE_UPDATED" to "위스키 속성이 수정되었습니다.",
		"ALCOHOL_ATTRIBUTE_MAPPING_DELETED" to "주류 속성 연결이 해제되었습니다.",
		"ALCOHOL_ATTRIBUTE_DELETED" to "위스키 속성이 삭제되었습니다.",
		"DISTILLERY_CREATED" to "증류소가 등록되었습니다.",
		"DISTILLERY_UPDATED" to "증류소가 수정되었습니다.",
		"DISTILLERY_DELETED" to "증류소가 삭제되었습니다.",
		"DISTILLERY_SORT_ORDER_UPDATED" to "증류소 정렬 순서가 변경되었습니다.",
		"TASTING_TAG_CREATED" to "테이스팅 태그가 등록되었습니다.",
		"TASTING_TAG_UPDATED" to "테이스팅 태그가 수정되었습니다.",
		"TASTING_TAG_DELETED" to "테이스팅 태그가 삭제되었습니다.",
		"TASTING_TAG_ALCOHOL_ADDED" to "위스키가 연결되었습니다.",
		"TASTING_TAG_ALCOHOL_REMOVED" to "위스키 연결이 해제되었습니다.",
		"TASTING_TAG_GROUP_CREATED" to "맛 그룹이 등록되었습니다.",
		"TASTING_TAG_GROUP_UPDATED" to "맛 그룹이 수정되었습니다.",
		"TASTING_TAG_GROUP_DELETED" to "맛 그룹이 삭제되었습니다.",
		"TASTING_TAG_GROUP_TAG_ADDED" to "맛 그룹에 태그가 연결되었습니다.",
		"TASTING_TAG_GROUP_TAG_ROLE_UPDATED" to "맛 그룹 태그의 역할이 변경되었습니다.",
		"TASTING_TAG_GROUP_TAG_REMOVED" to "맛 그룹에서 태그가 제거되었습니다.",
		"CURATION_CREATED" to "큐레이션이 등록되었습니다.",
		"CURATION_UPDATED" to "큐레이션이 수정되었습니다.",
		"CURATION_DELETED" to "큐레이션이 삭제되었습니다.",
		"CURATION_STATUS_UPDATED" to "큐레이션 활성화 상태가 변경되었습니다.",
		"CURATION_DISPLAY_ORDER_UPDATED" to "큐레이션 노출 순서가 변경되었습니다.",
		"CURATION_ALCOHOL_ADDED" to "큐레이션에 위스키가 추가되었습니다.",
		"CURATION_ALCOHOL_REMOVED" to "큐레이션에서 위스키가 제거되었습니다.",
		"BANNER_CREATED" to "배너가 등록되었습니다.",
		"BANNER_UPDATED" to "배너가 수정되었습니다.",
		"BANNER_DELETED" to "배너가 삭제되었습니다.",
		"BANNER_STATUS_UPDATED" to "배너 활성화 상태가 변경되었습니다.",
		"BANNER_SORT_ORDER_UPDATED" to "배너 정렬 순서가 변경되었습니다.",
		"REGION_CREATED" to "지역이 등록되었습니다.",
		"REGION_UPDATED" to "지역이 수정되었습니다.",
		"REGION_DELETED" to "지역이 삭제되었습니다.",
		"REGION_SORT_ORDER_UPDATED" to "지역 정렬 순서가 변경되었습니다.",
		"MFDS_IMPORTER_CREATED" to "수입사가 등록되었습니다.",
		"MFDS_IMPORTER_UPDATED" to "수입사가 수정되었습니다.",
		"MFDS_IMPORTER_DELETED" to "수입사가 삭제되었습니다.",
		"MFDS_DECLARATION_STATUS_UPDATED" to "수입 신고 정규화 상태가 변경되었습니다.",
		"MFDS_DECLARATION_IMPORTER_LINKED" to "수입 신고에 수입사가 연결되었습니다.",
		"MFDS_DECLARATION_IMPORTER_UNLINKED" to "수입 신고의 수입사 연결이 해제되었습니다.",
		"MFDS_RCNO_LINK_CREATED" to "수입신고번호 연결 근거가 등록되었습니다.",
		"MFDS_RCNO_LINK_DELETED" to "수입신고번호 연결 근거가 삭제되었습니다.",
		"CAMPAIGN_CONTENT_CREATED" to "캠페인 콘텐츠가 등록되었습니다.",
		"CAMPAIGN_CONTENT_UPDATED" to "캠페인 콘텐츠가 수정되었습니다.",
		"CAMPAIGN_CONTENT_DELETED" to "캠페인 콘텐츠가 삭제되었습니다.",
		"CAMPAIGN_CONTENT_STATUS_UPDATED" to "캠페인 콘텐츠 활성화 상태가 변경되었습니다."
	)

	private fun codes(): List<AdminResultCode> = AlcoholAdminResultCode.values().toList() + MfdsAdminResultCode.values().toList() +
		BannerAdminResultCode.values().toList() + CampaignContentAdminResultCode.values().toList() +
		CurationAdminResultCode.values().toList()

	@Test
	@DisplayName("업무 코드를 모을 때 원문 51쌍과 전체 53개를 유지한다")
	fun preservesOriginalBusinessCodePairs() {
		val all = codes()
		assertThat(
			listOf(
				AlcoholAdminResultCode.values().size,
				MfdsAdminResultCode.values().size,
				BannerAdminResultCode.values().size,
				CampaignContentAdminResultCode.values().size,
				CurationAdminResultCode.values().size
			)
		).containsExactly(34, 8, 5, 4, 2)
		assertThat(all).hasSize(53)
		val actual: List<Pair<String, String>> = all.map { it.name() to it.getMessage() }.distinct()
		val originalPairs: List<Pair<String, String>> = expected.toList()
		assertThat(actual).hasSize(51).containsExactlyInAnyOrderElementsOf(originalPairs)
	}

	@Test
	@DisplayName("두 업무가 큐레이션 코드를 반환할 때 같은 이름과 문구를 유지한다")
	fun preservesDuplicateCurationMessages() {
		assertThat(CurationAdminResultCode.values().map { it.name }).containsExactly("CURATION_CREATED", "CURATION_UPDATED")
		CurationAdminResultCode.values().forEach { content ->
			val alcohol = AlcoholAdminResultCode.valueOf(content.name)
			assertThat(content.getMessage()).isEqualTo(alcohol.getMessage()).isEqualTo(expected[content.name])
		}
	}

	@Test
	@DisplayName("결과 응답을 직렬화할 때 원문 코드와 문구 및 JSON 필드와 날짜 형식을 유지한다")
	fun preservesResponseJsonContract() {
		val mapper = ObjectMapper()
		codes().forEach { code ->
			val response = AdminResultResponse.of(code, 42L)
			val json = mapper.readTree(mapper.writeValueAsString(response))
			assertThat(json.fieldNames().asSequence().toList()).containsExactly("code", "message", "targetId", "responseAt")
			assertThat(json["code"].asText()).isEqualTo(code.name())
			assertThat(json["message"].asText()).isEqualTo(expected[code.name()])
			assertThat(json["targetId"].asLong()).isEqualTo(42L)
			val timestamp = json["responseAt"].asText()
			assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
			assertThat(LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).isNotNull()
		}
		assertThat(mapper.readTree(mapper.writeValueAsString(AdminResultResponse.of(AlcoholAdminResultCode.ALCOHOL_CREATED, null)))["targetId"].isNull).isTrue()
	}
}

package app.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// 스펙 문서의 최상단 정보와 인증 방식을 정의한다. 개별 엔드포인트 설명은 각 도메인의 문서 어노테이션이 담당한다.
@Configuration
class OpenApiConfig {
	@Bean
	fun adminOpenApi(): OpenAPI = OpenAPI()
		.info(adminApiInfo())
		.tags(MENU.flatMap { it.tags })
		.components(securityComponents())
		.apply { addExtension(TAG_GROUPS, MENU.map { it.toExtensionEntry() }) }

	private fun adminApiInfo(): Info = Info()
		.title("보틀노트 Admin API")
		.version("v1")
		.description(
			"""
			보틀노트 어드민 콘솔이 사용하는 관리자 API입니다.

			모든 응답은 success, code, data, errors, meta를 갖는 공통 형식으로 감싸여 있고, 실제 결과값은 data 안에 담깁니다.

			인증이 필요한 엔드포인트는 로그인 후 발급받은 액세스 토큰을 Authorization 헤더에 "Bearer {토큰}" 형태로 담아 호출합니다.
			""".trimIndent()
		)

	private fun securityComponents(): Components = Components()
		.addSecuritySchemes(
			BEARER_AUTH,
			SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("로그인 응답으로 받은 액세스 토큰을 그대로 입력합니다.")
		)

	// 사이드바의 상위 그룹 하나와 그 안에 들어가는 태그들
	private data class TagGroup(val name: String, val tags: List<Tag>) {
		// 그룹에서 빠진 태그는 Scalar 사이드바에 아예 나오지 않으므로 태그 목록과 같은 출처에서 만든다
		fun toExtensionEntry(): Map<String, Any> = linkedMapOf(
			"name" to name,
			"tags" to tags.map { it.name }
		)
	}

	companion object {
		// components 하위 이름은 OpenAPI 규칙상 영문 식별자여야 한다.
		const val BEARER_AUTH = "bearerAuth"

		// Scalar가 사이드바를 상위 그룹으로 접을 때 읽는 확장 키
		const val TAG_GROUPS = "x-tagGroups"

		private fun tag(name: String, description: String): Tag = Tag().name(name).description(description)

		private fun group(name: String, vararg tags: Tag): TagGroup = TagGroup(name, tags.toList())

		// 사이드바에 나올 메뉴 전체. 그룹 순서와 그룹 안의 태그 순서가 그대로 화면 순서가 된다.
		// 태그의 이름과 설명은 여기에만 적는다. 문서 어노테이션에 설명을 함께 적으면 springdoc이 태그 목록을 다시 정렬해서
		// 이 순서가 깨지고, 같은 이름을 설명만 다르게 선언하면 스펙에 태그가 중복으로 실린다.
		private val MENU = listOf(
			group(
				"계정과 권한",
				tag("인증", "관리자 로그인, 토큰 재발급, 관리자 계정 등록·탈퇴를 처리한다"),
				tag("회원 관리", "가입한 회원 목록을 검색하고 조회한다"),
				tag("IP 접근 제어", "IP 차단 상태, 감사 이력, 보안 signal 판정을 관리한다")
			),
			group(
				"위스키 기준 정보",
				tag("알코올", "위스키를 조회·등록·수정·삭제하고 엑셀 파일을 검증한다"),
				tag("알코올 벌크", "엑셀과 JSON으로 위스키를 검증하고 일괄 등록한다"),
				tag("증류소", "위스키 증류소 기준 정보를 등록·수정·삭제하고 정렬 순서를 관리한다"),
				tag("지역", "위스키 생산 지역 기준 정보를 등록·수정·삭제하고 정렬 순서를 관리한다"),
				tag("테이스팅 태그", "위스키 맛·향 테이스팅 태그를 등록·수정·삭제하고 위스키와의 연결을 관리한다"),
				tag(
					"수입 정보",
					"식약처 수입 원장에서 수집한 수입사와 수입 신고 데이터를 조회하고, 수입사 연결 근거와 BottleNote 위스키 매칭을 관리한다"
				)
			),
			group(
				"큐레이션",
				tag("큐레이션", "위스키를 묶어 노출하는 큐레이션을 등록·수정·삭제하고 정렬 순서와 포함 위스키를 관리한다"),
				tag("큐레이션 스펙", "스펙 기반 큐레이션 작성에 쓰는 스펙(필드 정의)을 조회한다"),
				tag("스펙 기반 큐레이션", "큐레이션 스펙을 기반으로 만든 큐레이션을 등록·수정하고 목록·피드·상세를 조회한다")
			),
			group(
				"고객 지원",
				tag("문의", "사용자가 남긴 문의를 조회하고 답변을 등록한다"),
				tag("리뷰 관리", "작성된 리뷰 목록을 검색하고 조회한다")
			),
			group(
				"공통",
				tag("이미지 업로드", "S3에 직접 업로드할 수 있는 presigned URL을 발급한다"),
				tag("배너", "앱에 노출되는 배너를 등록·수정·삭제하고 노출 상태와 정렬 순서를 관리한다"),
				tag("통계", "방문자 활동과 위스키 지표 시계열을 조회한다")
			)
		)
	}
}

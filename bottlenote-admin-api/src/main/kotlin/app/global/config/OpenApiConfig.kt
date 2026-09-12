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
		.tags(MENU)
		.components(securityComponents())

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

	companion object {
		// components 하위 이름은 OpenAPI 규칙상 영문 식별자여야 한다.
		const val BEARER_AUTH = "bearerAuth"

		private fun tag(name: String, description: String): Tag = Tag().name(name).description(description)

		// 사이드바에 나올 메뉴 전체. 선언 순서가 그대로 화면 순서가 된다.
		// 태그의 이름과 설명은 여기에만 적는다. 문서 어노테이션에 설명을 함께 적으면 springdoc이 태그 목록을 다시 정렬해서
		// 이 순서가 깨지고, 같은 이름을 설명만 다르게 선언하면 스펙에 태그가 중복으로 실린다.
		private val MENU = listOf(
			tag("인증", "관리자 로그인, 토큰 재발급, 관리자 계정 등록·탈퇴를 처리한다"),
			tag("운영 지원", "회원과 리뷰를 조회하고 문의에 답변하며 이미지 업로드 주소를 발급한다"),
			tag("IP 접근 제어", "IP 차단 상태, 감사 이력, 보안 signal 판정을 관리한다"),
			tag("알코올", "위스키를 조회·등록·수정·삭제하고 엑셀과 JSON으로 일괄 검증·등록한다"),
			tag("기준 정보", "증류소, 지역, 테이스팅 태그를 등록·수정·삭제하고 정렬 순서와 연결을 관리한다"),
			tag(
				"수입 정보",
				"식약처 수입 원장에서 수집한 수입사와 수입 신고 데이터를 조회하고, 수입사 연결 근거와 BottleNote 위스키 매칭을 관리한다"
			),
			tag("큐레이션", "큐레이션과 큐레이션 스펙을 등록·수정·삭제하고 목록·피드·상세를 조회한다"),
			tag("배너", "앱에 노출되는 배너를 등록·수정·삭제하고 노출 상태와 정렬 순서를 관리한다"),
			tag("통계", "방문자 활동과 위스키 지표 시계열을 조회한다")
		)
	}
}

package app.bottlenote.statistics.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.statistics.dto.request.MemberCohortRetentionRequest
import app.bottlenote.statistics.dto.request.MemberFunnelRequest
import app.bottlenote.statistics.facade.MemberStatisticsFacade
import app.bottlenote.statistics.presentation.docs.AdminMemberStatisticsApiDocs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/statistics/members")
@AdminMemberStatisticsApiDocs.ApiTag
class AdminMemberStatisticsController(
	private val memberStatisticsFacade: MemberStatisticsFacade
) {
	@AdminMemberStatisticsApiDocs.Funnel
	@GetMapping("/funnel")
	fun funnel(
		@ModelAttribute request: MemberFunnelRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(memberStatisticsFacade.findFunnel(request))

	@AdminMemberStatisticsApiDocs.CohortRetention
	@GetMapping("/cohort-retention")
	fun cohortRetention(
		@ModelAttribute request: MemberCohortRetentionRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(memberStatisticsFacade.findCohortRetention(request))
}

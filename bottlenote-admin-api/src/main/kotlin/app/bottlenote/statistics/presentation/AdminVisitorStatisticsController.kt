package app.bottlenote.statistics.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.statistics.dto.request.VisitorStatisticsRequest
import app.bottlenote.statistics.facade.VisitorStatisticsFacade
import app.bottlenote.statistics.presentation.docs.AdminVisitorStatisticsApiDocs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/statistics/visitors")
@AdminVisitorStatisticsApiDocs.ApiTag
class AdminVisitorStatisticsController(
	private val visitorStatisticsFacade: VisitorStatisticsFacade
) {
	@AdminVisitorStatisticsApiDocs.Active
	@GetMapping("/active")
	fun active(
		@ModelAttribute request: VisitorStatisticsRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(visitorStatisticsFacade.findActiveVisitors(request))

	@AdminVisitorStatisticsApiDocs.FindRetention
	@GetMapping("/retention")
	fun retention(
		@ModelAttribute request: VisitorStatisticsRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(visitorStatisticsFacade.findRetention(request))
}

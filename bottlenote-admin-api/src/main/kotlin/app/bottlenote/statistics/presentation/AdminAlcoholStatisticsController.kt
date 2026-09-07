package app.bottlenote.statistics.presentation

import app.bottlenote.alcohols.constant.PopularityAxis
import app.bottlenote.alcohols.dto.request.AlcoholPopularityTimeSeriesRequest
import app.bottlenote.alcohols.facade.AlcoholPopularityFacade
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.statistics.presentation.docs.AdminAlcoholStatisticsApiDocs
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/statistics/alcohols")
@AdminAlcoholStatisticsApiDocs.ApiTag
class AdminAlcoholStatisticsController(
	private val alcoholPopularityFacade: AlcoholPopularityFacade
) {
	@AdminAlcoholStatisticsApiDocs.GetPopularity
	@GetMapping("/{alcoholId}/popularity")
	fun getPopularity(
		@PathVariable alcoholId: Long,
		@ModelAttribute request: AlcoholPopularityTimeSeriesRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(alcoholPopularityFacade.findPopularity(alcoholId, request))

	@AdminAlcoholStatisticsApiDocs.GetObservations
	@GetMapping("/{alcoholId}/observations/{axis}")
	fun getObservations(
		@PathVariable alcoholId: Long,
		@PathVariable axis: PopularityAxis,
		@ModelAttribute request: AlcoholPopularityTimeSeriesRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(
		alcoholPopularityFacade.findObservations(alcoholId, axis, request)
	)
}

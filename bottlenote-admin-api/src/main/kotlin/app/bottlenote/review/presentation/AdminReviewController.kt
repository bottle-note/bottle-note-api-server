package app.bottlenote.review.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.review.dto.request.AdminReviewSearchRequest
import app.bottlenote.review.service.AdminReviewQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/reviews")
class AdminReviewController(
	private val adminReviewQueryService: AdminReviewQueryService
) {
	@GetMapping
	fun list(
		@ModelAttribute request: AdminReviewSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminReviewQueryService.searchReviews(request))
}

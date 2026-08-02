package app.bottlenote.review.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.review.dto.request.AdminReviewSearchRequest
import app.bottlenote.review.presentation.docs.AdminReviewApiDocs
import app.bottlenote.review.service.AdminReviewQueryService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/reviews")
@AdminReviewApiDocs.ApiTag
class AdminReviewController(
	private val adminReviewQueryService: AdminReviewQueryService
) {
	@AdminReviewApiDocs.ListReviews
	@GetMapping
	fun list(
		@Valid @ModelAttribute request: AdminReviewSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminReviewQueryService.searchReviews(request))
}

package app.bottlenote.alcohols.persentaton

import app.bottlenote.alcohols.domain.TastingTagRepository
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/tasting-tags")
class AdminTastingTagController(
	private val tastingTagRepository: TastingTagRepository
) {

	@GetMapping
	fun getAllTastingTags(): ResponseEntity<*> {
		return ResponseEntity.ok(GlobalResponse.ok(tastingTagRepository.findAllTastingTags()))
	}
}

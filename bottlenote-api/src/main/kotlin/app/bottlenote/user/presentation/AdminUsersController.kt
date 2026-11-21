package app.bottlenote.user.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.user.dto.request.AdminUserSearchRequest
import app.bottlenote.user.presentation.docs.AdminUsersApiDocs
import app.bottlenote.user.service.AdminUserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@AdminUsersApiDocs.ApiTag
class AdminUsersController(
	private val adminUserService: AdminUserService
) {
	@AdminUsersApiDocs.ListUsers
	@GetMapping
	fun list(
		@ModelAttribute request: AdminUserSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminUserService.searchUsers(request))
}

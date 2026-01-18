package app.bottlenote.common.file.presentation

import app.bottlenote.common.file.dto.request.ImageUploadRequest
import app.bottlenote.common.file.service.ImageUploadService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/s3")
class AdminImageUploadController(
	private val imageUploadService: ImageUploadService
) {

	@GetMapping("/presign-url")
	fun getPreSignUrl(@ModelAttribute request: ImageUploadRequest): ResponseEntity<*> {
		val adminId = SecurityContextUtil.getAdminUserIdByContext()
			.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
		return GlobalResponse.ok(imageUploadService.getPreSignUrlForAdmin(adminId, request))
	}
}

package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.global.data.response.GlobalResponse
import app.global.admin.AdminApi
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException

// product 오류 응답을 바꾸지 않도록 이식한 admin 컨트롤러에만 적용한다.
// handler가 정해지기 전에 난 admin 경로의 multipart 예외는 AdminMultipartFailureResolver가 이 처리기로 응답한다.
@RestControllerAdvice(
	basePackages = [
		AdminApi.ACCESS_CONTROL_PACKAGE,
		AdminApi.ALCOHOLS_PACKAGE,
		AdminApi.AUTH_PACKAGE,
		AdminApi.BANNER_PACKAGE,
		AdminApi.CAMPAIGN_CONTENT_PACKAGE,
		AdminApi.FILE_PACKAGE,
		AdminApi.CURATION_PACKAGE,
		AdminApi.MFDS_PACKAGE,
		AdminApi.REVIEW_PACKAGE,
		AdminApi.STATISTICS_PACKAGE,
		AdminApi.HELP_PACKAGE,
		AdminApi.USER_PACKAGE
	]
)
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminAlcoholExcelExceptionHandler {
	@ExceptionHandler(MaxUploadSizeExceededException::class)
	fun handleSizeLimit(): ResponseEntity<GlobalResponse> = GlobalResponse.error(AlcoholException(AlcoholExceptionCode.EXCEL_FILE_TOO_LARGE))

	@ExceptionHandler(MultipartException::class, MissingServletRequestPartException::class)
	fun handleInvalidFile(): ResponseEntity<GlobalResponse> = GlobalResponse.error(AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE))
}

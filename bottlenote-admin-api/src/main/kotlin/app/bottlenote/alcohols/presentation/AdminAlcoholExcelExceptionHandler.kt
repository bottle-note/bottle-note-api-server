package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminAlcoholExcelExceptionHandler {
	@ExceptionHandler(MaxUploadSizeExceededException::class)
	fun handleSizeLimit(): ResponseEntity<GlobalResponse> = GlobalResponse.error(AlcoholException(AlcoholExceptionCode.EXCEL_FILE_TOO_LARGE))

	@ExceptionHandler(MultipartException::class, MissingServletRequestPartException::class)
	fun handleInvalidFile(): ResponseEntity<GlobalResponse> = GlobalResponse.error(AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE))
}

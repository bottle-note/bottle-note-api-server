package app.global.admin

import app.bottlenote.alcohols.presentation.AdminAlcoholExcelExceptionHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

// admin 엑셀 예외 처리기를 admin 컨트롤러로 좁히면서 생긴 빈틈을 메운다.
// multipart는 DispatcherServlet이 handler를 찾기 전에 파싱하므로, 크기 초과나 파싱 실패는 handler 없이 예외 처리로 넘어온다.
// 범위를 좁힌 @RestControllerAdvice는 handler가 없으면 적용되지 않는다. 기존 admin 앱에서는 이 경우도 엑셀 처리기가 응답했으므로
// admin 경로에서 handler 없이 난 multipart 예외만 같은 처리기로 응답한다. product 경로는 건드리지 않아 기존 전역 처리기가 그대로 응답한다.
@Component
class AdminMultipartFailureResolver(
	private val excelExceptionHandler: AdminAlcoholExcelExceptionHandler,
	private val jsonConverter: MappingJackson2HttpMessageConverter
) : HandlerExceptionResolver,
	Ordered {

	override fun resolveException(
		request: HttpServletRequest,
		response: HttpServletResponse,
		handler: Any?,
		ex: Exception
	): ModelAndView? {
		if (handler != null || !AdminApi.isAdminApiRequest(request)) {
			return null
		}
		val entity = when (ex) {
			is MaxUploadSizeExceededException -> excelExceptionHandler.handleSizeLimit()
			is MultipartException, is MissingServletRequestPartException -> excelExceptionHandler.handleInvalidFile()
			else -> return null
		}
		val output = ServletServerHttpResponse(response)
		output.setStatusCode(entity.statusCode)
		jsonConverter.write(entity.body!!, MediaType.APPLICATION_JSON, output)
		return ModelAndView()
	}

	// DefaultErrorAttributes(HIGHEST_PRECEDENCE)는 예외를 기록만 하고 넘기므로 그 바로 뒤, @ExceptionHandler 처리보다 앞에 둔다.
	override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1
}

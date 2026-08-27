package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelValidateResponse
import org.springframework.web.multipart.MultipartFile

interface AdminAlcoholExcelService {
	fun createTemplateWorkbook(): ByteArray

	fun validate(file: MultipartFile): AdminAlcoholExcelValidateResponse
}

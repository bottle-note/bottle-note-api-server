package app.bottlenote.common.file.presentation.docs

import app.bottlenote.common.file.dto.response.ImageUploadResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 이미지 업로드 엔드포인트의 문서 설명. */
object AdminImageUploadApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "이미지 업로드", description = "S3에 직접 업로드할 수 있는 presigned URL을 발급한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "이미지 업로드용 presigned URL을 발급한다",
		description = """
			rootPath 하위에 uploadSize 개수만큼 S3 업로드용 presigned URL을 발급합니다.

			uploadSize를 지정하지 않으면 1장, contentType을 지정하지 않으면 image/jpeg가 기본값입니다.
			발급된 URL은 유효 기간(expiryTime, 분 단위) 동안만 PUT 업로드에 사용할 수 있으며,
			요청 시점에 관리자 계정으로 업로드 이력이 함께 기록됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "발급된 presigned URL 목록",
				content = [Content(schema = Schema(implementation = ImageUploadResponse::class))]
			)
		]
	)
	annotation class GetPreSignUrl
}

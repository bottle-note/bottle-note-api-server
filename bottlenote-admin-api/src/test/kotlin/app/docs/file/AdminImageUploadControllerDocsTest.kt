package app.docs.file

import app.bottlenote.common.file.dto.request.ImageUploadRequest
import app.bottlenote.common.file.dto.response.ImageUploadItem
import app.bottlenote.common.file.dto.response.ImageUploadResponse
import app.bottlenote.common.file.presentation.AdminImageUploadController
import app.bottlenote.common.file.service.ImageUploadService
import app.bottlenote.global.security.SecurityContextUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.util.*

@WebMvcTest(
	controllers = [AdminImageUploadController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin 이미지 업로드 컨트롤러 RestDocs 테스트")
class AdminImageUploadControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@MockitoBean
	private lateinit var imageUploadService: ImageUploadService

	@Test
	@DisplayName("PreSigned URL 발급")
	fun getPreSignUrl() {
		// given
		val imageUploadInfo = listOf(
			ImageUploadItem.builder()
				.order(1L)
				.viewUrl("https://cdn.example.com/admin/banner/uuid-1.jpg")
				.uploadUrl("https://s3.ap-northeast-2.amazonaws.com/bucket/admin/banner/uuid-1.jpg?X-Amz-Algorithm=...")
				.build(),
			ImageUploadItem.builder()
				.order(2L)
				.viewUrl("https://cdn.example.com/admin/banner/uuid-2.jpg")
				.uploadUrl("https://s3.ap-northeast-2.amazonaws.com/bucket/admin/banner/uuid-2.jpg?X-Amz-Algorithm=...")
				.build()
		)

		val response = ImageUploadResponse.builder()
			.bucketName("bottlenote-bucket")
			.expiryTime(5)
			.uploadSize(2)
			.imageUploadInfo(imageUploadInfo)
			.build()

		given(imageUploadService.getPreSignUrlForAdmin(anyLong(), any(ImageUploadRequest::class.java)))
			.willReturn(response)

		Mockito.mockStatic(SecurityContextUtil::class.java).use { mockedStatic: MockedStatic<SecurityContextUtil> ->
			mockedStatic.`when`<Optional<Long>> { SecurityContextUtil.getAdminUserIdByContext() }
				.thenReturn(Optional.of(1L))

			// when & then
			assertThat(
				mvc.get().uri("/s3/presign-url")
					.header("Authorization", "Bearer test_access_token")
					.param("rootPath", "admin/banner")
					.param("uploadSize", "2")
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/file/presign-url",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						requestHeaders(
							headerWithName("Authorization").description("Bearer 액세스 토큰")
						),
						queryParameters(
							parameterWithName("rootPath").description("업로드 경로 (예: admin/banner, admin/alcohol)"),
							parameterWithName("uploadSize").description("발급할 URL 개수")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data.bucketName").type(JsonFieldType.STRING).description("S3 버킷명"),
							fieldWithPath("data.expiryTime").type(JsonFieldType.NUMBER).description("URL 만료 시간 (분)"),
							fieldWithPath("data.uploadSize").type(JsonFieldType.NUMBER).description("발급된 URL 개수"),
							fieldWithPath("data.imageUploadInfo[].order").type(JsonFieldType.NUMBER).description("이미지 순서"),
							fieldWithPath("data.imageUploadInfo[].viewUrl").type(JsonFieldType.STRING).description("이미지 조회 URL (CDN)"),
							fieldWithPath("data.imageUploadInfo[].uploadUrl").type(JsonFieldType.STRING).description("이미지 업로드 URL (PreSigned)"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}
}

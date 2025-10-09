package app

import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/admin/api/v1")
class HelloAdminApiController {

	@GetMapping("/hello")
	fun hello(): GlobalResponse {
		val data = HashMap<String, String>()
		data["message"] = "Hello World!"
		return GlobalResponse.success(data)
	}
}

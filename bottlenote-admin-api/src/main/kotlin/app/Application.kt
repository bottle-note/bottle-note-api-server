package app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import java.util.*

@EntityScan(basePackages = ["app"])
@SpringBootApplication(scanBasePackages = ["app"])
class AdminApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    runApplication<AdminApplication>(*args)
}

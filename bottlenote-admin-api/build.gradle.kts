// bottlenote-admin-api
plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.asciidoctor)
	alias(libs.plugins.restdocs.api.spec)
}
// snippetsDir 정의
val snippetsDir by extra { file("build/generated-snippets") }
val asciidoctorExt: Configuration by configurations.creating

dependencies {
	implementation(project(":bottlenote-mono"))
	testImplementation(project(":bottlenote-mono").dependencyProject.sourceSets["test"].output)

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Spring Data
	implementation(libs.spring.boot.starter.data.jpa)

	// Security
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.security.test)
	testImplementation(libs.spring.security.test)

	testImplementation(libs.spring.boot.starter.data.jpa)
	testImplementation(libs.mysql.connector.j)

	// Test - Spring REST Docs
	add("asciidoctorExt", libs.spring.restdocs.asciidoctor)
	testImplementation(libs.spring.restdocs.mockmvc)
	testImplementation(libs.restdocs.api.spec.mockmvc)

	// Test - Testcontainers
	testImplementation(libs.bundles.testcontainers.complete)

	// Test - AWS S3 (for MinIO integration test)
	testImplementation(libs.aws.s3)
}

sourceSets {
	main {
		resources {
			srcDirs("src/main/resources", "${rootProject.projectDir}/git.environment-variables")
		}
	}
	test {
		resources {
			srcDirs("src/test/resources", "${rootProject.projectDir}/git.environment-variables")
		}
	}
}

tasks.processResources {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processTestResources {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.bootJar {
	enabled = true
	archiveFileName.set("bottlenote-admin-api.jar")
	logger.info("Building bottlenote-admin-api.jar")
}

tasks.jar {
	enabled = true
}

tasks.asciidoctor {
	inputs.dir(snippetsDir)
	configurations(asciidoctorExt.name)
	dependsOn(tasks.test)
	sources {
		include("**/admin-api.adoc")
	}
	baseDirFollowsSourceFile()
}
tasks.register("prepareKotlinBuildScriptModel") {}

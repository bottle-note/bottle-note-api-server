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
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Test - Spring REST Docs
	add("asciidoctorExt", libs.spring.restdocs.asciidoctor)
	testImplementation(libs.spring.restdocs.mockmvc)
	testImplementation(libs.restdocs.api.spec.mockmvc)
}

tasks.bootJar {
	enabled = true
	archiveFileName.set("bottlenote-admin-api.jar")
}

tasks.jar {
	enabled = true
}

tasks.asciidoctor {
	inputs.dir(snippetsDir)
	configurations(asciidoctorExt.name)
	dependsOn(tasks.test)
	setSourceDir(file("src/docs"))
	sources {
		include("**/admin-api.adoc")
	}
	baseDirFollowsSourceFile()
}

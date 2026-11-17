// bottlenote-admin-api
plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	implementation(project(":bottlenote-user"))
	implementation(project(":bottlenote-storage"))
	implementation(project(":bottlenote-community"))
	implementation(project(":bottlenote-history"))
	implementation(project(":bottlenote-support"))
	implementation(project(":bottlenote-notification"))
	implementation(project(":bottlenote-analytics"))
	implementation(project(":bottlenote-content"))
	implementation(project(":bottlenote-access-control"))
	testImplementation(testFixtures(project(":bottlenote-user")))
	testImplementation(testFixtures(project(":bottlenote-community")))
	testImplementation(testFixtures(project(":bottlenote-content")))
	testImplementation(testFixtures(project(":bottlenote-history")))
	testImplementation(testFixtures(project(":bottlenote-storage")))
	testImplementation(testFixtures(project(":bottlenote-support")))
	testImplementation(testFixtures(project(":bottlenote-access-control")))
	testImplementation(testFixtures(project(":bottlenote-analytics")))
	implementation(libs.spring.boot.starter.data.redis)
	implementation(libs.hypersistence.utils)
	implementation(libs.spring.cloud.starter.openfeign)
	implementation(libs.micrometer.core)
	implementation(project(":bottlenote-foundation"))
	implementation(project(":bottlenote-alcohol"))
	implementation(libs.querydsl.jpa)
	testImplementation(testFixtures(project(":bottlenote-alcohol")))
	testImplementation(project(":bottlenote-test-support"))

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// mono entity(BaseTimeEntity→AbstractAggregateRoot) 접근에 필요
	implementation(libs.spring.boot.starter.data.jpa)
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// OpenAPI
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	// XLSX (Admin module only)
	implementation("org.apache.poi:poi-ooxml:5.3.0")

	// Security
	implementation(libs.spring.boot.starter.security)
	testImplementation(libs.spring.security.test)

	testImplementation(libs.spring.boot.starter.data.jpa)
	testImplementation(libs.mysql.connector.j)

	// Test - Architecture rules
	testImplementation(libs.archunit)

	// Schema migration
	runtimeOnly(libs.flyway.core)
	runtimeOnly(libs.flyway.mysql)

	// Test - Testcontainers
	testImplementation(libs.bundles.testcontainers.complete)

	// Test - AWS S3 (for MinIO integration test)
	testImplementation(platform(libs.aws.sdk.bom))
	testImplementation(libs.aws.sdk.s3)
}

sourceSets {
	main {
		resources {
			srcDirs("src/main/resources")
		}
	}
	test {
		resources {
			srcDirs("src/test/resources")
		}
	}
}

tasks.processResources {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	val migrationDir = file("${rootProject.projectDir}/git.environment-variables/storage/db/migration")
	doFirst {
		val sqls = fileTree(migrationDir) { include("*.sql") }.files
		if (sqls.isEmpty()) {
			throw GradleException(
				"Flyway SQL missing under git.environment-variables/storage/db/migration. Run: git submodule update --init --recursive"
			)
		}
	}
	from(migrationDir) {
		include("*.sql")
		into("db/migration")
	}
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

tasks.register("prepareKotlinBuildScriptModel") {}

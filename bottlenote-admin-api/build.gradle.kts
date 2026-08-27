// bottlenote-admin-api
plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	implementation(project(":bottlenote-mono"))
	testImplementation(project(":bottlenote-test-support"))

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// OpenAPI
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	// XLSX (Admin module only)
	implementation("org.apache.poi:poi-ooxml:5.3.0")

	// Security
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.security.test)
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

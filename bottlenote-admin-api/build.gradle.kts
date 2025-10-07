plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":bottlenote-mono"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

tasks.bootJar {
    enabled = true
    archiveFileName.set("bottlenote-admin-api.jar")
}

tasks.jar {
    enabled = true
}

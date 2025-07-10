plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // 모든 하위 모듈 의존성
    implementation(project(":modules:global-utils:common-utils"))
    implementation(project(":modules:domain:weather-domain"))
    implementation(project(":modules:domain:user-domain"))
    implementation(project(":modules:infrastructure:weather-client"))
    implementation(project(":modules:infrastructure:geocoding-client"))
    implementation(project(":modules:infrastructure:slack-client"))

    // Spring Boot & Batch
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database
    runtimeOnly("com.h2database:h2:${rootProject.extra["h2Version"]}")
    runtimeOnly("com.mysql:mysql-connector-j:${rootProject.extra["mysqlVersion"]}")

    // Batch Test
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 실행 가능한 애플리케이션
tasks.bootJar {
    enabled = true
    archiveClassifier = ""
}

tasks.jar {
    enabled = false
}

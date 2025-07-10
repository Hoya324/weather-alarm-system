plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("org.springframework.boot") version "3.5.3" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.weather.alarm"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}

extra["springBootVersion"] = "3.5.3"
extra["springBatchVersion"] = "5.1.2"
extra["h2Version"] = "2.2.224"
extra["mysqlVersion"] = "8.4.0"

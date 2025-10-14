plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.deliveranything"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:2.26.19")
    }

    dependencies {
        dependency("co.elastic.clients:elasticsearch-java:8.18.5")
        dependency("org.elasticsearch.client:elasticsearch-rest-client:8.18.5")
    }
}

dependencies {
    // --- Spring Boot Starter ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // --- Database ---
    runtimeOnly("com.h2database:h2")               // 개발용
    runtimeOnly("com.mysql:mysql-connector-j")     // MySQL

    // --- Redis & Cache ---
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310") // 날짜 직렬화

    // --- JWT ---
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // --- OpenAPI 문서 ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // --- Lombok ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // --- Jakarta 표준 어노테이션 ---
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // --- Dotenv ---
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // --- QueryDSL ---
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.0")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.0:jakarta")

    // --- Spatial (for Geolocation) ---
    implementation("org.hibernate.orm:hibernate-spatial:6.6.26.Final")
    implementation("org.locationtech.jts:jts-core:1.19.0")

    // --- WebSocket ---
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // --- WebFlux ---
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // --- Kafka ---
    implementation("org.springframework.kafka:spring-kafka")

    // --- User-Agent 파싱 ---
    implementation("eu.bitwalker:UserAgentUtils:1.21")

    // --- AWS ---
    implementation("software.amazon.awssdk:s3")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
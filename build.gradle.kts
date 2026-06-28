import com.epages.restdocs.apispec.gradle.OpenApi3Extension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.lang.Closure
import io.swagger.v3.oas.models.servers.Server
import org.gradle.kotlin.dsl.closureOf

plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.4"
}

group = "cmc"
version = "0.0.1-SNAPSHOT"
description = "mody"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.2")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    // API 문서 (Swagger UI). springdoc 2.x = Spring Boot 3.x 지원
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    runtimeOnly("com.mysql:mysql-connector-j")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.4")
    testCompileOnly("org.projectlombok:lombok")
    developmentOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
    outputs.dir(layout.buildDirectory.dir("generated-snippets"))
}

configure<OpenApi3Extension> {
    @Suppress("UNCHECKED_CAST")
    setServers(listOf<Closure<Server>>(
        closureOf<Server> {
            url = "https://dev-mody.store"
            description = "Dev server"
        } as Closure<Server>,
        closureOf<Server> {
            url = "http://localhost:8080"
            description = "Local server"
        } as Closure<Server>
    ))
    title = "mody API"
    description = "친구들과 함께하는 다이어트 습관, 모디 mody 백엔드 API 명세"
    version = "v1"
    format = "json"
}

afterEvaluate {
    tasks.named("openapi3") {
        dependsOn(tasks.test)
    }
}

val applyOpenApiSecurity by tasks.registering {
    dependsOn("openapi3")
    outputs.upToDateWhen { false }

    doLast {
        val specFile = layout.buildDirectory.file("api-spec/openapi3.json").get().asFile
        @Suppress("UNCHECKED_CAST")
        val spec = JsonSlurper().parse(specFile) as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val components = spec.getOrPut("components") { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val securitySchemes = components.getOrPut("securitySchemes") {
            linkedMapOf<String, Any?>()
        } as MutableMap<String, Any?>

        securitySchemes["bearerAuth"] = linkedMapOf(
            "type" to "http",
            "scheme" to "bearer",
            "bearerFormat" to "JWT"
        )
        spec["security"] = listOf(mapOf("bearerAuth" to emptyList<String>()))

        specFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(spec)))
    }
}

val copyOpenApiSpec by tasks.registering(Copy::class) {
    dependsOn(applyOpenApiSecurity)
    from(layout.buildDirectory.file("api-spec/openapi3.json"))
    into(layout.buildDirectory.dir("resources/main/static/docs"))
}

tasks.named("bootJar") {
    dependsOn(copyOpenApiSpec)
}

tasks.named("jar") {
    dependsOn(copyOpenApiSpec)
}

tasks.named("resolveMainClassName") {
    dependsOn(copyOpenApiSpec)
}

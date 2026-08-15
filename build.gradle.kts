plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.contextstt"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.caffeine)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.webmvc)
	compileOnly(libs.lombok)
	runtimeOnly(libs.flyway.mysql)
	runtimeOnly(libs.mysql.connector)
	annotationProcessor(libs.lombok)

	implementation(libs.jjwt.api)
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.jackson)

	implementation(libs.springdoc.openapi.starter.webmvc.ui)
	testImplementation(libs.spring.boot.starter.data.jpa.test)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(libs.spring.boot.starter.validation.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testRuntimeOnly(libs.h2)
	testCompileOnly(libs.lombok)
	testRuntimeOnly(libs.junit.platform.launcher)
	testAnnotationProcessor(libs.lombok)
}

tasks.test {
	useJUnitPlatform()
}
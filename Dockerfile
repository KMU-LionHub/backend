FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon \
	&& find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" \
		-exec cp {} app.jar \;

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
	&& useradd --system --gid spring spring

COPY --from=builder --chown=spring:spring /workspace/app.jar ./app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
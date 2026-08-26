FROM gradle:jdk25 AS builder

WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN gradle :chatroom:bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /workspace/chatroom/build/libs/chatroom.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

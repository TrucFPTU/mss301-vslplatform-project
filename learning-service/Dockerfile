# Build jar truoc: mvn -DskipTests package
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/learning-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
# Giờ Việt Nam như monolith: streak / lịch nhắc / thống kê theo ngày tính đúng ngày của người dùng.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]

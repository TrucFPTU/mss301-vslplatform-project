# ⚠️ KHONG dung -alpine: JavaCV/FFmpeg/OpenCV can glibc, ban musl (alpine) se loi native lib.
# Build jar truoc: mvn -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ai-service-0.0.1-SNAPSHOT.jar app.jar
# Model KHONG dong goi vao image (~135MB) — mount qua volume trong docker-compose:
#   ./models  ->  /app/models
EXPOSE 8083
# Giờ Việt Nam như monolith: streak / lịch nhắc / thống kê theo ngày tính đúng ngày của người dùng.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]

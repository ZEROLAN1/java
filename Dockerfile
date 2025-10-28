# 构建阶段
# 使用阿里云镜像（如果 Docker Hub 访问困难）
FROM maven:3.8-openjdk-11 as build

WORKDIR /app

# 配置阿里云 Maven 镜像加速
COPY pom.xml .
RUN mvn dependency:go-offline -s /usr/share/maven/conf/settings.xml \
    || (echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><name>aliyun maven</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /tmp/settings.xml && mvn dependency:go-offline -s /tmp/settings.xml)

COPY src ./src
RUN mvn clean package -DskipTests -s /tmp/settings.xml || mvn clean package -DskipTests

# 运行阶段
FROM openjdk:11-jre-slim

WORKDIR /app

COPY --from=build /app/target/cloud-storage-0.0.1-SNAPSHOT.jar app.jar

# Create upload directory
RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

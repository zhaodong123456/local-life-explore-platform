# ============================================================
# 本地生活探店平台 - 后端 Dockerfile（多阶段构建）
# 阶段1：Maven 编译打包；阶段2：JRE 精简镜像运行
# ============================================================

# ---------- 阶段1：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 配置 Maven 阿里云镜像加速（国内网络拉依赖快很多；海外环境可删除此步骤）
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\
 xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">\
  <mirrors>\
    <mirror>\
      <id>aliyunmaven</id>\
      <mirrorOf>central</mirrorOf>\
      <name>阿里云中央仓库</name>\
      <url>https://maven.aliyun.com/repository/central</url>\
    </mirror>\
  </mirrors>\
</settings>' > /root/.m2/settings.xml

# 先拷贝 pom.xml 单独拉依赖，利用 Docker 层缓存（pom 不变则依赖层不重下）
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 拷贝源码并打包（跳过测试，构建产物 target/explore-platform-1.0.0.jar）
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- 阶段2：运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区与容器内时间设置（涉及订单/秒杀时间判断，统一东八区）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 只拷贝最终可执行 jar（pom 未配置 finalName，默认 artifactId-version 命名）
COPY --from=build /app/target/explore-platform-1.0.0.jar app.jar

EXPOSE 8080

# 连接配置通过 docker-compose 的 SPRING_* 环境变量注入，此处无需写死
ENTRYPOINT ["java", "-jar", "app.jar"]

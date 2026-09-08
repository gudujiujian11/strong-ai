FROM harbor.leke.cn/basic/openjdk:8-jdk-alpine as builder
ARG APP_NAME
MAINTAINER wangfeng1 "wangfeng1@cnstrong.com"
WORKDIR application
COPY ${APP_NAME}-webapp/target/${APP_NAME}.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


FROM harbor.leke.cn/basic/busybox:latest
MAINTAINER wangfeng1 "wangfeng1@cnstrong.com"
WORKDIR application
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/strong-lib-dependencies/ ./
COPY --from=builder /application/strong-app-dependencies/ ./
COPY --from=builder /application/application/ ./

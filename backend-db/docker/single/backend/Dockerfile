FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 首次启动 bootstrap（entrypoint 会拷到 volume）
COPY app.jar /bootstrap/app.jar
COPY version.json /bootstrap/version.json
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh \
    && mkdir -p /opt/scaffold

ENV APP_RELEASE_HOME=/opt/scaffold \
    JAVA_OPTS="-Xms512m -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:+ParallelRefProcEnabled \
    -XX:+UseStringDeduplication \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=80.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=Asia/Shanghai \
    -Dfile.encoding=UTF-8"

EXPOSE 18080

ENTRYPOINT ["/app/docker-entrypoint.sh"]

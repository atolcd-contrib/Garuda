ARG JAVA_IMAGE_NAME=eclipse-temurin
ARG JAVA_IMAGE_VERSION=11-jre
FROM ${JAVA_IMAGE_NAME}:${JAVA_IMAGE_VERSION}

WORKDIR /opt/garuda

COPY docker/garuda/docker-entrypoint.sh ./
COPY public/ classes/public/
COPY target/pack/lib/ lib/
COPY sdk.properties sdk.properties

# Creates a default non-root user
# Ensure that all files are readable regardless of user at runtime and that garuda is owner of its files
RUN groupadd --system --non-unique --gid 1000 garuda \
 && useradd --no-log-init --system --no-create-home --home /opt/garuda --non-unique --uid 1000 --gid garuda garuda \
 && chmod -R a+rX . && chown -R garuda:garuda .

EXPOSE 9000

USER garuda
ENV GC_OPTS="-XX:+DisableExplicitGC"
ENV JAVA_OPTS=""
ENV JAVA_MEM_OPTS="-XX:MaxRAMPercentage=75"
ENV GARUDA_OPTS="-Dslick.dbs.default.profile=slick.jdbc.PostgresProfile$ -Dslick.dbs.default.db.driver=org.postgresql.Driver"
ENTRYPOINT [ "/opt/garuda/docker-entrypoint.sh" ]

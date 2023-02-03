# Garuda packaging

Commands are executed in the project's root directory.

## Preriquisites

You only need [docker engine 19.03.0+](https://docs.docker.com/get-docker/) and [docker-compose 1.27.0+](https://docs.docker.com/compose/install/).

## Package `jar` app

Build application and dependencies' jars:
```sh
# Directories created before to avoid root owner at auto creation by bind mount with a non root container
mkdir -p ${HOME}/{.cache,.ivy2,.sbt} &&\
docker run -it --rm --name garuda-pack \
  --network host \
  `# non root and cache` \
  -u $(id -u):$(id -g) \
  -v /etc/passwd:/etc/passwd:ro \
  -v "${HOME}/.cache":"${HOME}/.cache" \
  -v "${HOME}/.ivy2":"${HOME}/.ivy2" \
  -v "${HOME}/.sbt":"${HOME}/.sbt" \
  `# application` \
  -v "$(pwd)":/app -w /app \
  sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.8.2_2.13.10 \
  sbt clean pack
```

You can found application and dependencies' jars in `target/pack/lib`.

**Example execution with java 11** in path and `garuda-postgres-1` started:
```sh
export GARUDA_OPTS=(-Dslick.dbs.default.profile="slick.jdbc.PostgresProfile$" -Dslick.dbs.default.db.driver="org.postgresql.Driver" -Dslick.dbs.default.db.url="jdbc:postgresql://localhost:5432/garudadef?user=garudadef&password=garudadef")

# Note : sdk.properties file and public directory are needed in classpath
java $GARUDA_OPTS -cp ".:target/pack/lib/*" play.core.server.ProdServerStart
```

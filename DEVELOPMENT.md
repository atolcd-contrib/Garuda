# Garuda development

Commands are executed in the project's root directory.

## Preriquisites

You only need [docker engine 19.03.0+](https://docs.docker.com/get-docker/) and [docker-compose 1.27.0+](https://docs.docker.com/compose/install/).

## Dependencies

Cf. [docker/README.md]().

```sh
(cd docker && docker-compose up --force-recreate -d)
```

## Compile and run

Run the following commands:
```sh
# Cf. https://hub.docker.com/r/sbtscala/scala-sbt
# Cf. https://github.com/sbt/docker-sbt
# tag: eclipse-temurin-jammy-<JDK version>_<sbt version>_<Scala version>

# Directories are prevently created to avoid root owner at auto creation by bind mount with a non root container
mkdir -p ${HOME}/{.cache,.ivy2,.sbt} &&\
docker run -it --rm --name garuda \
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
  sbt clean compile run
```

Wait for server to run and open [localhost:9000]()

💡 At this step, schema tables are created in H2 database.

You can follow instructions of [README.md]() to set up a collect.

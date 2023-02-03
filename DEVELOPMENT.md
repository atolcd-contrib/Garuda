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

# Options to use PostgreSQL's definition database
export GARUDA_OPTS=(-Dslick.dbs.default.profile="slick.jdbc.PostgresProfile$" -Dslick.dbs.default.db.driver="org.postgresql.Driver" -Dslick.dbs.default.db.url="jdbc:postgresql://localhost:5432/garudadef?user=garudadef&password=garudadef")

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
  sbt clean compile run ${GARUDA_OPTS}
```

Wait for server to run and open [localhost:9000]()

💡 At this step, schema tables are created in `garudadef` database.

To set up a collect, you can follow instructions:
* of [README.md]()
* of the next paragraph

## Insert a dataset

Next, we insert a minimal dataset with a rule.

⚠ At least think about defining `GARUDA_ACCOUNT_TYPE`, `GARUDA_ACCOUNT_NAME`, `GARUDA_ACCOUNT_BEARER_TOKEN` and `GARUDA_RULE_CONTENT` before execution:

```sh
export GARUDA_DIRECTORY="Collects"

# Essential, Elevated, Academic
export GARUDA_ACCOUNT_TYPE="${GARUDA_ACCOUNT_TYPE:-Essential}"
export GARUDA_ACCOUNT_NAME="${GARUDA_ACCOUNT_NAME:-SetIt}"
export GARUDA_ACCOUNT_BEARER_TOKEN="${GARUDA_ACCOUNT_BEARER_TOKEN:-SetIt}"

export GARUDA_COLLECT_NAME="collect1"
export GARUDA_PG_HOST="localhost"
export GARUDA_PG_PORT="5432"
export GARUDA_PG_BASE="garuda"
export GARUDA_PG_SCHEMA="${GARUDA_PG_SCHEMA:-garuda}"
export GARUDA_PG_USER="garuda"
export GARUDA_PG_PASSWORD="garuda"

# ⚠ tag label and content rule
export GARUDA_RULE_TAG="My tag label to identify which rules have matched"
# See https://developer.twitter.com/en/docs/twitter-api/tweets/filtered-stream/integrate/build-a-rule
export GARUDA_RULE_CONTENT="${GARUDA_RULE_CONTENT:-Garuda AND (Kashyapa OR Vinatâ)}"

docker exec -it garuda-postgres-1 sh -c "psql -U garudadef -d garudadef <<EOF

BEGIN;

INSERT INTO PUBLIC."account"
("name", "type", "bearer_token")
VALUES('${GARUDA_ACCOUNT_NAME}', '${GARUDA_ACCOUNT_TYPE}', '${GARUDA_ACCOUNT_BEARER_TOKEN}');

INSERT INTO PUBLIC."collect"
("name", "directory", "account")
VALUES('${GARUDA_COLLECT_NAME}', '${GARUDA_DIRECTORY}/${GARUDA_COLLECT_NAME}', '${GARUDA_ACCOUNT_NAME}');

INSERT INTO PUBLIC."postgres_configuration"
("collect", "host", "port", "base", "schema_", "user_", "password")
VALUES('${GARUDA_COLLECT_NAME}', '${GARUDA_PG_HOST}', ${GARUDA_PG_PORT}, '${GARUDA_PG_BASE}', '${GARUDA_PG_SCHEMA}', '${GARUDA_PG_USER}', '${GARUDA_PG_PASSWORD}');

INSERT INTO PUBLIC."temporary_rule"
("tag", "content", "collect")
VALUES('${GARUDA_RULE_TAG}', '${GARUDA_RULE_CONTENT}', '${GARUDA_COLLECT_NAME}');

COMMIT;
EOF"
```

In the collect page [localhost:9000/collects/collect1]():
- Remove rules of the account that are registered at Twitter but that are not part of the collect:
  - Click `Select all` and `Remove all selected rules`
- Select rule, switch to active with `<<` and click `Affect rules`
- Click `Start collect`

💡 It begins to collect data from twitter to files in `Collects/collect1/***` directory.

Click `Modules` to access to the modules page of the collect [localhost:9000/modules/collect1]():
- Click `Start module`

💡 It begins to insert data from file in `Collects/collect1` to PostgreSQL database.

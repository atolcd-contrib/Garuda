# Garuda - Docker

Commands are executed in the `docker` directory.

## Starting containers

```sh
docker compose up --force-recreate -d
```

## Cleaning

```sh
# Stopping and deleting containers including orphans, networks and volumes
docker compose down --remove-orphans --volumes

# Delete images created for the project
docker rmi $(docker images --filter=reference='garuda-*' -q)

# Complete cleaning, local images included
docker compose down --remove-orphans --volumes --rmi local
```

## Instantiated dependencies

### PostgreSQL Server
`garuda-postgres-1` container exposes a **PostgreSQL server** that listens on port `5432`.

One database is created :
* `garuda` : target database which will receive tweets, etc.

```sh
# SQL request example
docker exec -it garuda-postgres-1 psql -U garuda -c "select version()"

# Multiline SQL request example
docker exec -it garuda-postgres-1 sh -c 'psql -U garuda  <<EOF
select version()
EOF'
```

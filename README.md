# solabot

设计仅为重庆大学烤群使用的 bot。

# docker

```bash
# Start containers (and build if not exists) in detached mode
docker compose -f docker-compose.dev.yaml up -d

# Enter the bash interactive shell of the PostgreSQL container
docker exec -it dev-postgres bash
psql -U app -d appdb

# Enter the sh interactive shell of the Redis container
docker exec -it dev-redis sh
redis-cli

# Shutdown all containers
docker stop $(docker ps -q)
```

# maven

```bash
mvn clean package
mvn spring-boot:run  # java -jar target/bot-0.0.1-SNAPSHOT.jar
```

# version

OpenJDK 17+

Spring Boot 3.5

Docker 27.5

PostgreSQL 16

Redis 7

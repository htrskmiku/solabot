# solabot

仅为重庆大学 PJSK 同好群「翼遥啤酒烧烤大排档」设计的 bot 后端。

# version

OpenJDK 17+

Spring Boot 3.5

Spring WebFlux 3.5 (WebClient only)

Spring Cloud (in the planning)（计划首先用于组卡器微服务）

Kafka (in the planning)（计划首先用于组卡器微服务）

k8s (in the planning)（计划首先用于组卡器微服务）

Docker 27.5

PostgreSQL 16

Redis 7

NapCat（OneBot v11）

# docker

```bash
# Start containers (and build if not exists) in detached mode before running the project
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

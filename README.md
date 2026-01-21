# gluetun-qbittorrent-portforward

Small Spring Boot service to keep Gluetun and qBittorrent port-forwarding in sync.

## Description

`gluetun-qbittorrent-portforward` is a lightweight scheduler service that periodically queries a Gluetun container and a qBittorrent instance, and synchronizes port forwarding information between them. The application is implemented as a Spring Boot app and includes a `Dockerfile` and `compose.yaml` for containerized deployment.

## Prerequisites

- JDK 21
- Maven
- Docker (if building/running a container)
- docker-compose (optional, for `compose.yaml`)

## Build (local)

1. Compile and package the application with Maven:

```bash
mvn -DskipTests package
```

2. Run the packaged JAR locally:

```bash
java -jar target/gluetun-qbittorrent-portforward-0.0.1-SNAPSHOT.jar
```

Or use Maven to run directly:

```bash
mvn spring-boot:run
```

## Docker image (build & run)

Build the image using the included `Dockerfile`:

```bash
docker build -t tezzad17/gluetun-qbittorrent-portforward:latest .
```

Run the container (example):

```bash
docker run -d --name port-sync \
	-e APP_GLUETUN_BASE_URL=http://10.25.25.32:8000 \
	-e APP_QBITTORRENT_BASE_URL=http://10.25.25.32:8081 \
	-e APP_SYNC_FREQUENCY_MS=300000 \
	-v /home/tezzad/log:/home/tezzad/log \
	tezzad17/gluetun-qbittorrent-portforward:latest
```

Notes:
- The application writes logs to `/home/tezzad/log/port-protonvpn.log` by default (see `application.properties`). Mount a host directory to `/home/tezzad/log` in the container to persist logs.

## Docker Compose

A minimal `compose.yaml` is provided. Start the service with:

```bash
docker compose -f compose.yaml up -d
```

The supplied `compose.yaml` demonstrates setting environment variables for the service.

## Configuration

The application reads configuration from `application.properties` and supports overriding via environment variables (Spring Boot relaxed binding). Important properties:

- `app.gluetun.container-name` (env: `APP_GLUETUN_CONTAINER_NAME`) — Gluetun container name
- `app.gluetun.base-url` (env: `APP_GLUETUN_BASE_URL`) — Gluetun web API base URL
- `app.qbittorrent.container-name` (env: `APP_QBITTORRENT_CONTAINER_NAME`) — qBittorrent container name
- `app.qbittorrent.base-url` (env: `APP_QBITTORRENT_BASE_URL`) — qBittorrent web UI/API base URL
- `app.sync.frequency-ms` (env: `APP_SYNC_FREQUENCY_MS`) — polling frequency in milliseconds (default in repo: 300000)
- `app.slack.webhook-url` (env: `APP_SLACK_WEBHOOK_URL`) — Slack webhook for notifications (optional)

Example environment variables are already used in `compose.yaml`.

## What it does

- Periodically polls Gluetun and qBittorrent APIs.
- Detects and reconciles port-forwarding differences.
- Can report events via a configured Slack webhook.

## Useful commands

- Build: `mvn -DskipTests package`
- Build Docker image: `docker build -t tezzad17/gluetun-qbittorrent-portforward:latest .`
- Run via compose: `docker compose -f compose.yaml up -d`

## Where to look in the source

- Main application: `src/main/java/com/tezzad/vpnscheduler/VpnschedulerApplication.java`
- Core services: `src/main/java/com/tezzad/vpnscheduler/service/DockerService.java`, `src/main/java/com/tezzad/vpnscheduler/service/SyncLogic.java`

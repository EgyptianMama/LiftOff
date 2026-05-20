# 🚀 LiftOff

**Self-hosted deployment platform** — push to GitHub, get a live site with HTTPS.

A Vercel/Netlify clone built from scratch as a learning project. The system accepts a GitHub repository via webhook, pulls the code, builds it, serves it under a unique subdomain, and provisions SSL automatically — all triggered by a single `git push`.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.4.5 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | Spring Security + JWT (JJWT) |
| Containers | Docker (docker-java) |
| Reverse Proxy | Caddy (with auto-TLS) |

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Start PostgreSQL

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2. Run the application

```bash
mvn spring-boot:run
```

### 3. Verify

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

### Stop

```bash
# Stop the app (Ctrl+C)
docker compose -f docker-compose.dev.yml down
```

## Project Structure

```
src/main/java/dev/liftoff/platform/
├── LiftoffApplication.java       # Entry point
├── config/                        # App configuration
├── auth/                          # JWT authentication
├── project/                       # Project CRUD + webhooks
├── build/                         # Build pipeline + Docker
├── deployment/                    # Deploy + Caddy routing
├── streaming/                     # SSE log streaming
└── common/                        # Shared exceptions, DTOs
```

## License

MIT

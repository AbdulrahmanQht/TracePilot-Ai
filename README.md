# TracePilot AI

Multi-agent AI trace auditing system. Submits a coding-agent execution trace (Claude Code, Codex, Cursor, etc.) and returns a structured reliability report using blind outcome verification, so the agent's self-reported success claim is never trusted at face value.

[Unverified] This README reflects the project's Software Design Specification as it currently stands. It is a planning document, not a build log — sections describing the pipeline, endpoints, or roadmap describe the intended design, not confirmed running behavior.

## Architecture

Three decoupled services connected by a message queue, with distributed tracing across all of them.


- **Frontend** — React SPA (Vite, Bun) for trace submission, live audit status, and report dashboards.
- **Orchestrator** (`backend/`) — Spring Boot 3.x. Auth, validation, rate limiting, persistence, and job dispatch via RabbitMQ.
- **AI Worker** (`ai-worker/`) — FastAPI + CrewAI. Three agents analyze the trace and publish a result back to the orchestrator.
- **RabbitMQ** — durable queue between the orchestrator and worker (`audit.jobs`, `audit.jobs.dlq`, `audit.results`). Core architecture, not optional.
- **PostgreSQL** — single source of truth for users, audits, reports, and reliability history.
- **Jaeger** — collects OTLP spans from both backend services into one merged trace per audit request.

## Tech Stack

**Backend:** Java 21, Spring Boot, Spring Security (JWT + OAuth2), Spring Data JPA, Spring AMQP, Flyway, Bucket4j, Testcontainers, OpenTelemetry.

**AI Worker:** Python 3.11+, FastAPI, Pydantic, CrewAI, aio-pika, OpenTelemetry.

**Frontend:** React, Vite, Bun, TanStack Query, Recharts.

**Infrastructure:** PostgreSQL, RabbitMQ, Jaeger, Docker Compose.

## Project Structure

```
tracepilot-ai/
  docs/
  frontend/          tracepilot-web
  backend/            tracepilot-api
  ai-worker/          tracepilot-worker
  infra/
    render/
    vercel/
    github-actions/
```

## Core Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Email/password login |
| POST | `/api/auth/refresh` | Issue new access token |
| POST | `/api/audits` | Submit a trace for analysis |
| GET | `/api/audits/{id}` | Fetch status and completed report |
| GET | `/api/audits` | List current user's audit history |
| GET | `/api/reliability` | Reliability trend by repo and agent tool |
| POST | `/api/audits/{id}/share` | Create public share token |
| GET | `/api/shared/{token}` | Fetch public read-only report |

## Local Development

Requires Docker, Bun, Java 21, and Python 3.11+.

```bash
# Start PostgreSQL, RabbitMQ, and Jaeger
docker compose up -d postgres rabbitmq jaeger

# Backend
cd backend && ./gradlew bootRun

# AI worker
cd ai-worker && opentelemetry-instrument uvicorn app.main:app --port 8001

# Frontend
cd frontend && bun install && bun run dev
```

Environment configuration is documented separately and not duplicated here.

## Testing

- Backend: JUnit + Mockito for unit tests; Testcontainers (PostgreSQL, RabbitMQ) for integration tests.
- AI Worker: pytest, with dedicated tests for prompt-isolation and schema validation.

## Roadmap

| Phase | Focus |
|---|---|
| 1. Foundation | Monorepo, Docker Compose, migrations, service scaffolds |
| 2. Authentication | JWT, refresh tokens, GitHub/Google OAuth2 |
| 3. Trace Audit Pipeline | Submission, hashing, blind prompt separation, RabbitMQ dispatch, dead-letter handling |
| 4. AI Agents | Three CrewAI agents with Pydantic contracts |
| 5. React Frontend | Editor, dashboard, reliability chart, history, sharing |
| 6. Observability | OpenTelemetry + Jaeger across both backend services |
| 7. Deploy | CI/CD, Vercel, Render, Neon |

[Unverified] The Phase 3 timeline was originally scoped before RabbitMQ was moved into core architecture; it has not been re-estimated since.

## Optional / Not in Core Scope

- Prometheus + Grafana metrics dashboards.

## Known Open Gaps

Not yet addressed in the design: dispatch timeout/failure policy, prompt-injection handling for submitted traces, pagination on list endpoints, log correlation with trace IDs, and a data retention/backup policy.

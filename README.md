# TracePilot AI

Multi-agent system for auditing AI coding-agent execution traces. A developer pastes a raw transcript, terminal log, PR/CI bundle, or tool-call trace from a coding agent (Claude Code, Codex, Cursor, or similar), and the system returns a structured report on how the agent behaved and whether its outcome claim holds up against the observable evidence.

## Core Idea

The core question this project answers is not whether an LLM can solve a given bug, but whether a developer can trust what a coding agent just did. It does this by examining tool-call cycles, stop conditions, self-reported completion claims, test evidence, and reliability history for a given repo and agent tool.

A key structural feature is the Blind Outcome Verifier: it receives only observable evidence extracted from the trace, never the original agent's final claim or self-rationale. That separation is enforced at the prompt-construction layer and covered by dedicated tests, so the agent's own account of what it did carries no weight in the verdict.

## Architecture

Three decoupled services connected by a durable message queue, with distributed tracing across the backend services.

- **Frontend** (`frontend/`) — React SPA (Vite, Bun, TypeScript) for trace submission, live audit status, report dashboards, and reliability trend charts.
- **Orchestrator** (`backend/`) — Spring Boot 3.x, Java 21. Handles authentication, request validation, rate limiting, trace hashing/caching, persistence, and job dispatch.
- **AI Worker** (`ai-worker/`) — FastAPI + CrewAI, Python 3.11+. Runs three concurrent agents against a submitted trace and publishes a structured result back to the orchestrator. Not exposed to the public internet.
- **RabbitMQ** — durable queue between the orchestrator and the AI worker (`audit.jobs`, `audit.jobs.dlq`, `audit.results`), forming the core dispatch path instead of a direct HTTP call between the two services.
- **PostgreSQL** — single source of truth for users, audits, reports, and reliability history. The AI worker has no direct database access.
- **Jaeger** — collects OpenTelemetry (OTLP) spans from the orchestrator and the AI worker into one merged trace per audit request.


## Tech Stack

**Backend:** Java 21, Spring Boot, Spring Security (JWT + OAuth2), Spring Data JPA, Spring AMQP, Flyway, Bucket4j, Testcontainers, OpenTelemetry Java agent.

**AI Worker:** Python 3.11+, FastAPI, Pydantic, CrewAI, aio-pika, OpenTelemetry.

**Frontend:** React, TypeScript, Vite, Bun, TanStack Query, React Hook Form + Zod, Recharts, shadcn/ui.

**Infrastructure:** PostgreSQL, RabbitMQ, Jaeger, Docker Compose.

## Project Structure

```
tracepilot-ai/
  frontend/     React SPA (tracepilot-web)
  backend/      Spring Boot orchestrator (tracepilot-api)
  ai-worker/    FastAPI + CrewAI worker (tracepilot-worker)
  infra/        Deployment configuration (Render, Vercel, GitHub Actions)
```

## Core Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Email/password login |
| POST | `/api/auth/refresh` | Issue new access token from refresh cookie |
| POST | `/api/audits` | Submit a trace for analysis |
| GET | `/api/audits/{id}` | Fetch status and completed report |
| GET | `/api/audits` | List current user's audit history |
| GET | `/api/reliability` | Reliability trend grouped by repo and agent tool |
| POST | `/api/audits/{id}/share` | Create public share token |
| GET | `/api/shared/{token}` | Fetch public read-only report |

The only required field on audit submission is `rawTrace`. Title, agent tool, repo name, and input source are optional and improve report quality when present.

## Local Development

Requires Docker, Bun, Java 21, and Python 3.11+.

```bash
# Start PostgreSQL, RabbitMQ, and Jaeger
docker compose up -d postgres rabbitmq jaeger

# Backend
cd backend && ./gradlew bootRun

# AI worker
cd ai-worker && opentelemetry-instrument uvicorn app.main:app --reload --port 8001

# Frontend
cd frontend && bun install && bun run dev
```

During local development the frontend calls the orchestrator at `http://localhost:8080`, the orchestrator dispatches to the AI worker through RabbitMQ, and traces for each audit request are viewable at `http://localhost:16686` (Jaeger UI).

## Testing

- **Backend:** JUnit and Mockito for unit tests; Testcontainers against real PostgreSQL and RabbitMQ instances for integration tests.
- **AI Worker:** pytest, including dedicated tests for prompt isolation (confirming the blind verifier never receives withheld claims) and Pydantic schema validation.

## Disclaimer

This project uses large language models (CrewAI agents) to analyze traces and generate reports. Agent output should be treated as a structured judgment to review, not a verified fact.
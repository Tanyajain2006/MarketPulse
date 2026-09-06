# MarketPulse

> See what changed. Understand why.

MarketPulse is a market watchlist and market-change intelligence platform. It is designed to help a user return to a watchlist and quickly understand what meaningfully changed since their last review, why it changed, and what deserves attention.

This repository contains the local development foundation for the product. Product behavior is introduced incrementally behind clear backend, machine-learning, and data-source boundaries.

## What Is MarketPulse?

MarketPulse is built for people who follow many companies but do not have time to repeatedly reconstruct the story behind every price move. A normal watchlist shows the current state; MarketPulse is intended to show the meaningful change since the last review and explain the signals worth investigating.

The product will combine deterministic market-change calculations, NLP and ML event signals, and backend-controlled AI investigation. It is an intelligence and explanation tool, not a stock buy/sell recommendation system.

## Business Impact

Market monitoring is often noisy and repetitive: users lose time comparing old and new information, miss important developments between sessions, and struggle to distinguish a meaningful event from normal price movement. MarketPulse addresses that gap by making change the primary workflow.

The expected business impact is:

- Less time spent manually scanning prices, charts, and news.
- Faster awareness of material changes affecting tracked companies.
- Better context for follow-up research through transparent signals and explanations.
- A more focused experience that supports decisions without presenting investment recommendations.

## Repository Layout

```text
MarketPulse/
├── frontend/       React and TypeScript web application
├── backend/        Java and Spring Boot API
├── ml-service/     Python and FastAPI NLP/ML service
├── database/       MySQL schema and migration assets
├── data/           Historical market-data inputs, initially CSV
├── scripts/        Local development and maintenance scripts
├── docker-compose.yml
└── README.md
```

## Planned Architecture

- The React client will communicate with the Java Spring Boot backend.
- Deterministic market-change calculations will live in the backend.
- NLP and ML signals will be provided by the FastAPI service.
- Gemini investigations and explanations will be requested by the backend; secrets will never be exposed to the frontend.
- MySQL is the only planned database. Redis, MongoDB, and Kafka are intentionally out of scope.
- Historical CSV files are the initial market-data source. A provider abstraction will allow a real market-data provider to be added later.

Authentication, database-backed watchlists, persisted market/news reference data, preferences, checkpoint-aware dashboard calculations, and opt-in demo-data ingestion are implemented. AI, NLP, and recommendations remain future work.

## Prerequisites

- Node.js 20 or newer and npm
- Java 21 and Maven 3.9 or newer
- Python 3.11 or newer
- Docker Desktop with Docker Compose
- MySQL 8.0, or the MySQL container supplied by Compose

## Configuration

Copy the example environment files before starting development:

```text
/.env.example
frontend/.env.example
backend/.env.example
ml-service/.env.example
```

For a local setup, create the files that are consumed by the services:

```bash
cp .env.example .env
cp backend/.env.example backend/.env
```

Do not commit local `.env` files or API keys. Gemini credentials belong on the backend only and are not needed in this foundation phase.

## Local Infrastructure

The initial Compose file starts only MySQL and uses a named volume for local persistence:

```bash
docker compose up -d
```

Stop the container with:

```bash
docker compose down
```

## Start Locally

1. Start MySQL from the repository root:

	```bash
	docker compose up -d mysql
	```

2. Install frontend dependencies:

	```bash
	cd frontend
	npm install
	cd ..
	```

3. Install ML service dependencies:

	```bash
	python3 -m pip install -r ml-service/requirements.txt
	```

4. Start each service in its own terminal:

	```bash
	./scripts/start-backend.sh
	./scripts/start-ml-service.sh
	./scripts/start-frontend.sh
	```

	The scripts validate required runtimes and configuration and fail with an actionable message when setup is incomplete. On systems where executable permissions are not preserved, run them with `bash scripts/start-backend.sh`.

5. Open `http://localhost:5173`. The frontend calls `GET http://localhost:8080/api/health` and displays `Backend Connected` or `Backend Unavailable`.

Health endpoints:

```text
Backend:  http://localhost:8080/api/health
ML:       http://localhost:8000/health
```

## Authentication

Authentication is implemented with Spring Security, BCrypt password hashing, and signed JWTs:

```text
POST /api/auth/register
POST /api/auth/login
```

Registration creates a unique email-based account and returns a JWT with the public user profile. Login returns the same shape after verifying the BCrypt hash. `GET /api/auth/me` returns the current public profile and `POST /api/auth/logout` completes the stateless logout contract; the client removes its JWT. The frontend keeps the session token in browser session storage, never stores plaintext passwords, redirects unauthenticated users to `/login`, and protects `/dashboard`.

Set `JWT_SECRET` in `backend/.env` to a private value of at least 32 bytes. It must remain backend-only and must never be added to frontend environment variables.

## Current Foundation

The repository currently provides:

- Spring Boot API with environment-based MySQL and Hibernate configuration.
- JSON backend health endpoint and frontend-to-backend connectivity status.
- JWT authentication with registration, login, logout, protected dashboard routing, validation, and JSON error responses.
- Ownership-scoped watchlist management at `/api/watchlists` and the `/watchlists` React workspace.
- Opt-in idempotent CSV import for all 10,000 rows in `data/watchlist_seed.csv`, `data/market_snapshots.csv`, and `data/news_events.csv`.
- Queryable market, news, research, preferences, and checkpoint-aware dashboard APIs.
- React, TypeScript, Vite, Tailwind CSS, and React Router application shell.
- Minimal FastAPI service with a health endpoint.
- MySQL Compose service with a persistent named volume and health check.

### Demo data import

The backend importer is disabled by default. To load the supplied CSV data into MySQL, set these backend environment variables before starting Spring Boot:

```text
MARKETPULSE_DEMO_DATA_ENABLED=true
MARKETPULSE_DATA_DIR=data
MARKETPULSE_DEMO_USER_EMAIL=demo@marketpulse.local
MARKETPULSE_DEMO_PASSWORD=change-this-local-demo-password
```

Start the backend once with those variables enabled. The importer creates the local demo user if it does not exist, imports all 10,000 market snapshots and all 10,000 news events, builds the instrument catalog from the complete 10,000-row watchlist seed, and attaches every distinct seeded watchlist group to the demo user. Natural keys and database constraints make repeated runs idempotent. Do not use the documented local demo password outside development.

Available data APIs include:

```text
GET  /api/market/latest?tickers=AAPL,NVDA
GET  /api/market/{ticker}/history?limit=100
GET  /api/news/latest?tickers=AAPL,NVDA
GET  /api/news/{ticker}?limit=50
GET  /api/dashboard/overview
POST /api/dashboard/checkpoint
GET  /api/preferences
PATCH /api/preferences
GET  /api/research/search?q=NVDA
```

## License

Not yet specified.

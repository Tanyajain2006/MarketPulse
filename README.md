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

Authentication, watchlists, market-data ingestion, AI, NLP, recommendations, and business logic are not implemented yet.

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

## Current Foundation

The repository currently provides:

- Spring Boot API with environment-based MySQL and Hibernate configuration.
- JSON backend health endpoint and frontend-to-backend connectivity status.
- React, TypeScript, Vite, Tailwind CSS, and React Router application shell.
- Minimal FastAPI service with a health endpoint.
- MySQL Compose service with a persistent named volume and health check.

The next implementation phases should add database migrations and a market-data source abstraction before product workflows are built.

## License

Not yet specified.

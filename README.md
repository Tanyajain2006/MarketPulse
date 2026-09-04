# MarketPulse

> See what changed. Understand why.

MarketPulse is a market watchlist and market-change intelligence platform. It is designed to help a user return to a watchlist and quickly understand what meaningfully changed since their last review, why it changed, and what deserves attention.

This repository currently contains the project foundation only. Product behavior will be introduced incrementally behind clear backend, machine-learning, and data-source boundaries.

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

Copy the relevant example environment files before starting development:

```text
frontend/.env.example
backend/.env.example
ml-service/.env.example
```

Do not commit local `.env` files or API keys. Gemini credentials belong on the backend only.

## Local Infrastructure

The initial Compose file starts only MySQL and uses a named volume for local persistence:

```bash
docker compose up -d
```

Stop the container with:

```bash
docker compose down
```

## Development Status

This is the foundation phase. The next implementation phases should add the backend health boundary, database migrations, market-data source abstraction, frontend routing shell, and ML service health boundary before product workflows are built.

## License

Not yet specified.

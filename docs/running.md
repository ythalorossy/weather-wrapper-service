# Running locally

## Backend

**With Docker** (recommended — handles Java, Maven, Redis):

```bash
docker compose up --build
```

The API comes up on `http://localhost:8080`. Redis is on `localhost:6379`.

## Frontend (Vite + React)

The `web/` directory is a standalone Vite app that calls the backend at `/api/v1/weather`.
Vite proxies `/api` to `http://localhost:8080`, so you must have the backend running first.

```bash
cd web
npm install
npm run dev          # http://localhost:5173
npm run build        # type-check + production bundle into dist/
```

Open <http://localhost:5173>, type a city (e.g. `Arlington, VA`), and the forecast appears.

```bash
# Sanity check
curl http://localhost:8080/actuator/health

# Get a forecast
curl 'http://localhost:8080/api/v1/weather?city=Arlington,%20VA'
```

## Without Docker (requires Java 21 + Maven 3.9 on the host)

```bash
# Start Redis any way you like, e.g.:
docker run -d -p 6379:6379 --name redis redis:7-alpine

# Build & run
cd backend
mvn -DskipTests package
java -jar weather-api/target/weather-api-*.jar
```
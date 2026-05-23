# JMeter load & stress tests

Apache JMeter plans for **admin authentication** and **device APIs**, with HTML reports suitable for CI artifacts and demos.

## Scenarios

| Plan | Thread group | Endpoint | Purpose |
|------|----------------|----------|---------|
| `auth-load-stress.jmx` | Load Test — Auth Login | `POST /api/admin/auth/login` | 20 users, 30s ramp, 120s steady load |
| `auth-load-stress.jmx` | Stress Test — Auth Login | same | 50 users, 60s ramp (stress) |
| `device-load-stress.jmx` | Load Test — Device Active Config | `GET /api/device/active-config` | 25 users, Bearer device token (repeatable) |
| `device-load-stress.jmx` | Stress Test — Device Activate | `POST /api/device/activate` | 40 users ramp, one activation per CSV row |

## Seed data (`spring.profiles.active=jmeter`)

| Item | Value |
|------|--------|
| Admin user | `perf_admin` / `PerfTest123!` |
| Pending activations | `DEV-PERF-001` … `DEV-PERF-100` + `PERF-ACT-001` … |
| Activated devices | 50 tokens in `data/device-tokens.csv` |

## CI (GitHub Actions)

Workflow: [`.github/workflows/performance.yml`](../../.github/workflows/performance.yml)

1. Builds the JAR and starts it with profile `jmeter` (H2, seeded data).
2. Runs both JMX plans headless.
3. Uploads artifact **`jmeter-reports`** containing HTML dashboards and `.jtl` files.

**Manual run:** Actions → **Performance (JMeter)** → **Run workflow**.

Optional input `base_url` to hit an existing deployment instead of starting the app on the runner.

## Local run

```powershell
# Terminal 1 — API
mvn -q package -DskipTests
java -jar target\*.jar --spring.profiles.active=jmeter

# Terminal 2 — JMeter (requires Apache JMeter 5.6+ on PATH)
.\scripts\run-performance-tests.ps1
```

Reports: `target\jmeter-auth\report\index.html`, `target\jmeter-device\report\index.html`.

## Variables

| Property | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8080` | API base URL (`-JBASE_URL=...`) |
| `DATA_DIR` | `performance/jmeter/data` | CSV directory (`-JDATA_DIR=...`) |

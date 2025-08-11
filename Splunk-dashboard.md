You are a principal backend engineer and solutions architect. Design and scaffold a production-grade REST service (or small set of services) that:

Goal
- Accepts a Splunk query, a duration, and a date (anchor) in the payload.
- Executes the query against a Splunk Search REST API that takes {query, earliest_time, latest_time} and returns logs directly.
- Analyzes logs with an LLM to produce:
  - A concise executive report.
  - Iterative debugging on error traces to get as close as possible to root cause(s).
  - Actionable remediation steps / possible fixes.
  - Error counts, first-seen and last-seen timestamps, and a time-bucketed trend.
- Returns structured JSON suitable for automation and dashboards.
- Provides an operational dashboard showing component statuses and surfacing errors (recent and trending).

Non-Functional Requirements
- Language/stack: Python 3.12, FastAPI, Pydantic v2, httpx (async), uvicorn, structlog.
- Search Service (Splunk proxy) integration:
  - Preferred path: Call an internal Splunk Search REST API.
    - POST /search (or /api/search)
    - Auth: Bearer token (or mTLS), TLS config, timeouts.
    - Request JSON: { "query": "SPL", "earliest_time": "ISO-8601|Splunk format", "latest_time": "ISO-8601|Splunk format", "limit": int?, "cursor": string? }
    - Response JSON: { "events": [ ... ], "next_cursor": "string|null", "stats": {...}? }
    - Support pagination via cursor or page tokens if available; otherwise cap rows.
  - Optional fallback: Native Splunk REST (create job / poll / fetch) if feature-flagged.
- LLM: Abstracted client interface with two implementations:
  - In-house LLM: POST http://localhost:8080/llm/chat/completions with payload:
    { "messages": [ { "role": "user", "content": "..." } ] }
    Expected response shape similar to OpenAI: { "choices": [ { "message": { "content": "..." } } ] }
  - OpenAI-compatible client (optional) via env toggle.
- Reliability: timeouts, retries with exponential backoff/jitter, circuit breaker, request id propagation, structured logs, metrics.
- Performance: stream/paginate results from Search API, cap max rows, incremental fetch, memory-safe processing.
- Observability: Prometheus metrics (latency, success rate, Search/LLM call counts), health endpoints, structured logs with correlation ids.
- Operational Dashboard:
  - Surface live component statuses: API, Search API connectivity, LLM availability, queue/worker status, DB/cache (if any).
  - Error feed (recent N) with request ids, component, severity, first/last seen.
  - KPIs: rps, success/error rates, P95 latency, active jobs, Search/LLM call counts, circuit-breaker state.
  - Time-window selector, auto-refresh, downloads; simple server-rendered page or SPA; optional SSE/WebSocket.

API Design
- POST /v1/analyze
  - Request (JSON):
    {
      "query": "string (Splunk SPL)",
      "time": {
        "mode": "relative" | "absolute",
        "duration": "e.g., -24h or PT24H (required if mode=relative)",
        "date_anchor": "ISO-8601 date/time used with duration (optional)",
        "earliest": "ISO-8601 or Splunk earliest_time (required if mode=absolute)",
        "latest": "ISO-8601 or Splunk latest_time (required if mode=absolute)"
      },
      "limits": { "max_rows": 10000, "timeout_seconds": 120 },
      "analysis": { "max_iterations": 3, "language": "en" }
    }
  - Behavior:
    1) Normalize time window → earliest_time/latest_time for Search API.
    2) Execute POST /search to the Search API; paginate via cursor until cap reached or timeout.
    3) Pre-process logs (group by error signature/stack trace, extract timestamps, deduplicate).
    4) Iterative LLM analysis plan:
       - Pass summary samples + representative full traces.
       - Ask LLM to: identify error groups, hypothesize root causes, propose additional queries if needed, produce fixes and risk.
       - Iterate up to analysis.max_iterations: refine on ambiguities, incorporate more samples, converge on root cause(s).
    5) Produce final structured report (see schema).
  - Response (JSON):
    {
      "report": {
        "overview": "concise multi-paragraph executive summary",
        "key_findings": [ { "title": "string", "detail": "string" } ],
        "root_causes": [
          {
            "signature": "hash or short id",
            "hypothesis": "string",
            "evidence": ["bulleted evidence lines"],
            "confidence": 0.0
          }
        ],
        "error_groups": [
          {
            "signature": "hash or short id",
            "representative_trace": "string",
            "count": 123,
            "first_seen": "ISO-8601",
            "last_seen": "ISO-8601",
            "trend": [ { "bucket_start": "...", "count": n } ]
          }
        ],
        "suggested_fixes": [
          {
            "cause_signature": "link to root_causes.signature",
            "fix": "specific actionable steps",
            "risk": "low|medium|high",
            "effort": "S|M|L"
          }
        ],
        "additional_queries": [ "SPL suggestions to dig deeper" ],
        "limitations": "string"
      },
      "telemetry": {
        "log_sampled": true,
        "sample_ratio": 0.1,
        "total_events": 12345,
        "search_request": { "pages": 3, "duration_ms": 1234 },   // updated to reflect Search API usage
        "llm": { "model": "inhouse", "iterations": 2 }
      },
      "request_id": "uuid"
    }

- GET /v1/analyze/{request_id}/status
  - Returns job state: pending | running | aggregating | analyzing | done | failed; progress %; ETA.

- GET /v1/analyze/{request_id}/result
  - Returns the same JSON as POST response once ready.

- GET /healthz, /readyz
  - Health/readiness probes.

- Dashboard & Ops APIs
  - GET /v1/components
    - Summary of component health (api/search/llm/queue/db/cache), version, uptime, last error snapshot, circuit-breaker state.
  - GET /v1/errors/recent?limit=100&since=ISO-8601
    - Recent structured errors with request_id, component, code, message, first_seen, last_seen, count.
  - GET /v1/metrics/summary?window=15m
    - Aggregated KPIs: rps, success/error rates, latency percentiles, active jobs, Search/LLM call counts.
  - GET /dashboard (HTML)
    - Lightweight web dashboard pulling the above JSON endpoints.
  - GET /v1/stream/events (optional, SSE/WebSocket)
    - Live component/alerts stream for the dashboard.

Configuration (env)
- SEARCH_API_BASE_URL, SEARCH_API_TOKEN, SEARCH_API_VERIFY_SSL=true/false
- USE_NATIVE_SPLUNK=false (feature flag)
- SPLUNK_BASE_URL, SPLUNK_TOKEN (only if USE_NATIVE_SPLUNK=true)
- LLM_PROVIDER=inhouse|openai
- LLM_BASE_URL=http://localhost:8080/llm/chat/completions
- LLM_API_KEY (if needed)
- ANALYSIS_MAX_ITERATIONS=3
- SEARCH_MAX_ROWS=10000
- HTTP_TIMEOUT_SECONDS=30
- DASHBOARD_AUTO_REFRESH_SECONDS=5
- METRICS_WINDOW_DEFAULT=15m

Data Flow & Algorithms
- Log fetching:
  - Prefer Search API pagination (cursor/page); stop at limit/time budget.
  - Parse _time, host, source, sourcetype; extract error signatures (hash stack trace sans variable frames/ids).
  - Bucket timestamps (e.g., 5m) to build trend.
- Sampling strategy:
  - If rows > limit: stratified sampling across time buckets and error groups.
  - Always keep at least one full representative sample per error signature.
- Iterative LLM loop:
  - Iter 1: broad clustering + preliminary root-cause hypotheses + missing info.
  - Iter 2..N: targeted additional samples or counts to confirm/refute.
  - Final: concise report, fixes, confidence, limitations.
- Redaction:
  - Strip secrets, emails, tokens, IPs as configured; log redaction actions.
- Dashboard aggregation:
  - Periodically compute component health from health checks, circuit-breaker states, error budgets.
  - Maintain ring buffer or store for recent errors and KPI rollups.
  - Expose summarized views via ops APIs; push live updates via SSE/WebSocket if enabled.

Deliverables (all in one response)
1) High-level architecture diagram (ASCII).
2) OpenAPI 3.1 YAML spec for the endpoints above (including dashboard/ops endpoints).
3) Pydantic v2 models for requests/responses.
4) FastAPI app skeleton with:
   - /v1/analyze (async background task orchestrating Search API + LLM)
   - status/result endpoints
   - httpx clients for Search API and LLM (timeouts, retries, auth)
   - structlog with request_id middleware
   - Prometheus metrics (Starlette middleware or custom)
   - Dashboard routes and static assets (if SPA)
5) Search API client module (execute, paginate).
6) LLM client interface + InHouse implementation (POST messages → choices[0].message.content).
7) Iterative analyzer module with pluggable strategies and unit tests.
8) Dashboard data aggregator and ops controllers (components/errors/metrics) with unit tests.
9) Example requests/responses and a short runbook (operate, tune limits, dashboard usage).

Constraints & Acceptance Criteria
- Handle long-running analyses without blocking the request thread (background task + polling).
- Enforce input validation and return 400 with error details on invalid time modes.
- Timeouts: Search and LLM requests must fail fast with useful error messages and retry where safe (idempotent).
- Return deterministic JSON shapes; no free-form text outside specified fields.
- No leakage of raw secrets or PII in logs or responses.
- Clear separation of concerns (routers, services, clients, models).
- Graceful degradation: if LLM fails, still return counts/timeline with a note in limitations.
- Dashboard must load in <2s; auto-refresh must not overload backends; ops endpoints cacheable.

Implementation Guidance
- Use FastAPI dependency injection for clients/config.
- Use httpx.AsyncClient with timeouts, retries, and circuit breakers.
- Use Pydantic BaseModel (v2), from_attributes where needed.
- Provide minimal but complete working code scaffold; mark TODOs where appropriate.
- “Think step-by-step” privately but output only the final artifacts requested above (no chain-of-thought in the response).
- For the dashboard: keep UI minimal; prioritize stable JSON APIs; document server-rendered vs SPA.

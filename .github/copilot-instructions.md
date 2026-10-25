# Copilot PR review — Cranhua Backend

When reviewing pull requests, check both code quality and architecture. Flag violations clearly; suggest concrete fixes.

## Code quality

- Correctness and edge cases; regressions relative to existing behavior
- Tests added or updated for new/changed behavior; `mvn verify` should pass
- Security: no secrets, credentials, tokens, or connection strings in the diff; avoid unsafe input handling
- Style: Google Java Style (CI `format` job); keep changes consistent with surrounding code

## Architecture (TuaniStack / Spring)

- Layers: `controller` → `service` → `repository`. Controllers handle HTTP only — no business logic, persistence, or cross-cutting rules in controllers
- Packages under `com.tuanistack.cranhua…`, organized by module/feature and layer
- REST API: paths under `/api/...`, resource segments in kebab-case
- Database: table/column names in `snake_case` Spanish; respect PostGIS/spatial conventions when touching geo data
- Identifiers and domain naming: Spanish where the project already uses Spanish (entities, DB, domain terms)

Human approval (Calidad) remains required; Copilot comments assist review and do not replace it.

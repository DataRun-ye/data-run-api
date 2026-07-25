# DataRun API

DataRun API is the production Spring Boot backend for the DataRun
mobile/data-collection system. It owns authentication and access checks,
configuration delivery, form-template services, submission ingestion, and
server-side persistence.

This repository was initially generated with JHipster and contains historical
and incomplete surfaces. Directory names and old documentation are not proof
that a path is active.

## Repository Guidance

- Coding agents start with [AGENTS.md](AGENTS.md).
- Current production and migration boundaries are in
  [production-boundaries.md](docs/agent-context/production-boundaries.md).
- Accepted open work is in
  [current-work.md](docs/agent-context/current-work.md).
- Focused feature contracts live beside those files under
  `docs/agent-context/`.

## Development

The project uses Java 17, the Maven wrapper, Spring Boot, PostgreSQL, and
Liquibase. Environment-specific services and credentials are configured
outside source control.

```bash
./mvnw
./mvnw test
./mvnw -Pprod clean verify
```

Use focused tests while iterating, then the broader gate appropriate to the
change. A successful local build is not permission to deploy or migrate
production.

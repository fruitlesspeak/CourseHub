# CI/CD Workflows

This document describes the GitHub Actions workflows used in CourseHub.

## Overview

| Workflow | File | Trigger | Purpose |
|----------|------|---------|---------|
| Backend CI | `ci.yml` | Push/PR to `main` | Build and test the Spring Boot backend |
| Backend Mutation Tests | `mutation.yml` | PR to `main`, push to `main`, manual dispatch | Run PIT mutation testing for the authentication, registration, course management, and enrollment services |
| Frontend CI | `frontend-ci.yml` | Push/PR to `main` | Lint, type-check, test, and build the Vue.js frontend |
| Docker Build & Publish | `docker-build.yml` | Push/PR to `main` | Build Docker images; publish to ghcr.io on merge to main |

---

## Backend CI (`ci.yml`)

**Triggers:** Push or pull request to `main`

### What it does:
1. Spins up a PostgreSQL 15 service container
2. Sets up JDK 21 with Maven caching
3. Builds the backend with Maven (`mvn clean install -DskipTests`)
4. Runs unit tests against the PostgreSQL database
5. Uploads test results and JaCoCo coverage report as artifacts

### Required Secrets:
- `POSTGRES_PASSWORD` - Password for the test PostgreSQL database

### Artifacts:
- `test-results` - Surefire test reports
- `coverage-report` - JaCoCo coverage report

---

## Frontend CI (`frontend-ci.yml`)

**Triggers:** Push or pull request to `main`

### What it does:
1. Sets up Node.js 20 with npm caching
2. Installs dependencies (`npm ci`)
3. Runs linting (`npm run lint`) - ESLint + Oxlint
4. Runs TypeScript type checking (`npm run type-check`)
5. Runs unit tests (`npm run test:unit`) - Vitest
6. Builds production bundle (`npm run build`)
7. Uploads the built `dist/` folder as an artifact

### Artifacts:
- `frontend-dist` - Production build output (retained for 7 days)

---

## Backend Mutation Tests (`mutation.yml`)

**Triggers:** Pull request to `main`, push to `main`, or manual dispatch

### What it does:
1. Sets up JDK 21 with Maven caching
2. Compiles backend test classes
3. Runs PIT mutation testing against:
   - `AuthService`
   - `RegisterService`
   - `CourseService`
   - `EnrollmentService`
4. Uploads the generated PIT HTML/XML reports as an artifact

### Artifacts:
- `backend-mutation-report` - PIT mutation coverage reports from `backend/target/pit-reports/`

---

## Docker Build & Publish (`docker-build.yml`)

**Triggers:** Push or pull request to `main`, manual dispatch

### What it does:

**On Pull Requests:**
- Builds Docker images for backend and frontend (verification only, no push)
- Uses GitHub Actions cache for faster builds

**On Push to Main:**
- Builds Docker images for backend and frontend
- Pushes images to GitHub Container Registry (ghcr.io)
- Tags images with `latest` and short git SHA
- Outputs a summary of published images

### Published Images:

After merging to main, images are available at:
```
ghcr.io/fruitlesspeak/coursehub/backend:latest
ghcr.io/fruitlesspeak/coursehub/backend:<sha>

ghcr.io/fruitlesspeak/coursehub/frontend:latest
ghcr.io/fruitlesspeak/coursehub/frontend:<sha>
```

### Pulling Images:

```bash
# Pull latest
docker pull ghcr.io/fruitlesspeak/coursehub/backend:latest
docker pull ghcr.io/fruitlesspeak/coursehub/frontend:latest

# Pull specific version
docker pull ghcr.io/fruitlesspeak/coursehub/backend:abc1234
```

### Permissions:

The workflow uses `GITHUB_TOKEN` which is automatically provided. No additional secrets are required.

The repository must have **Packages** write permissions enabled for the `GITHUB_TOKEN`. This is configured via the `permissions` block in the workflow.

---

## Workflow Status

You can view workflow runs at:
https://github.com/fruitlesspeak/CourseHub/actions

## Accessing Automatic Scan Reports

CourseHub uses SonarCloud automatic analysis. These scans are not stored as GitHub Actions artifacts.

To access the report from GitHub:
1. Create a pull request.
2. Find the `SonarCloud Code Analysis` check.
3. Click the SonarCloud link in the check result to open the full report.

To access the report directly in SonarCloud:
1. Open https://sonarcloud.io
2. Navigate to the `fruitlesspeak_CourseHub` project.
3. Open the latest analysis for the branch or pull request you want to review.

The SonarCloud report includes detected issues, security hotspots, code smells, duplication, and quality gate status.

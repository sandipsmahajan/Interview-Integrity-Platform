# Recruiter Portal — Local Development

This guide explains how to build and run the recruiter portal
(`portals/recruiter`) on your machine. The portal is a React 19 + MUI v9
single-page application served by Vite. It talks to the backend microservices
through `/api`, which the Vite dev server proxies to the api-gateway on
`localhost:8080`.

## Prerequisites

- Node.js `>=24 <25` and npm `>=11` (see `engines` in `portals/package.json`).
- Backend stack for real data. The portal only renders mock-free, real API
  responses, so the platform services must be running (see "Run the backend"
  below).

## 1. Install dependencies

The portals repo is an npm workspace; all packages are installed from the
`portals` root in one go.

```bash
# From the repository root
cd portals

# Install all workspaces (recruiter, admin, shared/*)
npm install
```

## 2. Run the backend

The portal proxies `/api` to `http://localhost:8080` (api-gateway), so the
backend must be up. The easiest path is the Docker Compose stack plus Gradle
for the services.

```bash
# Start infrastructure: postgres, redis, kafka, minio
cd infra/docker
docker compose up -d

# From the repository root, run the services the portal needs.
# The api-gateway (8080) is the single entry point the portal talks to.
./gradlew :services:api-gateway:bootRun
```

Any backend API the portal pages call must be running (identity-service for
auth, recruiter/candidate/interview services, etc.). See
`docs/architecture.md` for the service inventory and ports. Run the remaining
`./gradlew :services:<name>:bootRun` tasks in separate
terminals, or use the IntelliJ run configurations described in
`docs/local-development-intellij.md`.

For local email/OTP flows you also need a mail sink on the SMTP port the
notification-service expects (default `localhost:1025`), e.g. Mailpit or
MailHog:

```bash
docker run --rm -p 1025:1025 -p 8025:8025 axllent/mailpit
```

Verify the OTP/verification emails at `http://localhost:8025`.

## 3. Run the dev server

```bash
# From the portals root
npm run dev
```

This starts Vite for the recruiter workspace on `http://localhost:5173`
(bound to `0.0.0.0`). The dev server:

- proxies `/api/*` to `http://localhost:8080` (configurable, see below);
- enables HMR, so edits to `portals/recruiter/src` hot-reload immediately;
- accepts the `*.monkeycode-ai.live` preview domain via `allowedHosts`.

Open http://localhost:5173 and sign in with a user created through the
registration page.

## 4. Build a production bundle

```bash
# Type-check then bundle with Vite
npm run build --workspace @interview-integrity/recruiter
```

Output lands in `portals/recruiter/dist`. The app is code-split with
`React.lazy`, so the initial chunk is intentionally below the raw Vite output
size; the >500 kB chunk warning is expected and safe to ignore.

To serve the built bundle locally:

```bash
npm run preview
```

Preview serves the `dist` folder on `http://localhost:4173` (also bound to
`0.0.0.0`).

## 5. Quality checks

Run these before pushing portal changes. All must pass.

```bash
# TypeScript strict type-check
npm run typecheck

# ESLint with zero warnings allowed
npm run lint

# Unit tests (vitest)
npm run test
```

A full workspace build (recruiter + admin + shared packages):

```bash
npm run build
```

## 6. Environment variables

The portal reads these from `.env` files in `portals/recruiter` (or the
process environment). Vite prefixes `VITE_` are required.

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api` | Base path prepended to every API request from the browser |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Backend target the Vite dev-server proxy forwards `/api` to |
| `VITE_APP_NAME` | `Integrity Pro Recruiter Portal` | Application name baked into the bundle (`__APP_NAME__`) |

Example `portals/recruiter/.env.local`:

```bash
VITE_API_PROXY_TARGET=http://localhost:8080
VITE_APP_NAME=Integrity Pro Recruiter Portal
```

## 7. Running the E2E smoke suite

Playwright tests live in `portals/tests`. The config starts the Vite dev server
itself and targets `http://localhost:5173`, so only the backend stack needs to
be running.

```bash
# From the portals root
npx playwright test
```

## Troubleshooting

- **Login fails / pages return 401** — the api-gateway (8080) is not running,
  or `VITE_API_PROXY_TARGET` points at the wrong port. Check the Vite terminal
  for proxy errors and confirm the gateway is up.
- **Port 5173 already in use** — Vite picks the next free port; use the
  printed URL, or stop the conflicting process.
- **`engines` error on `npm install`** — the installed Node/npm is outside the
  supported range (`>=24 <25` Node, `>=11` npm). Use a matching version via
  `nvm`/`fnm`.
- **Email verification/OTP emails not arriving** — no mail sink is listening on
  the SMTP port; start Mailpit (step 2) and check `http://localhost:8025`.

# Authentication and access control

## Roles

| Role | Main permissions |
| --- | --- |
| `SYSTEM_ADMIN` | View all tenders, bids, organizations, and users; register organizations; provision staff; create/publish tenders; evaluate bids. |
| `PROCUREMENT_OFFICER` | Create/publish tenders and manage requirements for their own authority; view that authority's bids after the submission deadline; evaluate those bids. |
| `BIDDER` | Register a bidder organization, view published tenders, submit one bid per tender, and view/upload documents for bids owned by that organization. |
| `EVALUATION_OFFICER` | View tenders and evaluate bids for their assigned authority (currently organization-scoped because no separate tender assignment model exists). |

The former `COMMITTEE_MEMBER` role and all role simulation controls have been removed. Backend object checks scope officers by organization and bidders by their own organization. Tender bid lists and bid details remain hidden from authority staff until the submission deadline.

Private bid and document access is constrained in repository queries using the organization resolved from the authenticated database user. Bidder tender discovery remains global for `PUBLISHED` tenders. PostgreSQL Row-Level Security is not enabled yet; it is a potential defense-in-depth layer after transaction-scoped tenant context and connection-pool reset behavior are designed and tested.

## API

All paths use `/api/v1`. JSON responses omit the refresh token; the server sets it as an HttpOnly cookie. The access token is a 15-minute bearer JWT and should be kept in memory by browser clients.

### Account registration

`POST /auth/register` accepts `actorType` values `BIDDER`, `ORGANIZATION`, or `ADMIN`. Bidder registration creates an active bidder organization and account. Organization and Admin selections create pending requests; they do not grant privileged access until an existing system administrator reviews them. Organization requests require `organizationName` and `registrationNumber`.

```json
{
  "fullName": "Asha Rao",
  "email": "asha@example.in",
  "password": "Long-Unique-Passphrase-7!",
  "actorType": "BIDDER",
  "organizationName": "Example Health Systems Pvt Ltd",
  "registrationNumber": "CIN-U12345MH2024PTC123456"
}
```

Returns `201` with `{ "status": "ACTIVE", "message": "...", "user": {...} }` for a bidder, or `{ "status": "PENDING", "message": "...", "requestId": "..." }` for an organization/admin request. Passwords need at least 12 characters, including uppercase, lowercase, number, and symbol.

System administrators review pending requests through `GET /auth/registration-requests`, then `POST /auth/registration-requests/{id}/approve` or `/reject`. Organization approval creates a procurement authority and its procurement officer account. Admin approval provisions a system administrator. The first system administrator must be bootstrapped through server-side credentials.

### Login

`POST /auth/login`:

```json
{"email":"asha@example.in","password":"Long-Unique-Passphrase-7!"}
```

Returns `200` with `accessToken` and `user`; sets `refresh_token` (HttpOnly, SameSite Strict, path `/api/v1/auth`). Invalid credentials return `401`.

### Refresh, logout, and account

- `GET /auth/csrf` returns a CSRF token and sets the `XSRF-TOKEN` cookie.
- `POST /auth/refresh` rotates the refresh cookie and returns a new access token. Send `X-XSRF-TOKEN` from the cookie.
- `POST /auth/logout` revokes the current refresh session and clears its cookie. Send the CSRF header.
- `POST /auth/logout-all` revokes all refresh sessions for the authenticated user.
- `GET /auth/me` returns the current authenticated account.
- `POST /auth/change-password` accepts `{ "currentPassword": "...", "newPassword": "..." }` and revokes refresh sessions.
- `POST /auth/provision` is system-admin-only and provisions `PROCUREMENT_OFFICER`, `EVALUATION_OFFICER`, or `SYSTEM_ADMIN` accounts scoped to a procurement authority.
- `GET /auth/users` is system-admin-only.
- `PATCH /auth/users/{id}/role` is system-admin-only; it validates organization type, revokes the account's refresh sessions, and prevents removing the last active system administrator.

For protected routes, send `Authorization: Bearer <accessToken>`. Unauthenticated calls return `401`; insufficient roles or scope return `403` or `404` (the latter hides whether another organization's resource exists).

## Configuration

Required existing variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` (at least 32 bytes of high-entropy secret). Authentication/session settings:

| Variable | Default | Purpose |
| --- | --- | --- |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated exact frontend origins. |
| `REFRESH_COOKIE_SECURE` | `false` | Set `true` behind HTTPS in production. |
| `REFRESH_TOKEN_DAYS` | `7` | Refresh session lifetime. |
| `BOOTSTRAP_ADMIN_EMAIL` | empty | Optional one-time initial system admin email. |
| `BOOTSTRAP_ADMIN_PASSWORD` | empty | Optional one-time initial admin password (12+ chars with uppercase, lowercase, number, and symbol). |
| `BOOTSTRAP_ADMIN_ORGANIZATION_ID` | empty | Optional existing authority UUID; leave empty for a global admin. |

The bootstrap user is created only when the email is not already present. Keep bootstrap credentials in a secret manager and remove them after first startup.

## Local start

1. Set the database variables and a random `JWT_SECRET` of at least 32 bytes in the backend environment.
2. Start PostgreSQL, then run `./mvnw spring-boot:run` from the repository root. Flyway applies migration `V9__create_authentication.sql`.
3. Set `VITE_API_BASE_URL=/api/v1` if desired (this is the default), then run `npm install` and `npm run dev` from `frontend/`. Vite proxies `/api` to `http://localhost:8080`.
4. Bootstrap a system admin, create a procurement authority, then provision authority staff through User Management. Bidders can self-register from the sign-in page.

## Current implementation limits

- Expired published tenders are processed hourly: those without bids are deleted; those with bids are moved to `CLOSED` so bid, document, and procurement history remains linked.
- Staff provisioning currently uses an admin-created initial password. There is no invitation-email delivery, password reset, MFA, or credential breach check.
- Evaluation-officer access is scoped to an authority rather than an individually assigned tender. Final approval/sign-off endpoints do not exist in the existing application.
- The current document API supports secure upload and metadata listing; it does not implement a document-download endpoint.
- Audit logs cover account creation, login success/failure, logout, password changes, tender publication, bid submission, and evaluation execution. They do not yet contain final approval events because there is no approval API.
- Refresh cookies use `SameSite=Strict`; deploy the browser and API on the same site, or adjust cookie policy before using separate sites. Configure `REFRESH_COOKIE_SECURE=true` in production.

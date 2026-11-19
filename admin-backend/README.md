# admin-backend

Backend of the [`admin-web/`](../admin-web) app (Spring Boot 3 / Java 21).

It has its own PostgreSQL 16 + PostGIS database and talks to
[`tourist-backend/`](../tourist-backend) only over the private network.

## Build and test

Java 21 and Maven are required.

```bash
mvn verify -DskipITs     # build, unit tests, contract tests, format check
mvn verify               # also runs the integration tests (*IT), which need Docker
mvn spotless:apply       # fix formatting
```

The integration tests start PostgreSQL 16 with PostGIS in Docker and run every
Flyway migration in `src/main/resources/db/migration`.

## Configuration

The database comes from the environment: `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. The application
runs the migrations when it starts.

Access tokens come from the "Traveler admin" OAuth2 provider in authentik:

| Variable | Meaning |
| --- | --- |
| `TRAVELER_SECURITY_ISSUER` | Issuer of the provider, exactly as in the `iss` claim |
| `TRAVELER_SECURITY_JWK_SET_URI` | JWKS URL of the provider |
| `TRAVELER_SECURITY_AUDIENCE` | Required audience, default `traveler-admin` |

Without an issuer and a JWKS URL every token is rejected. The provider's subject
mode must be the user's UUID (`sub_mode: user_uuid`), because the role is looked
up in `staff_users` by issuer and subject.

Photos are kept in S3-compatible object storage (path-style requests):
`TRAVELER_S3_ENDPOINT`, `TRAVELER_S3_REGION` (default `us-east-1`),
`TRAVELER_S3_BUCKET`, `TRAVELER_S3_ACCESS_KEY` and `TRAVELER_S3_SECRET_KEY`. The
bucket is created at start if it is missing. While the storage is not configured
or cannot be reached, the application still runs and the photo endpoints answer
503.

The QR codes carry a token signed with Ed25519. `TRAVELER_QR_PRIVATE_KEY` is the
private key, PKCS#8 DER in base64; without it the QR code PDF answers 503. Make one
with:

```bash
openssl genpkey -algorithm ed25519 -outform DER | base64 -w0
```

The tourist backend verifies the tokens with the matching public key
(`openssl pkey -inform DER -pubout` on the decoded private key). Keep the private
key out of the repository.

### Staff accounts on the test environment

Until staff management exists (US-13), add a staff member with SQL. The subject is
the authentik user's UUID and the issuer is the value of `TRAVELER_SECURITY_ISSUER`;
the role is `city_admin`, `traveler_admin` or `partner`:

```sql
insert into staff_users (oidc_issuer, oidc_subject, email, role)
values ('<issuer URL>', '<authentik user UUID>', '<email>', 'city_admin');
```

## Endpoints

- `/actuator/health`: health, including the database
- `/v3/api-docs`: OpenAPI description
- `/swagger-ui.html`: browsable OpenAPI description
- `/admin/me`: the signed-in staff member (any role)
- `/admin/attractions`: attraction management (`city_admin` and `traveler_admin`)

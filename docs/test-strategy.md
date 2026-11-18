# Test strategy

Every acceptance criterion (AC-01 to AC-71) is mapped to at least one test
case, and the results are recorded in a test report at the end of each
iteration. Section 9 of the technical offer defines the test types; this page
adds who owns each of them.

## Test types and owners

| Test type | What it covers | Tools | Criteria | Owner |
| --- | --- | --- | --- | --- |
| Unit tests | Business rules: distance check, points, route generation, voucher states | JUnit, Kotlin unit tests | FR-04, FR-08, FR-10 | Not named yet |
| Integration tests | API endpoints with a real database and cache, and contract tests for the internal API between the backends | Testcontainers | AC-25 to AC-33, AC-36 to AC-38, AC-63 to AC-71 | Check-in: Cimpoeru Theodora-Mihaela (EN-13). Every new endpoint needs them to be done, so they are written with its story |
| End-to-end tests | Main user flows on Android and iOS | Compose UI tests (Android), XCTest (iOS) | AC-01 to AC-35, AC-45 to AC-48, AC-62 | Not named yet |
| Performance tests | Local and server response times, cold start, first launch | Android Studio Profiler, Macrobenchmark, k6 | AC-55 to AC-57 | Popescu Emanoil-Florian-Sinisha (EN-16) |
| Load test | 100 concurrent users for 10 minutes | k6 | AC-41 | Popescu Emanoil-Florian-Sinisha (EN-16) |
| Reliability tests | Uptime monitoring; stopping a module to check failure quarantine | Uptime Kuma, Docker | AC-39, AC-42 | Not named yet |
| Field tests in Sinaia | GPS accuracy, weak network, battery use | Test phones, Android battery statistics | AC-59, AC-60 | Sainenco Luchian (EN-17) |
| Security tests | Role checks, access to other users' data, forged QR codes | Automated API tests, OWASP ZAP | AC-36 to AC-38 | Cimpoeru Theodora-Mihaela (EN-18) |
| User acceptance tests | Two sessions with at least 10 users each | Observation, System Usability Scale questionnaire | AC-58 | Cimpoeru Theodora-Mihaela (EN-14, EN-19) |

The test reports, including the final one that covers every test type above, are
written by Cimpoeru Theodora-Mihaela (EN-22).

### To decide

The offer and the sprint plan do not name an owner for unit tests, end-to-end
tests and reliability tests. Section 11 of the offer gives each member one part
of the system to lead, which is not enough to say who writes and runs these. The
team assigns them, and this table is updated.

## What runs in the pipeline

Every pull request that changes a backend or the API contract runs, for each
backend:

- a format check (Spotless);
- the build with the unit tests, the contract tests and the integration tests,
  which start PostgreSQL with PostGIS through Testcontainers;
- the contract tests again as a stage of their own, against
  `api/internal-api.yaml`;
- a Docker image build.

Every pull request also gets a dependency scan with OSV-Scanner. It also runs on
`main` and every Monday, and fails on a high or critical finding (CVSS 7.0 or
more) that `osv-scanner.toml` does not list.

Not in the pipeline yet: the k6 scenarios, OWASP ZAP, the mobile UI tests, and
the uptime monitoring.

## Defects

Defects are tracked in GitHub Issues with a severity level. Critical and major
defects are fixed before the iteration demo, and the second user test is
expected to report fewer defects than the first. The repository has the
`type:bug` label and the priorities P0 to P2, but no severity labels yet.

## Definitions

The Definition of Ready and the Definition of Done are in the overview tab of
the sprint plan. The team agrees them in EN-10.

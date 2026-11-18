# api

`internal-api.yaml` is the OpenAPI contract of the calls from
[`tourist-backend/`](../tourist-backend) to [`admin-backend/`](../admin-backend).
It is internal only and is not exposed to the apps.

Both backends build from this one file:

- `admin-backend` is the provider. The server interface is generated from the
  contract and implemented by a controller.
- `tourist-backend` is the consumer. Its client is generated from the contract.

## Contract tests

Each backend has tests tagged `contract` that check its side against the file:
the provider validates its answers, the consumer validates the answers it is
tested with. Run them from a backend folder:

```bash
mvn test -Dgroups=contract
```

Changing an operation means changing `internal-api.yaml`; both backends are
regenerated and both contract tests must stay green.

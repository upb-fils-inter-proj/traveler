-- EN-06.3: vouchers, points ledger and operations tables (DATA tab, sections 3.3 and 3.4).

create table vouchers (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references app_users (id) on delete cascade,
    -- IDs in the admin database (no foreign key); the voucher is valid only at that partner store.
    reward_id    uuid not null,
    partner_id   uuid not null,
    -- Hash of the signed token shown as a QR code.
    token_hash   bytea not null unique,
    points_spent integer not null,
    issued_at    timestamptz not null,
    -- issued_at + the reward's voucher validity.
    expires_at   timestamptz not null,
    -- Set once, in a transaction, when the admin backend reports a partner scan.
    used_at      timestamptz
);

create index vouchers_user_id_idx on vouchers (user_id);

create table points_ledger (
    id            bigint generated always as identity primary key,
    user_id       uuid not null references app_users (id) on delete cascade,
    -- Positive for earnings, negative for redemptions.
    delta         integer not null check (delta <> 0),
    reason        text not null
                  check (reason in ('claim', 'route_bonus', 'danger_award', 'redemption', 'adjustment')),
    -- Whatever caused the change.
    attraction_id uuid,
    route_id      uuid,
    voucher_id    uuid,
    created_at    timestamptz not null default now()
);

create index points_ledger_user_created_idx on points_ledger (user_id, created_at);

-- Events waiting to be sent to the admin backend's statistics engine.
create table outbox_events (
    id         bigint generated always as identity primary key,
    event_id   uuid not null unique default gen_random_uuid(),
    type       text not null
               check (type in ('claim', 'route_generated', 'route_completed', 'redemption')),
    -- Without user identity.
    payload    jsonb not null,
    created_at timestamptz not null default now(),
    sent_at    timestamptz
);

create index outbox_events_unsent_idx on outbox_events (id) where sent_at is null;

-- Makes retried internal calls safe; rows older than 7 days are deleted.
create table processed_requests (
    idempotency_key text primary key,
    response        jsonb,
    created_at      timestamptz not null default now()
);

-- Export requests.
create table data_exports (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references app_users (id) on delete cascade,
    status       text not null default 'queued'
                 check (status in ('queued', 'running', 'ready', 'failed')),
    requested_at timestamptz not null default now(),
    completed_at timestamptz,
    file_key     text,
    expires_at   timestamptz
);

create index data_exports_user_id_idx on data_exports (user_id);

-- Failure quarantine; mirrored in Redis.
create table feature_flags (
    key        text primary key,
    enabled    boolean not null,
    reason     text,
    updated_at timestamptz not null default now()
);

-- Scheduled and on-demand jobs. The DATA tab says these two tables are created by the
-- ShedLock and job queue libraries; they are created here because every table is created
-- only through Flyway migrations. Definitions follow the libraries' PostgreSQL schemas
-- (ShedLock, and db-scheduler for scheduled_tasks).
create table shedlock (
    name       varchar(64) not null primary key,
    lock_until timestamp(3) not null,
    locked_at  timestamp(3) not null,
    locked_by  varchar(255) not null
);

create table scheduled_tasks (
    task_name            text not null,
    task_instance        text not null,
    task_data            bytea,
    execution_time       timestamptz not null,
    picked               boolean not null,
    picked_by            text,
    last_success         timestamptz,
    last_failure         timestamptz,
    consecutive_failures integer,
    last_heartbeat       timestamptz,
    version              bigint not null,
    priority             smallint,
    primary key (task_name, task_instance)
);

create index scheduled_tasks_execution_time_idx on scheduled_tasks (execution_time);
create index scheduled_tasks_last_heartbeat_idx on scheduled_tasks (last_heartbeat);
create index scheduled_tasks_priority_execution_time_idx on scheduled_tasks (priority desc, execution_time asc);

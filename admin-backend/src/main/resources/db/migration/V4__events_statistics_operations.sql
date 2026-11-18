-- EN-07.3: events, statistics and operations tables (DATA tab, sections 4.4 and 4.5).

-- IDs of processed events, so a batch sent twice is counted once.
create table inbox_events (
    event_id    uuid primary key,
    type        text not null,
    received_at timestamptz not null default now()
);

-- Statistics views. Days use the Europe/Bucharest calendar day.
create table stats_attraction_daily (
    attraction_id    uuid not null references attractions (id),
    day              date not null,
    claims           integer not null default 0,
    route_inclusions integer not null default 0,
    primary key (attraction_id, day)
);

-- Redemption charts per partner location.
create table stats_reward_daily (
    reward_id   uuid not null references rewards (id),
    partner_id  uuid not null references partners (id),
    day         date not null,
    redemptions integer not null default 0,
    points_spent integer not null default 0,
    primary key (reward_id, partner_id, day)
);

create index stats_reward_daily_partner_day_idx on stats_reward_daily (partner_id, day);

-- Who changed what in the admin web app, including danger flags and reward removals.
create table audit_log (
    id             bigint generated always as identity primary key,
    actor_staff_id uuid references staff_users (id) on delete set null,
    action         text not null,
    entity_type    text not null,
    entity_id      text,
    details        jsonb,
    created_at     timestamptz not null default now()
);

create index audit_log_actor_idx on audit_log (actor_staff_id) where actor_staff_id is not null;
create index audit_log_entity_idx on audit_log (entity_type, entity_id);

-- Failure quarantine of the admin backend (same structure as in the tourist database).
create table feature_flags (
    key        text primary key,
    enabled    boolean not null,
    reason     text,
    updated_at timestamptz not null default now()
);

-- Jobs of the admin backend (same structure as in the tourist database). The DATA tab says
-- these two tables are created by the ShedLock and job queue libraries; they are created here
-- because every table is created only through Flyway migrations. Definitions follow the
-- libraries' PostgreSQL schemas (ShedLock, and db-scheduler for scheduled_tasks).
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

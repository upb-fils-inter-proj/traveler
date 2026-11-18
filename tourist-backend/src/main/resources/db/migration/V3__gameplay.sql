-- EN-06.2: gameplay tables (DATA tab, section 3.2).
-- routes is created first because progress references it.

create table routes (
    id             uuid primary key default gen_random_uuid(),
    user_id        uuid not null references app_users (id) on delete cascade,
    target_minutes smallint not null check (target_minutes in (30, 60, 120)),
    status         text not null default 'active'
                   check (status in ('active', 'completed', 'dismissed', 'expired')),
    -- After this day (Europe/Bucharest) the route counts as expired.
    route_date     date not null,
    -- Set on completion: 50% of the pinned points of the stops.
    bonus_points   integer
);

-- One active route per user.
create unique index routes_one_active_per_user_idx on routes (user_id) where status = 'active';
-- Supports the cascade when an account is deleted.
create index routes_user_id_idx on routes (user_id);

create table route_stops (
    route_id      uuid not null references routes (id) on delete cascade,
    position      smallint not null check (position between 1 and 7),
    -- ID of the attraction in the admin database (no foreign key).
    attraction_id uuid not null,
    -- Stop and point value copied when the route is generated.
    pinned_points integer not null,
    pinned_name   text not null,
    primary key (route_id, position)
);

create table progress (
    user_id        uuid not null references app_users (id) on delete cascade,
    -- ID of the attraction in the admin database (no foreign key).
    attraction_id  uuid not null,
    claimed_at     timestamptz not null default now(),
    points_awarded integer not null,
    source         text not null default 'scan' check (source in ('scan', 'danger_award')),
    -- Distance at check-in; null for danger awards.
    distance_m     real,
    -- Set when the attraction was a stop of the active route.
    route_id       uuid references routes (id) on delete set null,
    -- One claim per user and attraction.
    primary key (user_id, attraction_id)
);

create index progress_route_id_idx on progress (route_id) where route_id is not null;

create table locations (
    id          bigint generated always as identity primary key,
    user_id     uuid not null references app_users (id) on delete cascade,
    position    geography(Point, 4326) not null,
    accuracy_m  real not null,
    -- Rows older than 30 days are deleted every night.
    recorded_at timestamptz not null
);

create index locations_user_recorded_idx on locations (user_id, recorded_at);

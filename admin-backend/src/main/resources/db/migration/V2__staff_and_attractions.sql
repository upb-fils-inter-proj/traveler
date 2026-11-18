-- EN-07.1: staff accounts and attractions (DATA tab, sections 4.1 and 4.2).
-- Conventions (section 2): UUID keys from gen_random_uuid(), timestamptz in UTC,
-- text columns with CHECK constraints instead of enum types.

create table staff_users (
    id            uuid primary key default gen_random_uuid(),
    oidc_issuer   text not null,
    oidc_subject  text not null,
    email         text not null unique,
    -- Assigned by Traveler Administration; authentik only authenticates.
    role          text not null check (role in ('partner', 'city_admin', 'traveler_admin')),
    status        text not null default 'active' check (status in ('active', 'deactivated')),
    created_at    timestamptz not null default now(),
    last_login_at timestamptz,
    constraint staff_users_oidc_identity_key unique (oidc_issuer, oidc_subject)
);

create table attractions (
    id              uuid primary key default gen_random_uuid(),
    location        geography(Point, 4326) not null,
    -- Size of the red area on the map.
    danger_radius_m integer not null default 100,
    -- Key in object storage.
    photo_key       text,
    -- Set by administrators.
    point_value     integer not null check (point_value > 0),
    -- Used to estimate route durations.
    visit_minutes   integer not null default 15,
    -- Removed attractions are kept for history but hidden.
    status          text not null default 'open' check (status in ('open', 'closed', 'removed')),
    -- Red area on the map, check-ins blocked, excluded from new routes.
    danger_flag     boolean not null default false,
    -- Warning shown on the map and in the push notification.
    danger_message  text,
    -- Part of the signed QR token; increasing it invalidates printed codes.
    qr_version      integer not null default 1,
    -- Increases with every change; used by the tourist backend's cache.
    version         bigint not null,
    created_by      uuid references staff_users (id),
    constraint attractions_danger_message_required
        check (not danger_flag or (danger_message is not null and danger_message <> ''))
);

create index attractions_created_by_idx on attractions (created_by);

create table attraction_translations (
    attraction_id uuid not null references attractions (id) on delete cascade,
    lang          text not null check (lang in ('ro', 'en')),
    name          text not null,
    description   text not null,
    -- Longer historical details.
    history       text,
    -- Where each fact was checked (at least two sources); not sent to the tourist backend.
    sources       text,
    primary key (attraction_id, lang)
);

-- History of danger flags; cleared_at is null while the flag is active.
create table danger_alerts (
    id            uuid primary key default gen_random_uuid(),
    attraction_id uuid not null references attractions (id),
    message       text not null,
    flagged_by    uuid references staff_users (id),
    cleared_by    uuid references staff_users (id),
    flagged_at    timestamptz not null default now(),
    cleared_at    timestamptz
);

create index danger_alerts_attraction_id_idx on danger_alerts (attraction_id);

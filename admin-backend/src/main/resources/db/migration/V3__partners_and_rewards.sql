-- EN-07.2: partners, rewards and redemptions (DATA tab, section 4.3).

-- Partner stores and their locations.
create table partners (
    id            uuid primary key default gen_random_uuid(),
    staff_user_id uuid not null unique references staff_users (id),
    store_name    text not null,
    address       text not null,
    location      geography(Point, 4326) not null,
    contact_email text not null,
    status        text not null default 'active' check (status in ('active', 'deactivated')),
    created_by    uuid references staff_users (id),
    created_at    timestamptz not null default now()
);

create index partners_created_by_idx on partners (created_by);

-- Rewards created by Traveler Administration.
create table rewards (
    id                 uuid primary key default gen_random_uuid(),
    title              text not null,
    description        text not null,
    point_cost         integer not null check (point_cost > 0),
    voucher_valid_days integer not null check (voucher_valid_days > 0),
    available_from     timestamptz,
    available_until    timestamptz,
    active             boolean not null default true,
    -- Increases with every change; used by the tourist backend's cache.
    version            bigint not null,
    created_by         uuid references staff_users (id),
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now()
);

create index rewards_created_by_idx on rewards (created_by);

-- Which partner locations offer each reward. A partner removes a reward from its
-- location with a reason of at least 10 characters.
create table reward_assignments (
    reward_id      uuid not null references rewards (id),
    partner_id     uuid not null references partners (id),
    status         text not null default 'active' check (status in ('active', 'removed')),
    removal_reason text,
    removed_at     timestamptz,
    removed_by     uuid references staff_users (id),
    primary key (reward_id, partner_id),
    constraint reward_assignments_removal_reason_required
        check (status <> 'removed' or (removal_reason is not null and char_length(removal_reason) >= 10))
);

create index reward_assignments_partner_id_idx on reward_assignments (partner_id);
create index reward_assignments_removed_by_idx on reward_assignments (removed_by);

-- Written when a partner scan succeeds; feeds the partner's live statistics.
create table redemptions_log (
    id           uuid primary key default gen_random_uuid(),
    -- ID of the voucher in the tourist database (no foreign key).
    voucher_id   uuid not null,
    reward_id    uuid not null references rewards (id),
    partner_id   uuid not null references partners (id),
    validated_by uuid not null references staff_users (id),
    validated_at timestamptz not null default now()
);

create index redemptions_log_reward_id_idx on redemptions_log (reward_id);
create index redemptions_log_partner_validated_idx on redemptions_log (partner_id, validated_at);
create index redemptions_log_validated_by_idx on redemptions_log (validated_by);

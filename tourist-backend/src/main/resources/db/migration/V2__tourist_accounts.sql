-- EN-06.1: tourist accounts (DATA tab, section 3.1).
-- Conventions (section 2): UUID keys from gen_random_uuid(), timestamptz in UTC,
-- text columns with CHECK constraints instead of enum types.

create table app_users (
    id            uuid primary key default gen_random_uuid(),
    oidc_issuer   text not null,
    oidc_subject  text not null,
    status        text not null default 'profile_pending'
                  check (status in ('profile_pending', 'active', 'deactivated')),
    created_at    timestamptz not null default now(),
    last_login_at timestamptz,
    constraint app_users_oidc_identity_key unique (oidc_issuer, oidc_subject)
);

create table user_profiles (
    user_id           uuid primary key references app_users (id) on delete cascade,
    first_name        text not null,
    last_name         text not null,
    -- Unique: one account per phone number.
    phone_e164        text not null unique,
    phone_verified_at timestamptz not null,
    age_confirmed_at  timestamptz not null,
    language          text not null default 'ro' check (language in ('ro', 'en')),
    -- Kept equal to the sum of points_ledger, updated in the same transaction.
    points_balance    integer not null default 0 check (points_balance >= 0)
);

create table device_tokens (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null references app_users (id) on delete cascade,
    platform     text not null check (platform in ('android', 'ios')),
    token        text not null unique,
    created_at   timestamptz not null default now(),
    last_used_at timestamptz
);

-- Supports the cascade when an account is deleted.
create index device_tokens_user_id_idx on device_tokens (user_id);

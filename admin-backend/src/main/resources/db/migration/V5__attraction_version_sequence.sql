-- US-11.1: one version sequence for all attractions. Every change takes the next value,
-- so the tourist backend can ask for everything changed since the version it last saw.
create sequence attraction_version_seq;

-- Start after any version already stored.
select setval('attraction_version_seq', coalesce((select max(version) from attractions), 0) + 1, false);

-- Used by the "changed since" query of the internal API.
create index attractions_version_idx on attractions (version);

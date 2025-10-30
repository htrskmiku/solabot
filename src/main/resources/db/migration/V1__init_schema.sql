-- PJSK ID 关联表
create table if not exists t_pjsk_binding
(
    id            bigserial   primary key,
    pjsk_id       varchar(64) not null,
    user_id       bigint      not null,
    group_id      bigint,
    server_region CHAR(2)     NOT NULL CHECK (server_region IN ('cn', 'jp', 'tw', 'kr', 'en', 'xx')),
    created_at    timestamptz default now(),
    updated_at    timestamptz default now(),
    unique (pjsk_id, user_id, group_id)
);
create index if not exists idx_pjsk_binding_pjsk on t_pjsk_binding (pjsk_id);
create index if not exists idx_pjsk_binding_user on t_pjsk_binding (user_id);
create index if not exists idx_pjsk_binding_group on t_pjsk_binding (group_id);

-- Live 主播别名映射表
create table if not exists t_streamer_alias
(
    id            bigserial   primary key,
    streamer_id   bigint      not null,
    stream_id     bigint      not null,
    alias         varchar(32) not null,
    created_at    timestamptz default now(),
    updated_at    timestamptz default now(),
    unique (streamer_id, stream_id)
);
create index if not exists idx_streamer_alias_streamer on t_streamer_alias (streamer_id);
create index if not exists idx_streamer_alias_stream on t_streamer_alias (stream_id);
create index if not exists idx_streamer_alias_alias on t_streamer_alias (alias);

-- Live 推送订阅表
create table if not exists t_streamer_subscription
(
    id            bigserial primary key,
    user_id       bigint      not null,
    group_id      bigint,
    stream_id     bigint      not null,
    created_at    timestamptz default now(),
    updated_at    timestamptz default now(),
    unique (user_id, group_id, stream_id)
);
create index if not exists idx_subscription_user on t_streamer_subscription (user_id);
create index if not exists idx_subscription_group on t_streamer_subscription (group_id);
create index if not exists idx_subscription_stream on t_streamer_subscription (stream_id);

-- //============++++** 基本表 **++++============//

-- -- 基本用户表
-- create table if not exists t_user
-- (
--     id         bigint      primary key,
--     platform   varchar(16) not null default 'qq',
--     open_id    bigint      not null,
--     nickname   varchar(64),
--     avatar_url text,
--     locale     varchar(16)          default 'zh-CN',
--     timezone   varchar(64)          default 'Asia/Shanghai',
--     status     varchar(16)          default 'active',
--     created_at timestamptz          default now(),
--     updated_at timestamptz          default now(),
--     unique (platform, open_id)
-- );
-- create index if not exists idx_user_open on t_user (platform, open_id);
--
-- -- 基本群表
-- create table if not exists t_group
-- (
--     id            bigint      primary key,
--     platform      varchar(16) not null default 'qq',
--     open_id       bigint      not null,
--     name          varchar(128),
--     owner_open_id bigint,
--     status        varchar(16)          default 'active',
--     created_at    timestamptz          default now(),
--     updated_at    timestamptz          default now(),
--     unique (platform, open_id)
-- );
-- create index if not exists idx_group_open on t_group (platform, open_id);
--
-- -- 基本成员关系
-- create table if not exists t_membership
-- (
--     id           bigserial primary key,
--     user_id      bigint not null references t_user (id) on delete cascade,
--     group_id     bigint not null references t_group (id) on delete cascade,
--     role         varchar(16) default 'member',
--     note_name    varchar(64),
--     joined_at    timestamptz default now(),
--     last_seen_at timestamptz,
--     unique (user_id, group_id)
-- );
-- create index if not exists idx_membership_group on t_membership (group_id);
-- create index if not exists idx_membership_user on t_membership (user_id);
--
-- -- 订阅作用域枚举
-- do
-- $$
--     begin
--         if not exists (select 1 from pg_type where typname = 'subscription_scope') then
--             create type subscription_scope as enum ('user','group');
--         end if;
--     end
-- $$;
--
-- -- 基本订阅/功能开关
-- create table if not exists t_subscription
-- (
--     id            bigserial primary key,
--     scope         subscription_scope not null,
--     user_id       bigint references t_user (id),
--     group_id      bigint references t_group (id),
--     service_key   varchar(64)        not null,
--     params        jsonb              not null default '{}'::jsonb,
--     schedule_cron varchar(64),
--     enabled       boolean            not null default true,
--     created_at    timestamptz                 default now(),
--     updated_at    timestamptz                 default now(),
--     unique (scope, user_id, group_id, service_key),
--     check (
--         (scope = 'user' and user_id is not null and group_id is null)
--             or
--         (scope = 'group' and group_id is not null and user_id is null)
--         )
-- );
-- create index if not exists idx_sub_user on t_subscription (user_id) where user_id is not null;
-- create index if not exists idx_sub_group on t_subscription (group_id) where group_id is not null;
-- create index if not exists idx_sub_service on t_subscription (service_key);

-- 初始化 t_pjsk_binding 表数据
INSERT INTO t_pjsk_binding (pjsk_id, user_id, group_id, server_region, created_at, updated_at)
VALUES
    ('7485938033569569588', 1256977415, 619096416, 'cn', NOW(), NOW()),
    ('7445096955522390818', 1685280357, 619096416, 'cn', NOW(), NOW()),
    ('7487212719486049063', 1828209434, 619096416, 'cn', NOW(), NOW()),
    ('7486314772426939173', 984097301, 619096416, 'cn', NOW(), NOW()),
    ('7489244575534537481', 1461762986, 619096416, 'cn', NOW(), NOW()),
    ('7485918807991769867', 1935545201, 619096416, 'cn', NOW(), NOW()),
    ('7485918807991769867', 1935545201, 992406250, 'cn', NOW(), NOW()),
    ('7503172168113658687', 2462516428, 619096416, 'cn', NOW(), NOW()),
    ('7485918725925935911', 2660564229, 619096416, 'cn', NOW(), NOW()),
    ('7486453954226772745', 1355343505, 619096416, 'cn', NOW(), NOW()),
    ('7445607211377793833', 3468684112, 619096416, 'cn', NOW(), NOW()),
    ('123', 1093664084, 619096416, 'cn', NOW(), NOW()),
    ('123', 1093664084, 793709714, 'cn', NOW(), NOW())
    ON CONFLICT (pjsk_id, user_id, group_id)
DO NOTHING;

-- 初始化 t_streamer_alias 表数据
INSERT INTO t_streamer_alias (streamer_id, stream_id, alias, created_at, updated_at)
VALUES
    (299013902, 84074, '炫狗', NOW(), NOW())
    ON CONFLICT (streamer_id, stream_id)
DO NOTHING;

-- 初始化 t_streamer_subscription 表数据
INSERT INTO t_streamer_subscription (user_id, group_id, stream_id, created_at, updated_at)
VALUES
    (1093664084, 793709714, 7734200, NOW(), NOW()),
    (1093664084, 1034714395, 7734200, NOW(), NOW())
    ON CONFLICT (user_id, group_id, stream_id)
DO NOTHING;
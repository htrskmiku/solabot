package com.arth.bot.core.cache.service;

import com.arth.bot.core.common.exception.ExternalServiceErrorException;
import com.arth.bot.core.common.exception.InternalServerErrorException;
import com.arth.bot.core.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * GalleryCacheService
 *
 * 说明：
 * - 该服务不在本地持久化画廊图片，依赖远端 API 提供图片及元数据
 * - 采用 Redis 缓存元数据：hash (gallery:{role}:pics) + zset (gallery:{role}:pids)
 * - 采用 per-role 分布式锁防止缓存击穿
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final long TTL_HOURS = 6L;  // HOURS
    private static final String GALLERY_GLOBAL_MARKER = "gallery:cache"; // 全量更新标志（带 TTL）
    private static final String GALLERY_LOCK_PREFIX = "gallery:lock:";   // per-role lock 前缀

    private static final String GET_PIC_METADATA_API = "https://bot.teaphenby.com/api/galleries";
    private static final String BASE_GET_PIC_API = "https://bot.teaphenby.com/api/gallery/";
    private static final String AUTH_TOKEN = "arthur-stat";

    // 剩余过期时间大于该值时，不更新
    private static final long REFRESH_THRESHOLD_SECONDS = 300L;  // 5 minutes

    // 锁持续时间
    private static final long LOCK_BASE_SECONDS = 30L;

    public byte[] getPic(String role, String pid) {
        tryUpdateGalleryCache();
        String hashKey = "gallery:" + role + ":pics";
        Object picJson = redisTemplate.opsForHash().get(hashKey, pid);

        if (picJson == null) {
            try {
                // 如果没拿到缓存，短暂重试以等待可能正在更新的线程完成更新
                Thread.sleep(120L);
            } catch (InterruptedException ignored) {}
            picJson = redisTemplate.opsForHash().get(hashKey, pid);
            if (picJson == null) {
                throw new ResourceNotFoundException("pic metadata not found for role=" + role + ", pid=" + pid);
            }
        }

        Map<String, Object> picMeta;
        try {
            picMeta = objectMapper.readValue(picJson.toString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException("failed to deserialize pic metadata for pid=" + pid);
        }

        Object pathObj = picMeta.get("path");
        if (pathObj == null) {
            throw new InternalServerErrorException("Missing 'path' in metadata for pid=" + pid);
        }
        String path = pathObj.toString().trim();
        String url = BASE_GET_PIC_API + path;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + AUTH_TOKEN);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new ExternalServiceErrorException("failed to fetch image from remote API: " + url);
            }
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("error fetching image {}: {}", url, e.getMessage());
            throw new ExternalServiceErrorException("error fetching image from remote API: " + url);
        }
    }

    /**
     * 尝试更新整个画廊缓存（注意：远端 API 返回的是全量数据，但我们在本地以 role 为单位分别缓存/过期/加锁）
     *
     * 锁策略：
     * - 先检查全局标志 GALLERY_GLOBAL_MARKER 的 TTL（如果存在且 >阈值，则不刷新）
     * - 拉取远端全量数据后，按 role 分别更新各自的 hash/zset，并在每个 role 上设置独立随机 TTL
     * - 每个 role 更新时使用 per-role 锁（带 token）并用 compare-and-delete 的方式释放锁，避免误删
     * - 更新完成后设置 GALLERY_GLOBAL_MARKER 的 TTL（作为全局短期标志，便于快速判断是否需要刷新）
     */
    public void tryUpdateGalleryCache() {
        try {
            // 过期时间大于阈值，不更新
            Long globalTtl = redisTemplate.getExpire(GALLERY_GLOBAL_MARKER);
            if (globalTtl != null && globalTtl > REFRESH_THRESHOLD_SECONDS) {
                return;
            }
        } catch (Exception e) {
            log.warn("failed to read global marker TTL, will attempt refresh: {}", e.getMessage());
            // continue
        }

        log.info("[core.cache] find gallery cache expired, try to update");

        // 拉取远端数据（实际上 luna茶 提供的 api 是全量的，我们并不能只更新指定 role）
        Map<String, Map<String, Object>> galleries;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + AUTH_TOKEN);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Map<String, Object>>> response = restTemplate.exchange(
                    GET_PIC_METADATA_API, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<>() {});
            galleries = response.getBody();
            if (galleries == null || galleries.isEmpty()) {
                log.warn("remote gallery API returned empty body");
                return;
            }
        } catch (Exception e) {
            log.error("failed to fetch gallery metadata from remote API: {}", e.getMessage());
            return;
        }

        // 对每个 role 分别尝试更新（并发下每个 role 用独立锁）
        for (Map.Entry<String, Map<String, Object>> roleEntry : galleries.entrySet()) {
            String role = roleEntry.getKey();
            if (role == null) continue;
            Object picsObj = roleEntry.getValue().get("pics");
            if (!(picsObj instanceof List)) continue;
            List<Map<String, Object>> pics = (List<Map<String, Object>>) picsObj;

            String lockKey = GALLERY_LOCK_PREFIX + role;
            String lockToken = UUID.randomUUID().toString();
            long lockTtlSeconds = LOCK_BASE_SECONDS + ThreadLocalRandom.current().nextLong(10, 20);

            Boolean acquired = false;
            try {
                // 尝试获取锁（如果没拿到，跳过当前 role 的更新；但不影响其它 role）
                acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(lockTtlSeconds));
                if (!Boolean.TRUE.equals(acquired)) {
                    // 如果锁未获取，跳过更新（可能其他实例正在更新该 role）
                    continue;
                }

                // 清除旧数据 hash 和 zset
                String hashKey = "gallery:" + role + ":pics";
                String sortedSetKey = "gallery:" + role + ":pids";
                try {
                    Set<String> oldKeys = new HashSet<>();
                    oldKeys.add(hashKey);
                    oldKeys.add(sortedSetKey);
                    redisTemplate.delete(oldKeys);
                } catch (Exception e) {
                    log.warn("failed to delete old keys for role {}: {}", role, e.getMessage());
                    // 继续执行写入，避免中断
                }

                // 写入新数据：使用 pipeline 批量写入以提升性能
                final String finalHashKey = hashKey;
                final String finalSortedSetKey = sortedSetKey;

                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    StringRedisSerializer serializer = (StringRedisSerializer) redisTemplate.getStringSerializer();
                    for (Map<String, Object> pic : pics) {
                        Object pidObj = pic.get("pid");
                        if (pidObj == null) continue;
                        String pid = pidObj.toString();
                        try {
                            String picJson = objectMapper.writeValueAsString(pic);
                            // HSET hashKey pid picJson
                            connection.hSet(serializer.serialize(finalHashKey), serializer.serialize(pid),
                                    serializer.serialize(picJson));
                        } catch (JsonProcessingException e) {
                            // 序列化失败时跳过该项
                            log.warn("serialize pic error for role={}, pid={}, cause={}", role, pid, e.getMessage());
                        }

                        // ZADD sortedSetKey score pid
                        try {
                            double score = Double.parseDouble(pid);
                            connection.zAdd(serializer.serialize(finalSortedSetKey),
                                    score,
                                    serializer.serialize(pid));
                        } catch (NumberFormatException nfe) {
                            // 若 pid 不是数字，使用 0 作为 score，仍然存入（应该不会出现这个情况）
                            connection.zAdd(serializer.serialize(finalSortedSetKey), 0.0, serializer.serialize(pid));
                        }
                    }
                    return null;
                });

                // 为两个 key 设置随机化 TTL，防止所有 role 同步过期
                long extraSeconds = ThreadLocalRandom.current().nextLong(300, 1800); // 5~30 min
                long ttlSeconds = TimeUnit.HOURS.toSeconds(TTL_HOURS) + extraSeconds;
                try {
                    redisTemplate.expire(finalHashKey, ttlSeconds, TimeUnit.SECONDS);
                    redisTemplate.expire(finalSortedSetKey, ttlSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("failed to set TTL for role {} keys: {}", role, e.getMessage());
                }

            } finally {
                // 释放锁（比较 token 后删除，防止误删其他锁）
                if (Boolean.TRUE.equals(acquired)) {
                    try {
                        // 使用 RedisCallback 做一次原子比较并删除（GET + DEL）
                        redisTemplate.execute((RedisCallback<Object>) connection -> {
                            byte[] key = redisTemplate.getStringSerializer().serialize(lockKey);
                            byte[] val = connection.get(key);
                            if (val != null) {
                                String current = redisTemplate.getStringSerializer().deserialize(val);
                                if (lockToken.equals(current)) {
                                    connection.del(key);
                                }
                            }
                            return null;
                        });
                    } catch (Exception e) {
                        log.warn("failed to release lock {} safely: {}", lockKey, e.getMessage());
                    }
                }
            }
        }

        // 全部角色更新完毕后，设置全局标志键并加 TTL，作为快速检查的依据
        try {
            long globalExtra = ThreadLocalRandom.current().nextLong(60, 600); // 1~10 min
            long globalTtlSeconds = TimeUnit.HOURS.toSeconds(TTL_HOURS) + globalExtra;
            // 设置为简单的字符串并设置 TTL；该键仅用于快速判定是否需要再次刷新
            redisTemplate.opsForValue().set(GALLERY_GLOBAL_MARKER, String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(GALLERY_GLOBAL_MARKER, globalTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("failed to set global gallery marker TTL: {}", e.getMessage());
        }

        log.info("[core.cache] updated gallery cache successfully");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {

        }
    }

    /**
     * 将指定 pid 的元数据 URL 返回（辅助方法内部使用）
     */
    public String getPicUrlByPid(String role, String pid) {
        Map<String, Object> metadata = getPicMetadataByPid(role, pid);
        Object path = metadata.get("path");
        if (path == null) {
            throw new ExternalServiceErrorException("path not found for pid: " + pid);
        }
        return BASE_GET_PIC_API + path;
    }

    public String getRandomPicUrl(String role) {
        Map<String, Object> metadata = getRandomPicMetadata(role);
        Object path = metadata.get("path");
        if (path == null) {
            throw new ExternalServiceErrorException("path not found in random pic metadata");
        }
        return BASE_GET_PIC_API + path;
    }

    public Map<String, Object> getPicMetadataByPid(String role, String pid) {
        tryUpdateGalleryCache();
        String hashKey = "gallery:" + role + ":pics";
        Object picJson = redisTemplate.opsForHash().get(hashKey, pid);
        if (picJson == null) {
            // 再短暂等待并重试一次，防止刚好被其他线程刷新中
            try {
                Thread.sleep(120L);
            } catch (InterruptedException ignored) {}
            picJson = redisTemplate.opsForHash().get(hashKey, pid);
            if (picJson == null) throw new ResourceNotFoundException("gallery pics not found for role=" + role + ", pid=" + pid);
        }

        try {
            return objectMapper.readValue(picJson.toString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException("failed to deserialize pic data");
        }
    }

    public Map<String, Object> getRandomPicMetadata(String role) {
        String pid = getRandomPid(role);
        return getPicMetadataByPid(role, pid);
    }

    public String getRandomPid(String role) {
        tryUpdateGalleryCache();
        String sortedSetKey = "gallery:" + role + ":pids";
        Long size = redisTemplate.opsForZSet().size(sortedSetKey);
        if (size == null || size == 0) {
            throw new ResourceNotFoundException("Gallery not found for role: " + role);
        }

        // 优先尝试 zRandMember（Redis 6.2+）通过 low-level connection
        try {
            String maybe = redisTemplate.execute((RedisCallback<String>) connection -> {
                byte[] keyBytes = redisTemplate.getStringSerializer().serialize(sortedSetKey);
                if (keyBytes == null) return null;
                try {
                    byte[] member = connection.zRandMember(keyBytes);
                    if (member == null) return null;
                    return redisTemplate.getStringSerializer().deserialize(member);
                } catch (UnsupportedOperationException | AbstractMethodError e) {
                    return null;
                }
            });
            if (maybe != null) return maybe;
        } catch (Exception ignore) {}

        // 回退：按索引随机取
        long idx = ThreadLocalRandom.current().nextLong(size);
        Set<String> result = redisTemplate.opsForZSet().range(sortedSetKey, idx, idx);
        if (result == null || result.isEmpty()) throw new ResourceNotFoundException("no pid found for role: " + role);
        return result.iterator().next();
    }

    public List<Long> getPidList(String role) {
        tryUpdateGalleryCache();
        String sortedSetKey = "gallery:" + role + ":pids";
        Set<String> pids = redisTemplate.opsForZSet().range(sortedSetKey, 0, -1);
        if (pids == null || pids.isEmpty()) throw new ResourceNotFoundException("gallery role not found");

        List<Long> list = new ArrayList<>(pids.size());
        for (String pidStr : pids) {
            if (pidStr == null) continue;
            try {
                list.add(Long.parseLong(pidStr));
            } catch (NumberFormatException e) {
                throw new InternalServerErrorException("invalid pid format stored in Redis: " + pidStr);
            }
        }
        return list;
    }

    public void forceClearCache() {
        try {
            log.info("[core.cache] force clearing all gallery caches");

            Set<String> keys = redisTemplate.keys("gallery:*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[core.cache] cleared {} gallery cache keys", keys.size());
            } else {
                log.info("[core.cache] no gallery cache keys found to clear");
            }
        } catch (Exception e) {
            log.error("[core.cache] failed to clear gallery cache: {}", e.getMessage(), e);
            throw new InternalServerErrorException("failed to clear gallery cache");
        }
    }

    public void forceRefreshCache() {
        log.info("[core.cache] force refreshing gallery cache...");
        forceClearCache();
        tryUpdateGalleryCache();
    }
}

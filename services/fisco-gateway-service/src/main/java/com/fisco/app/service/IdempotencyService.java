package com.fisco.app.service;

import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 幂等性服务
 * 使用内存缓存存储已处理的幂等键
 * 注意：重启服务后会重置，生产环境建议使用 Redis 替代
 */
@Service
public class IdempotencyService {

    private static final long DEFAULT_TTL_SECONDS = 3600; // 1小时

    private final ConcurrentHashMap<String, IdempotencyRecord> cache = new ConcurrentHashMap<>();

    /**
     * 检查幂等键是否已存在
     */
    public boolean exists(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return false;
        }
        IdempotencyRecord record = cache.get(idempotencyKey);
        if (record == null) {
            return false;
        }
        // 检查是否过期
        if (isExpired(record)) {
            cache.remove(idempotencyKey);
            return false;
        }
        return true;
    }

    /**
     * 检查幂等键对应的操作是否成功
     */
    public boolean isSuccess(String idempotencyKey) {
        IdempotencyRecord record = cache.get(idempotencyKey);
        return record != null && record.success;
    }

    /**
     * 获取幂等键对应的交易哈希
     */
    public String getTxHash(String idempotencyKey) {
        IdempotencyRecord record = cache.get(idempotencyKey);
        return record != null ? record.txHash : null;
    }

    /**
     * 记录成功处理的幂等键
     */
    public void markSuccess(String idempotencyKey, String txHash) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return;
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.success = true;
        record.txHash = txHash;
        record.createTime = LocalDateTime.now();
        record.expireTime = record.createTime.plusSeconds(DEFAULT_TTL_SECONDS);
        cache.put(idempotencyKey, record);
    }

    /**
     * 记录失败处理的幂等键（允许重试）
     */
    public void markFailure(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return;
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.success = false;
        record.createTime = LocalDateTime.now();
        record.expireTime = record.createTime.plusSeconds(DEFAULT_TTL_SECONDS);
        cache.put(idempotencyKey, record);
    }

    /**
     * 移除幂等键
     */
    public void remove(String idempotencyKey) {
        if (idempotencyKey != null) {
            cache.remove(idempotencyKey);
        }
    }

    private boolean isExpired(IdempotencyRecord record) {
        return record.expireTime != null && LocalDateTime.now().isAfter(record.expireTime);
    }

    private static class IdempotencyRecord {
        boolean success;
        String txHash;
        LocalDateTime createTime;
        LocalDateTime expireTime;
    }
}

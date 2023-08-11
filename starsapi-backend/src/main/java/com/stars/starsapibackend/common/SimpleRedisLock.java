package com.stars.starsapibackend.common;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis乐观锁
 * 用于实现基于Redis的乐观锁，以确保在多线程环境中对共享资源的安全访问。
 * 使用方法：
 * 1. 创建SimpleRedisLock对象，传入StringRedisTemplate和锁的名称。
 * 2. 调用tryLock方法尝试获取锁，如果成功返回true，否则返回false。
 * 3. 在获取锁后，执行需要加锁的代码。
 * 4. 执行完成后，调用unlock方法释放锁。
 * 锁的名称由参数name决定，确保在不同的业务场景中使用不同的名称以避免冲突。
 * 使用线程ID作为锁的标识，确保每个线程只能释放自己持有的锁。
 *
 * @author stars
 */
public class SimpleRedisLock implements ILock {

    private StringRedisTemplate stringRedisTemplate;

    private String name;

    private static final String KEY_PREFIX = "lock:";

    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    /**
     * 尝试获取锁
     *
     * @param timeoutSec 超时时间（秒）
     * @return 如果成功获取锁，返回true，否则返回false
     */
    public boolean tryLock(long timeoutSec) {
        // 获取线程id标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = this.stringRedisTemplate
                .opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return success;
    }

    /**
     * 释放锁
     */
    public void unlock() {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if (threadId.equals(id))
            stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}

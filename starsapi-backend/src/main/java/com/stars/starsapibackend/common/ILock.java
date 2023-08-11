package com.stars.starsapibackend.common;

/**
 * ILOK锁接口
 * 用于定义一种基本的锁接口，提供了尝试锁定和解锁的操作。
 *
 * @author stars
 */
public interface ILock {

    /**
     * 尝试锁定资源
     *
     * @param paramLong 超时时间（毫秒），如果在指定时间内无法获取锁，则返回false
     * @return 如果成功获取锁，返回true，否则返回false
     */
    boolean tryLock(long paramLong);

    /**
     * 解锁资源
     */
    void unlock();
}

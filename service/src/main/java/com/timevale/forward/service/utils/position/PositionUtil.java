package com.timevale.forward.service.utils.position;

import com.timevale.base.elock.Elock;
import com.timevale.base.elock.LockFactory;
import com.timevale.base.elock.exception.LockNotAcquiredException;
import com.timevale.framework.tedis.util.TedisUtil;

import java.util.concurrent.TimeUnit;

/**
 * 唯一编号生成工具类
 */
public class PositionUtil {

    // redis key前缀
    private static final String REDIS_KEY_PREFIX = "FORWARD:POSITION:";
    // redis自增数最大值
    private static final int MAX_INCR = 999;
    // redis过期时间（秒）
    private static final int EXPIRE_SECONDS = 60; // 1分钟
 
    /**
     * 生成唯一编号：当前时间戳+3位自增数
     * @param key 业务唯一key（如业务类型）
     * @return 唯一编号字符串
     * @throws Exception 
     * @throws LockNotAcquiredException 
     */
    public static double generate(String key, long timestamp) throws LockNotAcquiredException, Exception {
        String redisKey = REDIS_KEY_PREFIX + key + ":" +timestamp;
        String lockKey = "lock:" + redisKey;
        // 分布式锁伪代码，实际请替换为elock实现
        Elock reentrantLock = LockFactory.getReentrantLock(lockKey);
        return reentrantLock.lockAndProtect(30, () -> {
            // 获取当前自增数   
            Integer incr = TedisUtil.get(redisKey);
            if (incr == null || incr >= MAX_INCR) {
                incr = 1;
            } else {
                incr++;
            }
            // 存回redis并设置过期
            TedisUtil.set(redisKey, incr, EXPIRE_SECONDS, TimeUnit.SECONDS);
            // 拼接当前时间戳和3位自增数（字符串拼接）
            return timestamp*1000+ incr;
        });
    }
} 
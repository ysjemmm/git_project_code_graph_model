package com.timevale.forward.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author by YangXu
 * @date 2022/01/27 15:20
 */
@Configuration
@EnableAsync
public class ThreadPoolExecutorConfig {
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 1000;

    public ThreadPoolExecutorConfig() {
    }

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        return initThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 200, 30000, "ThreadPoolExecutor-");
    }

    @Bean(name = "trackImportExecutor")
    public ThreadPoolTaskExecutor trackImportExecutor() {
        return initThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 200, 30000, "TrackImportExecutor-");
    }

    public static ThreadPoolTaskExecutor initThreadPoolExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, int keepAliveSeconds, String threadNamePrefix) {
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();
        poolTaskExecutor.setCorePoolSize(corePoolSize);
        poolTaskExecutor.setMaxPoolSize(maxPoolSize);
        poolTaskExecutor.setQueueCapacity(queueCapacity);
        poolTaskExecutor.setKeepAliveSeconds(keepAliveSeconds);
        poolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
        poolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        poolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        return poolTaskExecutor;
    }
}

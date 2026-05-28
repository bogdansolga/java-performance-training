package net.safedata.performance.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolsConfig {

    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    @Bean
    @Primary
    public ThreadPoolTaskExecutor shortExecTimeThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(CORE_POOL_SIZE * 2);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(5);
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor longExecTimeThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(CORE_POOL_SIZE * 2);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(20);
        return executor;
    }
}

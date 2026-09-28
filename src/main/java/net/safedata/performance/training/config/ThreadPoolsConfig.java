package net.safedata.performance.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolsConfig {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    @Bean
    @Primary
    public ThreadPoolTaskExecutor shortExecTimeThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(AVAILABLE_PROCESSORS / 2);
        executor.setMaxPoolSize(AVAILABLE_PROCESSORS * 2);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(5);
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor longExecTimeThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(AVAILABLE_PROCESSORS);
        executor.setMaxPoolSize(AVAILABLE_PROCESSORS * 2);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(20);
        return executor;
    }
}

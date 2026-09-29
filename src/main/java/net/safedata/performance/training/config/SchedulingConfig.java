package net.safedata.performance.training.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
@ConditionalOnBooleanProperty(name = "scheduled.beans.enabled", havingValue = true)
public class SchedulingConfig {

    private static final int PROCESSORS_COUNT = Runtime.getRuntime().availableProcessors();

    @Primary
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        final ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

        // Math.max floors this at 1: on a 1-CPU container availableProcessors() returns 1,
        // 1 / 2 == 0, and ThreadPoolTaskScheduler rejects a pool size below 1 — the app then
        // fails to start, with the container still reporting Running.
        threadPoolTaskScheduler.setPoolSize(Math.max(1, PROCESSORS_COUNT / 2));
        threadPoolTaskScheduler.setThreadGroupName("scheduled-thread-pool-");
        threadPoolTaskScheduler.setThreadNamePrefix("scheduled-thread-");
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setAwaitTerminationSeconds(10);
        threadPoolTaskScheduler.setRemoveOnCancelPolicy(true);
        threadPoolTaskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskScheduler.initialize();

        return threadPoolTaskScheduler;
    }
}

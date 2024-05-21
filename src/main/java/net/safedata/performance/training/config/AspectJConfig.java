package net.safedata.performance.training.config;

import net.safedata.performance.training.aop.aspect.LoggingAspect;
import net.safedata.performance.training.aop.aspect.ProfilingAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AspectJConfig {

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    @Bean
    public ProfilingAspect profiler() {
        return new ProfilingAspect();
    }
}

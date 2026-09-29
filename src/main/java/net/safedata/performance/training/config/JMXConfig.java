package net.safedata.performance.training.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Profile;

// Lab contexts skip the demo MBean export, so several contexts (e.g. integration tests) can share one JVM without clashing on its fixed JMX name.
@Profile("!lab")
@Configuration
@EnableMBeanExport
public class JMXConfig {
}

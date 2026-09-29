package net.safedata.performance.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProfilingDemoApplication {

	public static void main(String[] args) {
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2");
		//System.setProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares", "3");
		System.setProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler",
				"net.safedata.performance.training.error.CustomExceptionHandler");

		SpringApplication springApplication = new SpringApplication(ProfilingDemoApplication.class);
		// Activate one issue for a demo, e.g. setAdditionalProfiles(RunProfiles.UNBOUNDED_RETENTION)
		springApplication.setAdditionalProfiles();
		springApplication.run(args);
	}
}

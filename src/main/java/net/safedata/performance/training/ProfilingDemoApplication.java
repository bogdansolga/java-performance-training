package net.safedata.performance.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProfilingDemoApplication {

	public static void main(String[] args) {
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "8");
		System.setProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares", "10");
		System.setProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler",
				"net.safedata.performance.training.error.CustomExceptionHandler");

		SpringApplication.run(ProfilingDemoApplication.class, args);
	}
}

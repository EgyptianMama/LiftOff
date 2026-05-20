package dev.liftoff.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async and scheduling configuration.
 * <p>
 * The "buildExecutor" bean limits concurrent builds to 2
 * so we don't overwhelm the server. Additional builds queue
 * up (max 20 in queue) and wait for a slot.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "buildThreadPool")
    public Executor buildThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("build-");
        executor.setRejectedExecutionHandler((r, e) -> {
            throw new RuntimeException("Build queue is full. Please try again later.");
        });
        executor.initialize();
        return executor;
    }

}

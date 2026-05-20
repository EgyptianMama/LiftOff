package dev.liftoff.platform.build.service;

import dev.liftoff.platform.project.entity.Build;
import dev.liftoff.platform.project.repository.BuildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BuildOrchestrator {

    private final BuildRepository buildRepository;
    private final BuildExecutor buildExecutor;

    /**
     * Polls the database every 10 seconds for QUEUED builds and dispatches them to the executor.
     */
    @Scheduled(fixedDelayString = "10000")
    @Transactional
    public void pollQueuedBuilds() {
        // Find QUEUED builds. In a real system, you'd use a SELECT FOR UPDATE skip locked or an event queue.
        // For our MVP, we query a small batch.
        // We'll iterate all QUEUED builds and dispatch them. The thread pool will queue them internally.
        buildRepository.findAll().stream()
                .filter(b -> "QUEUED".equals(b.getStatus()))
                .forEach(build -> {
                    log.info("Found QUEUED build {}, dispatching to executor", build.getId());
                    // Temporarily set to 'DISPATCHED' or let executor set to 'RUNNING' immediately
                    // to prevent double processing on next poll if thread pool queue is full.
                    build.setStatus("STARTING");
                    buildRepository.save(build);
                    
                    buildExecutor.executeBuild(build.getId());
                });
    }
}

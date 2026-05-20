package dev.liftoff.platform.build.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class LogStreamingService {

    // Map of buildId -> List of SseEmitters
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID buildId) {
        // 30 minute timeout for SSE
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        emitters.computeIfAbsent(buildId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> removeEmitter(buildId, emitter));
        emitter.onTimeout(() -> removeEmitter(buildId, emitter));
        emitter.onError((e) -> removeEmitter(buildId, emitter));

        try {
            // Send an initial event to establish connection
            emitter.send(SseEmitter.event().name("init").data("Connected to log stream for build " + buildId));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcastLog(UUID buildId, String message) {
        CopyOnWriteArrayList<SseEmitter> buildEmitters = emitters.get(buildId);
        if (buildEmitters != null) {
            for (SseEmitter emitter : buildEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("log").data(message));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        }
    }

    public void completeStream(UUID buildId) {
        CopyOnWriteArrayList<SseEmitter> buildEmitters = emitters.remove(buildId);
        if (buildEmitters != null) {
            for (SseEmitter emitter : buildEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("complete").data("Build finished"));
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("Failed to complete emitter for build {}", buildId, e);
                }
            }
        }
    }

    private void removeEmitter(UUID buildId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> buildEmitters = emitters.get(buildId);
        if (buildEmitters != null) {
            buildEmitters.remove(emitter);
            if (buildEmitters.isEmpty()) {
                emitters.remove(buildId);
            }
        }
    }
}

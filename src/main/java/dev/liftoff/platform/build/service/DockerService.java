package dev.liftoff.platform.build.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import dev.liftoff.platform.common.exception.LiftoffException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;

@Slf4j
@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final LogStreamingService logStreamingService;

    public DockerService(LogStreamingService logStreamingService) {
        this.logStreamingService = logStreamingService;
        
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
                
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @PreDestroy
    public void cleanup() {
        try {
            dockerClient.close();
        } catch (Exception e) {
            log.error("Failed to close docker client", e);
        }
    }

    /**
     * Pulls the image if not present, creates a container binding the workspace directory,
     * executes the build command, captures logs, and returns the exit code.
     */
    public int runBuildContainer(java.util.UUID buildId, File workspaceDir, String command) {
        String image = "node:20-alpine";
        
        try {
            logStreamingService.broadcastLog(buildId, "Pulling docker image " + image + "...");
            dockerClient.pullImageCmd(image).start().awaitCompletion();
            
            // Map the host workspace path to /app in the container
            String hostPath = workspaceDir.getAbsolutePath();
            // On Windows, Docker Desktop requires path translation or it handles standard absolute paths
            
            Volume volume = new Volume("/app");
            Bind bind = new Bind(hostPath, volume);
            
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(bind)
                    .withMemory(1024L * 1024L * 1024L) // 1GB limit
                    .withCpuCount(1L);

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/app")
                    // Use sh -c to evaluate compound commands like "npm install && npm run build"
                    .withCmd("sh", "-c", command) 
                    .exec();

            String containerId = container.getId();
            logStreamingService.broadcastLog(buildId, "Starting container " + containerId + "...");
            
            dockerClient.startContainerCmd(containerId).exec();

            // Stream logs
            StringBuilder completeLog = new StringBuilder();
            
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTailAll()
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame item) {
                            String logLine = new String(item.getPayload()).trim();
                            if (!logLine.isEmpty()) {
                                completeLog.append(logLine).append("\n");
                                logStreamingService.broadcastLog(buildId, logLine);
                            }
                        }
                    }).awaitCompletion();

            // Wait for container to exit and get status
            com.github.dockerjava.api.command.WaitContainerResultCallback waitCallback = 
                    new com.github.dockerjava.api.command.WaitContainerResultCallback();
                    
            dockerClient.waitContainerCmd(containerId).exec(waitCallback);
            Integer statusCode = waitCallback.awaitStatusCode();
            
            logStreamingService.broadcastLog(buildId, "Container exited with code " + statusCode);

            // Cleanup container
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            
            return statusCode != null ? statusCode : -1;

        } catch (Exception e) {
            log.error("Docker build failed", e);
            logStreamingService.broadcastLog(buildId, "DOCKER ERROR: " + e.getMessage());
            throw new LiftoffException("Docker build failed: " + e.getMessage());
        }
    }
}

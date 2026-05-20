package dev.liftoff.platform.build.service;

import dev.liftoff.platform.project.entity.Build;
import dev.liftoff.platform.project.repository.BuildRepository;
import dev.liftoff.platform.proxy.entity.Deployment;
import dev.liftoff.platform.proxy.repository.DeploymentRepository;
import dev.liftoff.platform.proxy.service.CaddyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildExecutor {

    private final BuildRepository buildRepository;
    private final GitService gitService;
    private final FrameworkDetector frameworkDetector;
    private final DockerService dockerService;
    private final LogStreamingService logStreamingService;
    private final DeploymentRepository deploymentRepository;
    private final CaddyService caddyService;

    @Value("${liftoff.deployments.base-dir}")
    private String deploymentsBaseDir;

    @Async("buildThreadPool")
    @org.springframework.transaction.annotation.Transactional
    public void executeBuild(UUID buildId) {
        log.info("Starting execution for build {}", buildId);
        Build build = buildRepository.findById(buildId).orElse(null);
        if (build == null) {
            log.error("Build {} not found", buildId);
            return;
        }

        build.setStatus("RUNNING");
        build.setStartedAt(Instant.now());
        buildRepository.save(build);
        
        logStreamingService.broadcastLog(buildId, "Build process started at " + build.getStartedAt());

        // Temp workspace directory for this build
        String workspacePath = System.getProperty("java.io.tmpdir") + File.separator + "liftoff-builds" + File.separator + buildId.toString();
        File workspaceDir = new File(workspacePath);

        try {
            // 1. Clone repository
            logStreamingService.broadcastLog(buildId, "Cloning repository: " + build.getProject().getGithubRepoUrl());
            gitService.cloneRepository(build.getProject().getGithubRepoUrl(), build.getBranch(), workspaceDir);

            // 2. Detect framework
            FrameworkDetector.FrameworkInfo info = frameworkDetector.detect(workspaceDir.toPath());
            build.setFrameworkDetected(info.framework());
            build.setBuildCommand(info.buildCommand());
            build.setOutputDir(info.outputDir());
            buildRepository.save(build);
            
            logStreamingService.broadcastLog(buildId, "Detected framework: " + info.framework());
            
            // 3. Run Build if there's a build command
            if (info.buildCommand() != null && !info.buildCommand().isEmpty()) {
                logStreamingService.broadcastLog(buildId, "Running build command: " + info.buildCommand());
                int exitCode = dockerService.runBuildContainer(buildId, workspaceDir, info.buildCommand());
                
                if (exitCode != 0) {
                    throw new RuntimeException("Build command failed with exit code " + exitCode);
                }
            } else {
                logStreamingService.broadcastLog(buildId, "No build command required (static HTML).");
            }

            // 4. Finalize (Move artifacts)
            File outputDir = new File(workspaceDir, info.outputDir());
            if (!outputDir.exists()) {
                throw new RuntimeException("Expected output directory does not exist: " + info.outputDir());
            }
            
            // Move files to permanent deployments directory
            String deployDirName = UUID.randomUUID().toString();
            File deployDir = new File(deploymentsBaseDir, deployDirName);
            deployDir.mkdirs();
            
            logStreamingService.broadcastLog(buildId, "Moving artifacts to deployment directory...");
            copyDirectory(outputDir.toPath(), deployDir.toPath());
            
            // Create Deployment record
            Deployment deployment = new Deployment();
            deployment.setBuild(build);
            deployment.setProject(build.getProject());
            // Path inside Caddy container
            deployment.setArtifactPath("/srv/deployments/" + deployDirName);
            deployment.setLive(true);
            deployment.setUrl("http://" + build.getProject().getSubdomain() + ".localhost");
            
            deploymentRepository.markAllDeploymentsNotLiveForProject(build.getProject().getId());
            deploymentRepository.save(deployment);

            // Register with Caddy
            logStreamingService.broadcastLog(buildId, "Registering route with Caddy reverse proxy...");
            caddyService.registerSite(build.getProject().getSubdomain(), deployment.getArtifactPath());

            logStreamingService.broadcastLog(buildId, "Deployment successful! Live at " + deployment.getUrl());

            build.setStatus("SUCCESS");
            
        } catch (Exception e) {
            log.error("Build {} failed", buildId, e);
            build.setStatus("FAILED");
            logStreamingService.broadcastLog(buildId, "BUILD FAILED: " + e.getMessage());
        } finally {
            build.setFinishedAt(Instant.now());
            buildRepository.save(build);
            logStreamingService.completeStream(buildId);
            
            // Cleanup workspace
            deleteDirectory(workspaceDir);
        }
    }
    
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] allContents = dir.listFiles();
            if (allContents != null) {
                for (File file : allContents) {
                    deleteDirectory(file);
                }
            }
            dir.delete();
        }
    }
    
    private void copyDirectory(Path source, Path target) throws java.io.IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectory(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (java.io.IOException ex) {
                log.error("Failed to copy file", ex);
            }
        });
    }
}

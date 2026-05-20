package dev.liftoff.platform.build.service;

import dev.liftoff.platform.common.exception.LiftoffException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GitService {

    /**
     * Clones a git repository into the target directory.
     * Checks out the specified branch.
     */
    public void cloneRepository(String repoUrl, String branch, File targetDir) {
        log.info("Cloning {} (branch: {}) into {}", repoUrl, branch, targetDir.getAbsolutePath());
        
        // Clean up directory if it exists
        if (targetDir.exists()) {
            deleteDirectory(targetDir);
        }
        targetDir.mkdirs();

        try {
            // ProcessBuilder to run: git clone -b <branch> --single-branch <url> .
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "clone",
                    "-b", branch,
                    "--single-branch",
                    repoUrl,
                    "."
            );
            pb.directory(targetDir);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            
            if (!finished) {
                process.destroyForcibly();
                throw new LiftoffException("Git clone timed out");
            }
            
            if (process.exitValue() != 0) {
                // Read output for error message
                String output = new String(process.getInputStream().readAllBytes());
                log.error("Git clone failed: {}", output);
                throw new LiftoffException("Git clone failed with exit code " + process.exitValue());
            }
            
            log.info("Successfully cloned repo.");
        } catch (Exception e) {
            throw new LiftoffException("Failed to clone repository: " + e.getMessage());
        }
    }

    private void deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}

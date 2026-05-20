package dev.liftoff.platform.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "builds")
@Getter
@Setter
public class Build {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private String commitSha;
    
    @Column(columnDefinition = "TEXT")
    private String commitMessage;

    private String branch;

    @Column(nullable = false)
    private String status = "QUEUED"; // QUEUED, RUNNING, SUCCESS, FAILED, TIMEOUT

    private String frameworkDetected;
    private String buildCommand;
    private String outputDir;

    @Column(columnDefinition = "TEXT")
    private String logText;

    @Column(unique = true)
    private String githubDeliveryId;

    private Instant startedAt;
    private Instant finishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

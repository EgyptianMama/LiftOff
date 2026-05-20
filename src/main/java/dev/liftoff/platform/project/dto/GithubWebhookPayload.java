package dev.liftoff.platform.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubWebhookPayload(
        String ref,
        String after, // commit sha
        Repository repository,
        HeadCommit head_commit,
        List<Commit> commits
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(
            String name,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("clone_url") String cloneUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeadCommit(
            String id,
            String message
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(
            String id,
            String message
    ) {}
}

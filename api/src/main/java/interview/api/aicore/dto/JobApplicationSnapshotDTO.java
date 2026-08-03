package interview.api.aicore.dto;

public record JobApplicationSnapshotDTO(
        Long enterpriseId,
        Long jobId,
        Long candidateId
) {
}
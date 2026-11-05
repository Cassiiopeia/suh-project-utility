package me.suhsaechan.grassplanter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub 커밋 결과: 성공 여부, 실패 사유 (성공 시 사유 없음)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrassCommitResult {
    private Boolean isSuccess;
    private String failureReason;

    public static GrassCommitResult success() {
        return GrassCommitResult.builder().isSuccess(true).build();
    }

    public static GrassCommitResult failure(String failureReason) {
        return GrassCommitResult.builder().isSuccess(false).failureReason(failureReason).build();
    }
}

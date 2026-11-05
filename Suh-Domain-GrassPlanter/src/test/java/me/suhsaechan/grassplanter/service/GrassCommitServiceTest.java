package me.suhsaechan.grassplanter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.GrassEncryptionUtil;
import me.suhsaechan.grassplanter.dto.GrassCommitResult;
import me.suhsaechan.grassplanter.dto.GrassRequest;
import me.suhsaechan.grassplanter.entity.GrassCommitLog;
import me.suhsaechan.grassplanter.entity.GrassProfile;
import me.suhsaechan.grassplanter.repository.GrassCommitLogRepository;
import me.suhsaechan.grassplanter.repository.GrassProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// GrassCommitService 단위 테스트: 실패 사유 기록, 자동 커밋 메시지, 프로필별 실패 이력 (#62)
@ExtendWith(MockitoExtension.class)
class GrassCommitServiceTest {

  @Mock
  private GrassProfileRepository grassProfileRepository;
  @Mock
  private GrassCommitLogRepository grassCommitLogRepository;
  @Mock
  private GrassEncryptionUtil encryptionUtil;
  @Mock
  private GrassGithubClient grassGithubClient;

  private GrassCommitService grassCommitService;

  @BeforeEach
  void setUp() {
    grassCommitService = new GrassCommitService(
        grassProfileRepository, grassCommitLogRepository, encryptionUtil, grassGithubClient);
  }

  @Test
  void 수동_커밋_실패_사유가_로그에_기록() {
    GrassProfile profile = buildProfile("alice", "enc-alice", null);
    when(grassProfileRepository.findByIdWithRepository(any())).thenReturn(Optional.of(profile));
    when(encryptionUtil.decrypt("enc-alice")).thenReturn("pat");
    when(grassGithubClient.commitChangelog(profile, "pat", "msg"))
        .thenReturn(GrassCommitResult.failure("GitHub 커밋 실패 (상태 코드 409): conflict"));

    GrassRequest request = GrassRequest.builder()
        .profileId(profile.getGrassProfileId()).commitMessage("msg").build();

    assertThatThrownBy(() -> grassCommitService.executeCommit(request))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.GRASS_COMMIT_FAILED);

    GrassCommitLog savedLog = captureSavedLogs(1).get(0);
    assertThat(savedLog.getIsSuccess()).isFalse();
    assertThat(savedLog.getIsAutoCommit()).isFalse();
    assertThat(savedLog.getErrorMessage()).isEqualTo("GitHub 커밋 실패 (상태 코드 409): conflict");
  }

  @Test
  void 자동_커밋_예외_프로필도_실패_이력_저장_후_다음_프로필_진행() throws Exception {
    GrassProfile brokenProfile = buildProfile("broken", "enc-broken", null);
    GrassProfile normalProfile = buildProfile("normal", "enc-normal", null);
    when(grassProfileRepository.findAutoCommitEnabledWithRepository())
        .thenReturn(List.of(brokenProfile, normalProfile));
    when(encryptionUtil.decrypt("enc-broken")).thenThrow(new IllegalStateException("decrypt failed"));
    when(encryptionUtil.decrypt("enc-normal")).thenReturn("pat");
    when(grassCommitLogRepository.countDailyCommits(eq(normalProfile), any(), any())).thenReturn(0L);
    when(grassGithubClient.getTodayContributionCount(eq("normal"), any(), eq("pat"))).thenReturn(0);
    when(grassGithubClient.commitChangelog(eq(normalProfile), eq("pat"), anyString()))
        .thenReturn(GrassCommitResult.success());

    grassCommitService.executeAutoCommits();

    List<GrassCommitLog> savedLogs = captureSavedLogs(2);
    assertThat(savedLogs.get(0).getGrassProfile()).isSameAs(brokenProfile);
    assertThat(savedLogs.get(0).getIsSuccess()).isFalse();
    assertThat(savedLogs.get(0).getIsAutoCommit()).isTrue();
    assertThat(savedLogs.get(0).getErrorMessage()).contains("decrypt failed");
    assertThat(savedLogs.get(1).getGrassProfile()).isSameAs(normalProfile);
    assertThat(savedLogs.get(1).getIsSuccess()).isTrue();
    assertThat(savedLogs.get(1).getErrorMessage()).isNull();
  }

  @Test
  void 자동_커밋_기여_있으면_로그_없이_생략() throws Exception {
    GrassProfile profile = buildProfile("busy", "enc-busy", null);
    when(grassProfileRepository.findAutoCommitEnabledWithRepository()).thenReturn(List.of(profile));
    when(encryptionUtil.decrypt("enc-busy")).thenReturn("pat");
    when(grassCommitLogRepository.countDailyCommits(eq(profile), any(), any())).thenReturn(0L);
    when(grassGithubClient.getTodayContributionCount(eq("busy"), any(), eq("pat"))).thenReturn(3);

    grassCommitService.executeAutoCommits();

    verify(grassGithubClient, never()).commitChangelog(any(), any(), any());
    verify(grassCommitLogRepository, never()).save(any());
  }

  @Test
  void 자동_커밋_메시지는_템플릿_우선_비면_기본_문구() throws Exception {
    GrassProfile templatedProfile = buildProfile("templated", "enc-templated", "chore: daily grass");
    GrassProfile blankProfile = buildProfile("blank", "enc-blank", "  ");
    when(grassProfileRepository.findAutoCommitEnabledWithRepository())
        .thenReturn(List.of(templatedProfile, blankProfile));
    when(encryptionUtil.decrypt(anyString())).thenReturn("pat");
    when(grassCommitLogRepository.countDailyCommits(any(), any(), any())).thenReturn(0L);
    when(grassGithubClient.getTodayContributionCount(anyString(), any(), eq("pat"))).thenReturn(0);
    when(grassGithubClient.commitChangelog(any(), eq("pat"), anyString()))
        .thenReturn(GrassCommitResult.success());

    grassCommitService.executeAutoCommits();

    verify(grassGithubClient).commitChangelog(templatedProfile, "pat", "chore: daily grass");
    verify(grassGithubClient).commitChangelog(
        blankProfile, "pat", GrassCommitService.DEFAULT_AUTO_COMMIT_MESSAGE);
    List<GrassCommitLog> savedLogs = captureSavedLogs(2);
    assertThat(savedLogs.get(0).getCommitMessage()).isEqualTo("chore: daily grass");
    assertThat(savedLogs.get(1).getCommitMessage()).isEqualTo(GrassCommitService.DEFAULT_AUTO_COMMIT_MESSAGE);
  }

  @Test
  void 긴_실패_사유는_상한_길이로_잘라_저장() {
    GrassProfile profile = buildProfile("alice", "enc-alice", null);
    when(grassProfileRepository.findByIdWithRepository(any())).thenReturn(Optional.of(profile));
    when(encryptionUtil.decrypt("enc-alice")).thenReturn("pat");
    when(grassGithubClient.commitChangelog(any(), any(), any()))
        .thenReturn(GrassCommitResult.failure("x".repeat(GrassCommitService.MAX_ERROR_MESSAGE_LENGTH + 50)));

    assertThatThrownBy(() -> grassCommitService.executeCommit(
        GrassRequest.builder().profileId(profile.getGrassProfileId()).build()))
        .isInstanceOf(CustomException.class);

    assertThat(captureSavedLogs(1).get(0).getErrorMessage())
        .hasSize(GrassCommitService.MAX_ERROR_MESSAGE_LENGTH);
  }

  private GrassProfile buildProfile(String githubUsername, String encryptedPat, String commitMessageTemplate) {
    return GrassProfile.builder()
        .grassProfileId(UUID.randomUUID())
        .githubUsername(githubUsername)
        .encryptedPat(encryptedPat)
        .commitMessageTemplate(commitMessageTemplate)
        .isActive(true)
        .isAutoCommitEnabled(true)
        .build();
  }

  private List<GrassCommitLog> captureSavedLogs(int expectedSaveCount) {
    ArgumentCaptor<GrassCommitLog> captor = ArgumentCaptor.forClass(GrassCommitLog.class);
    verify(grassCommitLogRepository, times(expectedSaveCount)).save(captor.capture());
    return captor.getAllValues();
  }
}

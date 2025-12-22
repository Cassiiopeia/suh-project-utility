package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusUser;
import me.suhsaechan.somansabus.repository.SomansaBusUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusUserService {

  private final SomansaBusUserRepository userRepository;
  private final SomansaBusApiService apiService;

  @Transactional
  public SomansaBusResponse registerUser(SomansaBusRequest request) {
    log.info("버스 예약 사용자 등록 시작: {}", request.getLoginId());

    if (userRepository.existsByLoginId(request.getLoginId())) {
      throw new CustomException(ErrorCode.SOMANSA_BUS_USER_ALREADY_EXISTS);
    }

    int passengerId = apiService.login(request.getLoginId());
    if (passengerId <= 0) {
      log.warn("버스 예약 시스템 로그인 실패: {}", request.getLoginId());
      throw new CustomException(ErrorCode.SOMANSA_BUS_LOGIN_FAILED);
    }

    log.info("버스 예약 시스템 로그인 성공: {}, passengerId: {}", request.getLoginId(), passengerId);

    String displayName = request.getLoginId();
    if (request.getLoginId().contains("@")) {
      displayName = request.getLoginId().substring(0, request.getLoginId().indexOf("@"));
    }

    SomansaBusUser user = SomansaBusUser.builder()
        .loginId(request.getLoginId())
        .displayName(displayName)
        .isActive(true)
        .isVerified(true)
        .build();

    SomansaBusUser savedUser = userRepository.save(user);
    log.info("버스 예약 사용자 등록 완료: {}", savedUser.getLoginId());

    return SomansaBusResponse.builder()
        .user(savedUser)
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getAllUsers() {
    log.info("전체 사용자 목록 조회");
    List<SomansaBusUser> users = userRepository.findAll();
    return SomansaBusResponse.builder()
        .users(users)
        .totalCount((long) users.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getActiveUsers() {
    log.info("활성 사용자 목록 조회");
    List<SomansaBusUser> users = userRepository.findByIsActiveTrueAndIsVerifiedTrue();
    return SomansaBusResponse.builder()
        .users(users)
        .totalCount((long) users.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getUserById(UUID userId) {
    log.info("사용자 상세 조회: {}", userId);
    SomansaBusUser user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_USER_NOT_FOUND));
    return SomansaBusResponse.builder()
        .user(user)
        .build();
  }

  @Transactional
  public SomansaBusResponse toggleUserActive(UUID userId) {
    log.info("사용자 활성화 상태 토글: {}", userId);
    SomansaBusUser user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_USER_NOT_FOUND));

    user.setIsActive(!user.getIsActive());
    SomansaBusUser savedUser = userRepository.save(user);

    log.info("사용자 활성화 상태 변경 완료: {} -> {}", userId, savedUser.getIsActive());
    return SomansaBusResponse.builder()
        .user(savedUser)
        .build();
  }

  @Transactional
  public SomansaBusResponse deleteUser(UUID userId) {
    log.info("사용자 삭제: {}", userId);
    SomansaBusUser user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_USER_NOT_FOUND));

    userRepository.delete(user);
    log.info("사용자 삭제 완료: {}", userId);

    return SomansaBusResponse.builder()
        .user(user)
        .build();
  }
}

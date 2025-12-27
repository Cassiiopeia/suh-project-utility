package me.suhsaechan.somansabus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.dto.SomansaBusRequest;
import me.suhsaechan.somansabus.dto.SomansaBusResponse;
import me.suhsaechan.somansabus.entity.SomansaBusMember;
import me.suhsaechan.somansabus.repository.SomansaBusMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusMemberService {

  private final SomansaBusMemberRepository memberRepository;
  private final SomansaBusApiService apiService;

  @Transactional
  public SomansaBusResponse registerMember(SomansaBusRequest request) {
    log.info("버스 예약 멤버 등록 시작: {}", request.getLoginId());

    if (memberRepository.existsByLoginId(request.getLoginId())) {
      throw new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_ALREADY_EXISTS);
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

    SomansaBusMember member = SomansaBusMember.builder()
        .loginId(request.getLoginId())
        .displayName(displayName)
        .isActive(true)
        .isVerified(true)
        .build();

    SomansaBusMember savedMember = memberRepository.save(member);
    log.info("버스 예약 멤버 등록 완료: {}", savedMember.getLoginId());

    return SomansaBusResponse.builder()
        .member(savedMember)
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getAllMembers() {
    log.info("전체 멤버 목록 조회");
    List<SomansaBusMember> members = memberRepository.findAll();
    return SomansaBusResponse.builder()
        .members(members)
        .totalCount((long) members.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getActiveMembers() {
    log.info("활성 멤버 목록 조회");
    List<SomansaBusMember> members = memberRepository.findByIsActiveTrueAndIsVerifiedTrue();
    return SomansaBusResponse.builder()
        .members(members)
        .totalCount((long) members.size())
        .build();
  }

  @Transactional(readOnly = true)
  public SomansaBusResponse getMemberById(UUID memberId) {
    log.info("멤버 상세 조회: {}", memberId);
    SomansaBusMember member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_NOT_FOUND));
    return SomansaBusResponse.builder()
        .member(member)
        .build();
  }

  @Transactional
  public SomansaBusResponse toggleMemberActive(UUID memberId) {
    log.info("멤버 활성화 상태 토글: {}", memberId);
    SomansaBusMember member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_NOT_FOUND));

    member.setIsActive(!member.getIsActive());
    SomansaBusMember savedMember = memberRepository.save(member);

    log.info("멤버 활성화 상태 변경 완료: {} -> {}", memberId, savedMember.getIsActive());
    return SomansaBusResponse.builder()
        .member(savedMember)
        .build();
  }

  @Transactional
  public SomansaBusResponse deleteMember(UUID memberId) {
    log.info("멤버 삭제: {}", memberId);
    SomansaBusMember member = memberRepository.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.SOMANSA_BUS_MEMBER_NOT_FOUND));

    memberRepository.delete(member);
    log.info("멤버 삭제 완료: {}", memberId);

    return SomansaBusResponse.builder()
        .member(member)
        .build();
  }
}

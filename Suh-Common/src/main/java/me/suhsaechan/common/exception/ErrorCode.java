package me.suhsaechan.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  // COMMON
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
  INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 파라미터입니다."),

  // SCRIPT
  EMPTY_SCRIPT_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 반환값이 없습니다"),
  JSON_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 파싱 중 오류가 발생했습니다"),

  // DOCKER
  DOCKER_INVALID_CONTAINER_NAME(HttpStatus.BAD_REQUEST, "유효하지 않은 컨테이너 이름입니다."),

  // NETWORK
  NETWORK_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "네트워크 요청이 실패했습니다."),

  // NOTICE
  NOT_FOUND_NOTICE(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
  
  // COMMENT
  NOT_FOUND_COMMENT(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  COMMENT_NO_PERMISSION(HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),
  
  // GITHUB
  GITHUB_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "GitHub 레포지토리를 찾을 수 없습니다."),
  GITHUB_REPOSITORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "GitHub 레포지토리 접근이 거부되었습니다."),
  GITHUB_ISSUE_URL_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 GitHub 이슈 URL입니다."),
  GITHUB_ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "GitHub 이슈를 찾을 수 없습니다."),
  GITHUB_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub API 호출 중 오류가 발생했습니다."),

  // GRASS PLANTER
  GRASS_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
  GRASS_PROFILE_DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 GitHub 사용자명입니다."),
  GRASS_PROFILE_DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 프로필입니다."),
  GRASS_PAT_ENCRYPT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Personal Access Token 암호화에 실패했습니다."),
  GRASS_PAT_DECRYPT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Personal Access Token 복호화에 실패했습니다."),
  GRASS_COMMIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 커밋 실행에 실패했습니다."),
  GRASS_CONTRIBUTION_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 기여도 확인에 실패했습니다."),

  // AI SERVER
  AI_SERVER_MODEL_PULL_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "모델이 이미 다운로드 중입니다."),
  AI_SERVER_MODEL_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "모델 삭제에 실패했습니다."),

  // SOMANSA BUS
  SOMANSA_BUS_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "버스 예약 멤버를 찾을 수 없습니다."),
  SOMANSA_BUS_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 멤버입니다."),
  SOMANSA_BUS_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "버스 노선을 찾을 수 없습니다."),
  SOMANSA_BUS_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "예약 스케줄을 찾을 수 없습니다."),
  SOMANSA_BUS_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "버스 예약 시스템 로그인에 실패했습니다."),
  SOMANSA_BUS_SESSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스 예약 세션 생성에 실패했습니다."),
  SOMANSA_BUS_API_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "버스인 노선 조회 API 호출에 실패했습니다."),

  // CHATBOT
  CHATBOT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "챗봇 문서를 찾을 수 없습니다."),
  CHATBOT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "챗봇 세션을 찾을 수 없습니다."),
  CHATBOT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "챗봇 메시지를 찾을 수 없습니다."),
  CHATBOT_VECTOR_STORE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "벡터 저장소 처리 중 오류가 발생했습니다."),
  CHATBOT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지가 너무 깁니다."),

  // TRANSLATE
  TRANSLATE_EMPTY_TEXT(HttpStatus.BAD_REQUEST, "번역할 텍스트가 비어있습니다."),
  TRANSLATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "번역 처리 중 오류가 발생했습니다."),
  TRANSLATE_TEXT_TOO_LONG(HttpStatus.BAD_REQUEST, "번역할 텍스트가 너무 깁니다."),

  // SUH RANDOM
  SUH_RANDOM_ADULT_CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "성인용 콘텐츠 사용을 위해서는 동의가 필요합니다."),

  // TASK TRACKER
  TASK_GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Task 목표를 찾을 수 없습니다."),
  TASK_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Task 진행 기록을 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String message;
}

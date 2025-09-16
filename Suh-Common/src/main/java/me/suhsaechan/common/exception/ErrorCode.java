package me.suhsaechan.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  // COMMON
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근이 거부되었습니다."),
  INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "잘못된 파라미터입니다."),

  // SCRIPT
  EMPTY_SCRIPT_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "스크립트 반환값이 없습니다"),

  // UTILS
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
  FILE_COPY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 복사에 실패했습니다."),
  FILE_PATH_EMPTY(HttpStatus.BAD_REQUEST, "파일 경로가 비어 있거나 null입니다."),
  PERCENTILE_CALCULATION_ERROR(HttpStatus.BAD_REQUEST, "백분위 계산 오류: 전체 기준이 0 일 수 없습니다."),

  // AUTHENTICATION
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ID와 비밀번호를 정확히 입력해 주십시오."),

  // NOTICE
  NOT_FOUND_NOTICE(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
  NOTICE_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 생성에 실패했습니다."),
  NOTICE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 수정에 실패했습니다."),
  
  // COMMENT
  NOT_FOUND_COMMENT(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  COMMENT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 생성에 실패했습니다."),
  COMMENT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "댓글 삭제에 실패했습니다."),
  COMMENT_NO_PERMISSION(HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),
  
  // GITHUB
  GITHUB_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "GitHub 레포지토리를 찾을 수 없습니다."),
  GITHUB_REPOSITORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "GitHub 레포지토리 접근이 거부되었습니다."),
  GITHUB_ISSUE_URL_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 GitHub 이슈 URL입니다."),
  GITHUB_ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, "GitHub 이슈를 찾을 수 없습니다."),
  GITHUB_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub API 호출 중 오류가 발생했습니다."),

  // STUDY
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
  POST_NOT_FOUND(HttpStatus.NOT_FOUND, "포스트를 찾을 수 없습니다."),
  CATEGORIES_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
  PARENT_CATEGORIES_NOT_FOUND(HttpStatus.NOT_FOUND, "상위 카테고리를 찾을 수 없습니다."),
  INVALID_DELETE_CHILD_CATEGORIES_EXISTS(HttpStatus.NOT_FOUND, "하위 카테고리가 있는 카테고리는 삭제할 수 없습니다."),
  INVALID_DELETE_CATEGORIES_POST_EXISTS(HttpStatus.NOT_FOUND, "카테고리에 포스트가 있어 삭제할 수 없습니다."),
  DUPLICATE_CATEGORIES(HttpStatus.NOT_FOUND, "이미 존재하는 카테고리 이름입니다."),
  ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "첨부 파일을 찾을 수 없습니다."),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
  INVALID_OPERATION(HttpStatus.BAD_REQUEST, "유효하지 않은 작업입니다."),
  INVALID_STUDY_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "검색 키워드나 태그를 입력해주세요"),
  FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),

  // GRASS PLANTER
  GRASS_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
  GRASS_PROFILE_DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 GitHub 사용자명입니다."),
  GRASS_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "기본 저장소 정보가 없습니다."),
  GRASS_PAT_DECRYPT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Personal Access Token 복호화에 실패했습니다."),
  GRASS_COMMIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 커밋 실행에 실패했습니다."),
  GRASS_CONTRIBUTION_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub 기여도 확인에 실패했습니다."),
  GRASS_AUTO_COMMIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "자동 커밋 실행에 실패했습니다.");

  private final HttpStatus httpStatus;
  private final String message;
}

package me.suhsaechan.aiserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.EmbeddingsPayload;
import me.suhsaechan.aiserver.dto.GeneratePayload;
import me.suhsaechan.aiserver.dto.TunnelInfoDto;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.NetworkUtil;
import me.suhsaechan.common.util.SshCommandExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServerService {

    private final NetworkUtil networkUtil;
    private final SshCommandExecutor sshCommandExecutor;
    private final ObjectMapper objectMapper;

    @Value("${ai-server.tunnel-info-url}")
    private String tunnelInfoUrl;

    @Value("${ai-server.api-key}")
    private String aiServerApiKey;

    //FIXME: 별도로 ENUM 으로 설치한 모델에 대해서 ENUM 추가하기 or 동적으로 ollama에서 채팅하는것처럼 확인할수있는 방법 찾아보기
    //EMBEDDING만 그냥 별도로 하드코딩해서 관리필요해보임 : 이외는 일반적인 채팅으로 해결, 파라미터에 세션정보도 일벽해야하는디 ollama API g형식 체크 필요
    /**
     * 모델별 지원 기능 확인
     */
    public boolean isModelSupportsGenerate(String modelName) {
        if (modelName == null) return false;
        // embedding 전용 모델들은 generate를 지원하지 않음
        return !modelName.toLowerCase().contains("embedding");
    }

    public boolean isModelSupportsEmbedding(String modelName) {
        if (modelName == null) return false;
        // embedding 모델이거나 일반 모델 중 embedding을 지원하는 모델들
        return modelName.toLowerCase().contains("embedding") ||
               modelName.toLowerCase().contains("all-minilm") ||
               modelName.toLowerCase().contains("sentence");
    }

    /**
     * AI 서버 정보를 조회합니다.
     * tunnel-info는 SSH를 통해서만 접근 가능하므로 SSH 명령어로 호출
     */
    public AiServerResponse getTunnelInfo() {
        log.info("AI 서버 터널 정보 조회 시작: {}", tunnelInfoUrl);

        // tunnel-info는 SSH로만 접근 가능하므로 curl 명령어 사용
        String curlCommand = String.format("curl -sL \"%s\"", tunnelInfoUrl);
        log.debug("실행할 curl 명령어: {}", curlCommand);

        try {
            // SSH를 통해 curl 명령어 실행 (tunnel-info는 SSH 전용)
            String jsonResponse = sshCommandExecutor.executeCommandWithSudoStdin(curlCommand);
            log.debug("API 응답 크기: {} bytes", jsonResponse.length());

            // 빈 응답 체크
            if (jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            // JSON 파싱
            TunnelInfoDto tunnelInfo = objectMapper.readValue(jsonResponse, TunnelInfoDto.class);
            log.info("AI 서버 터널 정보 조회 성공 - 상태: {}, URL: {}",
                    tunnelInfo.getStatus(), tunnelInfo.getUrl());

            // 상태 판단
            Boolean isActive = "active".equalsIgnoreCase(tunnelInfo.getStatus());
            String currentUrl = isActive ? tunnelInfo.getUrl() : null;

            return AiServerResponse.builder()
                    .tunnelInfo(tunnelInfo)
                    .isActive(isActive)
                    .currentUrl(currentUrl)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("AI 서버 응답 JSON 파싱 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 터널 정보 조회 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AI 서버의 모델 목록 조회
     */
    public AiServerResponse getModels() {
        log.info("AI 서버 모델 목록 조회 시작");

        try {
            // 먼저 터널 정보를 가져와서 활성 URL 확인
            AiServerResponse tunnelResponse = getTunnelInfo();
            if (!Boolean.TRUE.equals(tunnelResponse.getIsActive()) || tunnelResponse.getCurrentUrl() == null) {
                log.error("AI 서버가 비활성화 상태이거나 URL을 찾을 수 없습니다");
                throw new CustomException(ErrorCode.AI_SERVER_UNAVAILABLE);
            }

            String modelsUrl = tunnelResponse.getCurrentUrl() + "/api/tags";
            log.debug("모델 목록 조회 URL: {}", modelsUrl);

            // 헤어 API KEY 추가
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", aiServerApiKey);

            // HTTP GET Request
            String jsonResponse = networkUtil.sendGetRequest(modelsUrl, headers);
            log.debug("AI 서버 모델 목록 : {}", jsonResponse);

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 모델 목록 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            return AiServerResponse.builder()
                    .tunnelInfo(tunnelResponse.getTunnelInfo())
                    .isActive(tunnelResponse.getIsActive())
                    .currentUrl(tunnelResponse.getCurrentUrl())
                    .modelsJson(jsonResponse)
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 모델 목록 조회 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * AI 서버 generate API 호출
     */
    public AiServerResponse callGenerate(AiServerRequest request) {
        log.info("AI 서버 generate 호출 시작 - 모델: {}, 프롬프트: {}", request.getModel(), request.getPrompt());

        // 모델이 generate 지원 확인
        if (!isModelSupportsGenerate(request.getModel())) {
            log.error("모델 {}는 generate 기능을 지원하지 않습니다", request.getModel());
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            // 먼저 터널 정보를 가져와서 활성 URL 확인
            AiServerResponse tunnelResponse = getTunnelInfo();
            if (!Boolean.TRUE.equals(tunnelResponse.getIsActive()) || tunnelResponse.getCurrentUrl() == null) {
                log.error("AI 서버가 비활성화 상태이거나 URL을 찾을 수 없습니다");
                throw new CustomException(ErrorCode.AI_SERVER_UNAVAILABLE);
            }

            String generateUrl = tunnelResponse.getCurrentUrl() + "/api/generate";
            log.debug("generate 호출 URL: {}", generateUrl);

            // stream 기본값 설정
            Boolean stream = request.getStream() != null ? request.getStream() : false;

            // JSON 페이로드 객체 생성
            GeneratePayload payload = GeneratePayload.builder()
                    .model(request.getModel())
                    .prompt(request.getPrompt())
                    .stream(stream)
                    .build();

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", aiServerApiKey);
            headers.put("Content-Type", "application/json");

            // HTTP POST 요청
            String jsonResponse = networkUtil.sendPostJsonRequest(generateUrl, headers, payload);
            log.debug("generate 응답 크기: {} bytes", jsonResponse.length());

            if (jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 generate 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            return AiServerResponse.builder()
                    .tunnelInfo(tunnelResponse.getTunnelInfo())
                    .isActive(tunnelResponse.getIsActive())
                    .currentUrl(tunnelResponse.getCurrentUrl())
                    .generatedJson(jsonResponse)
                    .model(request.getModel())
                    .prompt(request.getPrompt())
                    .stream(stream)
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 generate 호출 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * embeddings API 호출
     */
    public AiServerResponse callEmbeddings(AiServerRequest request) {
        log.info("AI 서버 embeddings 호출 시작 - 모델: {}, 입력: {}", request.getModel(), request.getInput());

        // 모델이 embedding을 지원하는지 확인
        if (!isModelSupportsEmbedding(request.getModel())) {
            log.error("모델 {}는 embedding 기능을 지원하지 않습니다", request.getModel());
            throw new CustomException(ErrorCode.INVALID_PARAMETER);
        }

        try {
            // 먼저 터널 정보를 가져와서 활성 URL 확인
            AiServerResponse tunnelResponse = getTunnelInfo();
            if (!Boolean.TRUE.equals(tunnelResponse.getIsActive()) || tunnelResponse.getCurrentUrl() == null) {
                log.error("AI 서버가 비활성화 상태이거나 URL을 찾을 수 없습니다");
                throw new CustomException(ErrorCode.AI_SERVER_UNAVAILABLE);
            }

            String embeddingsUrl = tunnelResponse.getCurrentUrl() + "/api/embeddings";
            log.debug("embeddings 호출 URL: {}", embeddingsUrl);

            // JSON 페이로드 객체 생성
            EmbeddingsPayload payload = EmbeddingsPayload.builder()
                    .model(request.getModel())
                    .input(request.getInput())
                    .build();

            // 헤더 설정
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-Key", aiServerApiKey);
            headers.put("Content-Type", "application/json");

            // NetworkUtil을 통해 HTTP POST 요청 수행 (JSON 객체 직렬화)
            String jsonResponse = networkUtil.sendPostJsonRequest(embeddingsUrl, headers, payload);
            log.debug("embeddings 응답 크기: {} bytes", jsonResponse.length());

            if (jsonResponse.trim().isEmpty()) {
                log.error("AI 서버로부터 빈 embeddings 응답을 받았습니다");
                throw new CustomException(ErrorCode.EMPTY_SCRIPT_RESPONSE);
            }

            // JSON 응답을 그대로 반환 (프론트엔드에서 파싱)
            return AiServerResponse.builder()
                    .tunnelInfo(tunnelResponse.getTunnelInfo())
                    .isActive(tunnelResponse.getIsActive())
                    .currentUrl(tunnelResponse.getCurrentUrl())
                    .embeddingsJson(jsonResponse)
                    .model(request.getModel())
                    .input(request.getInput())
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 embeddings 호출 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

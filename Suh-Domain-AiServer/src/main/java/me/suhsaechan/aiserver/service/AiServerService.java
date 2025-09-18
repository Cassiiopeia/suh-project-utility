package me.suhsaechan.aiserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.aiserver.dto.AiServerRequest;
import me.suhsaechan.aiserver.dto.AiServerResponse;
import me.suhsaechan.aiserver.dto.TunnelInfoDto;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.common.util.SshCommandExecutor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServerService {

    private final SshCommandExecutor sshCommandExecutor;
    private final ObjectMapper objectMapper;
    
    private static final String TUNNEL_INFO_URL = "http://suh-project.synology.me:11435/api/tunnel-info";

    /**
     * AI 서버 정보를 조회합니다.
     */
    public AiServerResponse getTunnelInfo(AiServerRequest request) {
        log.info("AI 서버 터널 정보 조회 시작: {}", TUNNEL_INFO_URL);
        
        // curl 명령어 구성 (-L 플래그로 리다이렉트 따라가기)
        String curlCommand = String.format("curl -sL \"%s\"", TUNNEL_INFO_URL);
        log.debug("실행할 curl 명령어: {}", curlCommand);
        
        try {
            // SSH를 통해 curl 명령어 실행
            String jsonResponse = sshCommandExecutor.executeCommandWithSudoStdin(curlCommand);
            log.debug("API 응답 크기: {} bytes", jsonResponse.length());
            
            // 빈 응답 체크
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
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
            // 이미 적절한 CustomException으로 처리된 경우 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 터널 정보 조회 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

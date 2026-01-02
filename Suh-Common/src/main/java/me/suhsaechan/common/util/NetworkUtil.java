package me.suhsaechan.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

/**
 * 네트워크 HTTP 요청을 위한 유틸리티 클래스
 * OkHttp를 이용한 GET/POST 요청 지원
 * SSH + curl 대신 직접 HTTP 클라이언트로 안전한 요청 처리
 */
@Slf4j
@Component
public class NetworkUtil {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public NetworkUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }

    /**
     * GET 요청 수행
     *
     * @param url 요청할 URL
     * @param headers HTTP 헤더 맵 (null 가능)
     * @return HTTP 응답 문자열
     */
    public String sendGetRequest(String url, Map<String, String> headers) {
        log.debug("GET 요청 시작: {}", url);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        // 헤더 추가
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                log.error("GET 요청 실패: {} - Status: {}, Body: {}", url, response.code(), responseBody);
                throw new CustomException(ErrorCode.NETWORK_REQUEST_FAILED);
            }

            log.debug("GET 요청 성공: {} - Status: {}, Body Size: {} bytes",
                    url, response.code(), responseBody.length());
            return responseBody;
        } catch (IOException e) {
            log.error("GET 요청 중 IOException 발생: {} - {}", url, e.getMessage(), e);
            throw new CustomException(ErrorCode.NETWORK_REQUEST_FAILED);
        }
    }

    /**
     * POST 요청 수행 (JSON 문자열 데이터)
     *
     * @param url 요청할 URL
     * @param headers HTTP 헤더 맵 (null 가능)
     * @param jsonBody JSON 요청 본문 (null 가능)
     * @return HTTP 응답 문자열
     */
    public String sendPostRequest(String url, Map<String, String> headers, String jsonBody) {
        log.debug("POST 요청 시작: {}", url);

        RequestBody body = RequestBody.create(
                jsonBody != null ? jsonBody : "",
                MediaType.get("application/json; charset=utf-8")
        );

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);

        // 헤더 추가
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                log.error("POST 요청 실패: {} - Status: {}, Body: {}", url, response.code(), responseBody);
                throw new CustomException(ErrorCode.NETWORK_REQUEST_FAILED);
            }

            log.debug("POST 요청 성공: {} - Status: {}, Body Size: {} bytes",
                    url, response.code(), responseBody.length());
            return responseBody;
        } catch (IOException e) {
            log.error("POST 요청 중 IOException 발생: {} - {}", url, e.getMessage(), e);
            throw new CustomException(ErrorCode.NETWORK_REQUEST_FAILED);
        }
    }

    /**
     * POST 요청 수행 (JSON 객체 직렬화)
     *
     * @param url 요청할 URL
     * @param headers HTTP 헤더 맵 (null 가능)
     * @param payload JSON으로 직렬화할 객체
     * @return HTTP 응답 문자열
     */
    public String sendPostJsonRequest(String url, Map<String, String> headers, Object payload) {
        log.debug("POST JSON 요청 시작: {}", url);

        try {
            // JSON 직렬화
            String jsonString = objectMapper.writeValueAsString(payload);
            log.debug("JSON 페이로드: {}", jsonString);

            return sendPostRequest(url, headers, jsonString);

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {} - {}", url, e.getMessage(), e);
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
        }
    }

    /**
     * 단순 GET 요청 (헤더 없음)
     *
     * @param url 요청할 URL
     * @return HTTP 응답 문자열
     */
    public String sendGetRequest(String url) {
        return sendGetRequest(url, null);
    }

    /**
     * 단순 POST 요청 (헤더 없음)
     *
     * @param url 요청할 URL
     * @param jsonBody JSON 요청 본문
     * @return HTTP 응답 문자열
     */
    public String sendPostRequest(String url, String jsonBody) {
        return sendPostRequest(url, null, jsonBody);
    }

    /**
     * 단순 POST JSON 요청 (헤더 없음)
     *
     * @param url 요청할 URL
     * @param payload JSON으로 직렬화할 객체
     * @return HTTP 응답 문자열
     */
    public String sendPostJsonRequest(String url, Object payload) {
        return sendPostJsonRequest(url, null, payload);
    }

    /**
     * DELETE 요청 수행 (JSON 문자열 데이터)
     *
     * @param url 요청할 URL
     * @param headers HTTP 헤더 맵 (null 가능)
     * @param jsonBody JSON 요청 본문 (null 가능)
     * @return HTTP 응답 문자열
     */
    public String sendDeleteRequest(String url, Map<String, String> headers, String jsonBody) {
        log.debug("DELETE 요청 시작: {}", url);

        RequestBody body = RequestBody.create(
                jsonBody != null ? jsonBody : "",
                MediaType.get("application/json; charset=utf-8")
        );

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .delete(body);

        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                log.error("DELETE 요청 실패: {} - Status: {}, Body: {}", url, response.code(), responseBody);
                throw new CustomException(ErrorCode.NETWORK_REQUEST_FAILED);
            }

            log.debug("DELETE 요청 성공: {} - Status: {}, Body Size: {} bytes",
                    url, response.code(), responseBody.length());
            return responseBody;
        } catch (IOException e) {
            log.error("DELETE 요청 중 IOException 발생: {} - {}", url, e.getMessage(), e);
            throw new CustomException(ErrorCode.NETWORK_REQUEST_FAILED);
        }
    }
}
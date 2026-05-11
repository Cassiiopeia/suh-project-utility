package me.suhsaechan.somansabus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.common.exception.CustomException;
import me.suhsaechan.common.exception.ErrorCode;
import me.suhsaechan.somansabus.config.SomansaBusHttpClient;
import me.suhsaechan.somansabus.dto.RouteData;
import me.suhsaechan.somansabus.dto.RouteLabelParts;
import me.suhsaechan.somansabus.entity.SomansaBusRoute;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SomansaBusApiService {

  @Value("${somansa.busin.url.login-page:https://csios.busin.co.kr/Login.aspx?device=}")
  private String loginPageUrl;

  @Value("${somansa.busin.url.login-api:https://csios.busin.co.kr/Login.aspx/LoginCheck}")
  private String loginUrl;

  @Value("${somansa.busin.url.create-session:https://csios.busin.co.kr/Default.aspx/CreateSession}")
  private String createSessionUrl;

  @Value("${somansa.busin.url.reservation:https://csios.busin.co.kr/driving/ride_on.aspx/Reserv}")
  private String reservationUrl;

  @Value("${somansa.busin.url.drivelist:https://csios.busin.co.kr/driving/drivelist.aspx}")
  private String drivelistUrl;

  private static final String PUSH_ID = "pc";
  private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final Pattern GO_PAGE_PATTERN = Pattern.compile("goPage\\((\\d+),\\s*'([^']+)'\\s*,\\s*'(True|False)'\\)");
  private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*(\\d{2}:\\d{2})\\s+(.+?)(?:\\s+(\\d+)호)?\\s*-.*$");

  private final OkHttpClient client = SomansaBusHttpClient.newClient();

  public int login(String loginId) {
    log.info("버스 예약 로그인 시작: {}", loginId);

    Request getRequest = new Request.Builder()
        .url(loginPageUrl)
        .get()
        .addHeader("User-Agent", USER_AGENT)
        .build();

    try (Response getResponse = client.newCall(getRequest).execute()) {
      if (!getResponse.isSuccessful()) {
        log.error("로그인 페이지 GET 실패, 코드: {}", getResponse.code());
        return -1;
      }
      log.debug("로그인 페이지 GET 성공, 세션 쿠키 획득");
    } catch (IOException e) {
      log.error("로그인 페이지 GET 중 예외 발생", e);
      return -1;
    }

    String payloadData = String.format("{ \"data\": \"%s,%s\" }", loginId, PUSH_ID);
    log.debug("로그인 페이로드: {}", payloadData);

    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(payloadData, mediaType);

    Request postRequest = new Request.Builder()
        .url(loginUrl)
        .post(body)
        .addHeader("Content-Type", "application/json; charset=UTF-8")
        .addHeader("User-Agent", USER_AGENT)
        .addHeader("Referer", loginPageUrl)
        .build();

    try (Response response = client.newCall(postRequest).execute()) {
      if (!response.isSuccessful()) {
        log.error("로그인 POST 실패, 코드: {}", response.code());
        return -1;
      }

      String responseBody = response.body().string();
      log.debug("로그인 응답: {}", responseBody);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(responseBody);
      int passengerId = rootNode.path("d").asInt();

      if (passengerId > 0) {
        log.info("로그인 성공: {}, 승객ID: {}", loginId, passengerId);
      } else {
        log.warn("로그인 실패: {}, 응답 값: {}", loginId, passengerId);
      }

      return passengerId;
    } catch (IOException e) {
      log.error("로그인 POST 중 예외 발생", e);
      return -1;
    }
  }

  public boolean createSession(String rideId, int passengerId) {
    log.info("세션 생성 시작: rideId={}, passengerId={}", rideId, passengerId);

    String data = String.format("%s,%d,,%s", rideId, passengerId, PUSH_ID);
    String payload = String.format("{ \"data\": \"%s\" }", data);
    log.debug("세션 생성 페이로드: {}", payload);

    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(payload, mediaType);

    Request request = new Request.Builder()
        .url(createSessionUrl)
        .post(body)
        .addHeader("Content-Type", "application/json; charset=UTF-8")
        .addHeader("User-Agent", USER_AGENT)
        .addHeader("Referer", "https://csios.busin.co.kr/default.aspx?device=")
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("세션 생성 실패, 코드: {}", response.code());
        return false;
      }
      log.info("세션 생성 성공: rideId={}, passengerId={}", rideId, passengerId);
      return true;
    } catch (IOException e) {
      log.error("세션 생성 중 예외 발생", e);
      return false;
    }
  }

  public boolean makeReservation(int passengerId, SomansaBusRoute route, LocalDate reservationDate) {
    String formattedDate = reservationDate.format(DATE_FORMATTER);
    log.info("예약 시작 - 승객ID: {}, 버스: {}, 예약일: {}", passengerId, route.getDescription(), formattedDate);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode payload = mapper.createObjectNode();
    payload.put("passengerid", passengerId);
    payload.put("disptid", route.getDisptid());
    payload.put("caralias", route.getCaralias());
    payload.put("clientid", "busman");
    payload.put("createdate", formattedDate);

    String payloadJson = payload.toString();
    log.debug("예약 페이로드: {}", payloadJson);

    String encodedShuttleType = URLEncoder.encode(route.getCaralias(), StandardCharsets.UTF_8);
    String refererUrl = "https://csios.busin.co.kr/driving/ride_on.aspx?rt=r&disptid=" + route.getDisptid() +
        "&shuttletype=" + encodedShuttleType + "&scr=0&isshuttle=0";
    log.debug("Referer URL: {}", refererUrl);

    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(payloadJson, mediaType);

    Request request = new Request.Builder()
        .url(reservationUrl)
        .post(body)
        .addHeader("Content-Type", "application/json; charset=UTF-8")
        .addHeader("User-Agent", USER_AGENT)
        .addHeader("Referer", refererUrl)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        String responseBody = response.body() != null ? response.body().string() : "응답 없음";
        log.error("예약 실패 - HTTP 상태 코드: {}, 응답: {}", response.code(), responseBody);
        return false;
      }

      String responseBody = response.body() != null ? response.body().string() : "";
      log.debug("예약 응답: {}", responseBody);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(responseBody);
      int resultCode = root.path("d").asInt(0);

      if (resultCode == -1) {
        log.warn("예약 정원 초과 - 버스: {}, 예약일: {}", route.getDescription(), formattedDate);
        return false;
      } else if (resultCode == -2) {
        log.warn("예약 등록 오류 - 버스: {}, 예약일: {}", route.getDescription(), formattedDate);
        return false;
      }

      log.info("예약 성공 - 버스: {}, 예약일: {}, 결과코드: {}", route.getDescription(), formattedDate, resultCode);
      return true;
    } catch (IOException e) {
      log.error("예약 중 예외 발생", e);
      return false;
    }
  }

  public List<RouteData> fetchRouteList() {
    log.info("노선 목록 조회 시작");

    Request request = new Request.Builder()
        .url(drivelistUrl)
        .get()
        .addHeader("User-Agent", USER_AGENT)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("노선 목록 조회 실패, 코드: {}", response.code());
        throw new CustomException(ErrorCode.SOMANSA_BUS_API_FETCH_FAILED);
      }
      String html = response.body() != null ? response.body().string() : "";
      List<RouteData> result = parseRouteList(html);
      log.info("노선 목록 조회 완료: {}개", result.size());
      return result;
    } catch (IOException e) {
      log.error("노선 목록 조회 중 예외 발생", e);
      throw new CustomException(ErrorCode.SOMANSA_BUS_API_FETCH_FAILED);
    }
  }

  List<RouteData> parseRouteList(String html) {
    Document doc = Jsoup.parse(html);
    List<RouteData> result = new ArrayList<>();

    for (Element li : doc.select(".line-list li")) {
      Element a = li.selectFirst("a.cont");
      if (a == null) continue;

      String onclick = a.attr("onclick");
      Matcher m = GO_PAGE_PATTERN.matcher(onclick);
      if (!m.find()) continue;

      int disptid = Integer.parseInt(m.group(1));
      String caralias = m.group(2);
      boolean isShuttle = "True".equals(m.group(3));

      Element textSpan = a.selectFirst("span.text");
      String description = textSpan != null ? textSpan.text().trim() : "";

      RouteLabelParts parts = parseLabel(description);

      result.add(RouteData.builder()
          .disptid(disptid)
          .caralias(caralias)
          .isShuttle(isShuttle)
          .description(description)
          .departureTime(parts.getDepartureTime())
          .station(parts.getStation())
          .busNumber(parts.getBusNumber())
          .build());
    }
    return result;
  }

  RouteLabelParts parseLabel(String description) {
    if (description == null) return RouteLabelParts.empty();
    Matcher m = LABEL_PATTERN.matcher(description);
    if (!m.find()) return RouteLabelParts.empty();
    return RouteLabelParts.builder()
        .departureTime(m.group(1))
        .station(m.group(2).trim())
        .busNumber(m.group(3) != null ? Integer.parseInt(m.group(3)) : null)
        .build();
  }

  RouteLabelParts parseLabelForTest(String description) {
    return parseLabel(description);
  }

  List<RouteData> parseRouteListForTest(String html) {
    return parseRouteList(html);
  }
}

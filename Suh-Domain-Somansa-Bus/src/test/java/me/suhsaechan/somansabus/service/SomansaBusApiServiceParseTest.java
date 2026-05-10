package me.suhsaechan.somansabus.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import me.suhsaechan.somansabus.dto.RouteData;
import me.suhsaechan.somansabus.dto.RouteLabelParts;
import org.junit.jupiter.api.Test;

class SomansaBusApiServiceParseTest {

  private final SomansaBusApiService service = new SomansaBusApiService();

  @Test
  void parseLabel_출근_정상() {
    RouteLabelParts parts = service.parseLabelForTest("06:55 당산역 1호 - 출근");
    assertAll(
        () -> assertEquals("06:55", parts.getDepartureTime()),
        () -> assertEquals("당산역", parts.getStation()),
        () -> assertEquals(1, parts.getBusNumber())
    );
  }

  @Test
  void parseLabel_셔틀_busNumber_없음() {
    RouteLabelParts parts = service.parseLabelForTest("07:30 판교역 - 셔틀");
    assertAll(
        () -> assertEquals("07:30", parts.getDepartureTime()),
        () -> assertEquals("판교역", parts.getStation()),
        () -> assertNull(parts.getBusNumber())
    );
  }

  @Test
  void parseLabel_텍스트_앞뒤_공백_제거() {
    RouteLabelParts parts = service.parseLabelForTest("   17:45  당산역  1호 -  퇴근   ");
    assertEquals("17:45", parts.getDepartureTime());
    assertEquals("당산역", parts.getStation());
    assertEquals(1, parts.getBusNumber());
  }

  @Test
  void parseLabel_빈문자열_empty_반환() {
    RouteLabelParts parts = service.parseLabelForTest("");
    assertAll(
        () -> assertNull(parts.getDepartureTime()),
        () -> assertNull(parts.getStation()),
        () -> assertNull(parts.getBusNumber())
    );
  }

  @Test
  void parseRouteList_HTML_파싱() {
    String html = """
        <html><body>
        <div class="line-list">
          <ul>
            <li>
              <a href="#n" onclick="goPage(47040,  '출근' ,  'False')" class="cont">
                <span class="workshift forward">출근</span>
                <span class="text">06:55 당산역 1호 - 출근</span>
              </a>
            </li>
            <li>
              <a href="#n" onclick="goPage(46124,  '출근' ,  'True')" class="cont">
                <span class="workshift forward">셔틀</span>
                <span class="text">07:30 판교역 - 셔틀</span>
              </a>
            </li>
          </ul>
        </div>
        </body></html>
        """;

    List<RouteData> routes = service.parseRouteListForTest(html);

    assertEquals(2, routes.size());

    RouteData first = routes.get(0);
    assertAll(
        () -> assertEquals(47040, first.getDisptid()),
        () -> assertEquals("출근", first.getCaralias()),
        () -> assertFalse(first.getIsShuttle()),
        () -> assertEquals("06:55 당산역 1호 - 출근", first.getDescription()),
        () -> assertEquals("06:55", first.getDepartureTime()),
        () -> assertEquals("당산역", first.getStation()),
        () -> assertEquals(1, first.getBusNumber())
    );

    RouteData shuttle = routes.get(1);
    assertAll(
        () -> assertEquals(46124, shuttle.getDisptid()),
        () -> assertTrue(shuttle.getIsShuttle()),
        () -> assertNull(shuttle.getBusNumber())
    );
  }
}

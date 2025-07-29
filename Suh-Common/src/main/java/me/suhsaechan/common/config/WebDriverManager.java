package me.suhsaechan.common.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebDriverManager {

  private final ChromeOptions options;
  private final String seleniumGridUrl;
  private final boolean isLinuxServer;

  // ThreadLocal로 각 스레드별 WebDriver와 WebDriverWait 관리
  private final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
  private final ThreadLocal<WebDriverWait> waitThreadLocal = new ThreadLocal<>();

  public WebDriverManager(@Value("${selenium.grid-url}") String seleniumGridUrl) {
    this.seleniumGridUrl = seleniumGridUrl;
    this.options = new ChromeOptions();
    this.isLinuxServer = System.getProperty("os.name").toLowerCase().contains("linux"); // 리눅스 서버 감지
    this.options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
    this.options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36");
  }

  /**
   * WebDriver 가져오기 (필요 시 초기화)
   */
  public WebDriver getDriver() {
    WebDriver driver = driverThreadLocal.get();
    if (driver == null || (driver instanceof RemoteWebDriver && ((RemoteWebDriver) driver).getSessionId() == null)) {
      driver = initDriver();
      driverThreadLocal.set(driver);
    }
    return driver;
  }

  /**
   * WebDriver 초기화
   */
  private WebDriver initDriver() {
    try {
      WebDriver driver;
      if (isLinuxServer && !seleniumGridUrl.isEmpty()) {
        driver = new RemoteWebDriver(new URL(seleniumGridUrl), options);
        log.info("Selenium Grid에 연결된 WebDriver 초기화 완료: {}", seleniumGridUrl);
      } else {
        driver = new ChromeDriver(options);
        log.info("로컬 ChromeDriver 초기화 완료");
      }
      WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
      waitThreadLocal.set(wait);
      return driver;
    } catch (MalformedURLException e) {
      throw new RuntimeException("Selenium Grid URL이 잘못되었습니다: " + seleniumGridUrl, e);
    }
  }

  /**
   * WebDriverWait 가져오기
   */
  public WebDriverWait getWait() {
    WebDriverWait wait = waitThreadLocal.get();
    if (wait == null) {
      getDriver(); // driver가 없으면 초기화
      wait = waitThreadLocal.get();
    }
    return wait;
  }

  /**
   * WebDriver 종료
   */
  public void quitDriver() {
    WebDriver driver = driverThreadLocal.get();
    if (driver != null) {
      driver.quit();
      driverThreadLocal.remove();
      waitThreadLocal.remove();
      log.info("WebDriver 세션 종료");
    }
  }
}
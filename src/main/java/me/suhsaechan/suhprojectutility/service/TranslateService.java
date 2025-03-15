package me.suhsaechan.suhprojectutility.service;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import me.suhsaechan.suhprojectutility.config.WebDriverManager;
import me.suhsaechan.suhprojectutility.object.TranslationRequest;
import me.suhsaechan.suhprojectutility.object.TranslationResponse;
import me.suhsaechan.suhprojectutility.object.constant.CommonStatus;
import me.suhsaechan.suhprojectutility.object.constant.TranslatorLanguage;
import me.suhsaechan.suhprojectutility.object.constant.TranslatorType;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TranslateService {

  private final WebDriverManager webDriverManager;

  // 세션 최대 동작 시간 (밀리초)
  private static final long SESSION_TIMEOUT = 60 * 1000; // 60초

  // 세션 잠금을 위한 객체
  private final Object sessionLock = new Object();

  @Autowired
  public TranslateService(WebDriverManager webDriverManager) {
    this.webDriverManager = webDriverManager;
  }

  /**
   * 번역 수행
   */
  public TranslationResponse translate(TranslationRequest request) {
    if (request.getTranslatorType() != TranslatorType.PAPAGO) {
      return TranslationResponse.builder()
          .result(CommonStatus.FAIL)
          .errorMessage("지원하지 않는 번역기 유형입니다.")
          .build();
    }

    // 텍스트 검증
    String text = request.getText();
    if (text == null || text.trim().isEmpty()) {
      return TranslationResponse.builder()
          .result(CommonStatus.FAIL)
          .errorMessage("번역할 텍스트가 비어있습니다.")
          .build();
    }

    // 세션 잠금으로 동시 접근 방지 (Selenium은 한 번에 하나씩만 처리)
    synchronized (sessionLock) {
      WebDriver driver = null;

      try {
        long startTime = System.currentTimeMillis();

        // 웹드라이버 초기화
        driver = webDriverManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // 파파고 페이지 로드
        driver.get("https://papago.naver.com/");

        // 페이지 로드 완료 대기
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
            .executeScript("return document.readyState").equals("complete"));

        // 언어 선택 설정
        setLanguages(driver, wait, request.getSourceLang(), request.getTargetLang());

        // 번역할 텍스트 입력 및 번역 요청
        WebElement sourceTextarea = wait.until(ExpectedConditions.elementToBeClickable(By.id("txtSource")));
        sourceTextarea.clear();
        sourceTextarea.sendKeys(text);

        // 번역 버튼 클릭
        try {
          WebElement translateButton = driver.findElement(By.id("btnTranslate"));
          translateButton.click();
        } catch (NoSuchElementException e) {
          // 자동 번역이 이미 트리거되었을 수 있음
          log.info("번역 버튼을 찾을 수 없습니다. 자동 번역 모드일 수 있습니다.");
        }

        // 번역 결과 초기 대기
        WebElement targetElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtTarget")));

        // 첫 번째 번역 결과가 로드될 때까지 대기
        wait.until(webDriver -> {
          String currentText = targetElement.getText();
          return currentText != null && !currentText.isEmpty() && !currentText.endsWith("...");
        });

        // 첫 번째 번역 결과 저장
        String initialTranslation = targetElement.getText();
        log.debug("초기 번역 결과: {}", initialTranslation);

        // 두 번째 고품질 번역을 위한 추가 대기
        // 텍스트가 변경될 때까지 기다림
        try {
          wait.until(webDriver -> {
            String currentText = targetElement.getText();
            // 초기 번역과 다른 결과가 나타나면 두 번째 번역이 완료된 것
            return !initialTranslation.equals(currentText);
          });
        } catch (Exception e) {
          // 시간 초과 - 두 번째 번역이 없거나 이미 최적의 번역이었을 수 있음
          log.debug("두 번째 번역 결과 대기 중 시간 초과, 초기 번역 결과 사용");
        }

        // 최종 번역 결과 추출 (두 번째 또는 초기 번역)
        String translatedText = targetElement.getText();

        // 감지된 언어 확인 (AUTO인 경우)
        TranslatorLanguage detectedLang = request.getSourceLang();
        if (request.getSourceLang() == TranslatorLanguage.AUTO) {
          try {
            // 파파고 화면에서 감지된 언어 정보 추출 (버튼 텍스트 형식: "English - detected")
            WebElement sourceLanguageButton = driver.findElement(By.id("ddSourceLanguageButton"));
            String buttonText = sourceLanguageButton.getText();

            // 언어 이름 추출 및 매핑 (예: "English - detected" -> "en")
            if (buttonText.contains("-")) {
              String detectedLangName = buttonText.split("-")[0].trim();
              detectedLang = mapLanguageNameToEnum(detectedLangName);
            }
          } catch (Exception e) {
            log.warn("감지된 언어를 확인할 수 없습니다: {}", e.getMessage());
          }
        }

        // 최대 허용 시간 확인
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > SESSION_TIMEOUT) {
          log.warn("번역 처리 시간이 너무 깁니다: {}ms", elapsedTime);
        }

        // 응답 구성
        return TranslationResponse.builder()
            .translatedText(translatedText)
            .detectedLang(detectedLang)
            .targetLang(request.getTargetLang())
            .result(CommonStatus.SUCCESS)
            .build();

      } catch (Exception e) {
        log.error("번역 처리 중 오류 발생: {}", e.getMessage(), e);

        return TranslationResponse.builder()
            .result(CommonStatus.FAIL)
            .errorMessage("번역 처리 중 오류가 발생했습니다: " + e.getMessage())
            .build();
      } finally {
        // 웹드라이버 정리
        if (driver != null) {
          webDriverManager.quitDriver();
        }
      }
    }
  }

  /**
   * 파파고 언어 선택 설정
   */
  private void setLanguages(WebDriver driver, WebDriverWait wait,
      TranslatorLanguage sourceLang, TranslatorLanguage targetLang) {
    try {
      // 소스 언어 설정 (드롭다운)
      if (sourceLang != TranslatorLanguage.AUTO) {
        WebElement sourceDropdown = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("ddSourceLanguageButton")));
        sourceDropdown.click();

        String languageSelector = getLanguageSelector(sourceLang);
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(languageSelector))).click();
      }

      // 타겟 언어 설정 (드롭다운)
      WebElement targetDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("ddTargetLanguageButton")));
      targetDropdown.click();

      String languageSelector = getLanguageSelector(targetLang);
      wait.until(ExpectedConditions.elementToBeClickable(By.xpath(languageSelector))).click();

      // 언어 변경 후 UI가 안정화될 때까지 대기 (sleep 대신 wait 사용)
      wait.until(webDriver -> {
        try {
          // 드롭다운 메뉴가 닫혔는지 확인
          return driver.findElements(By.cssSelector(".dropdown_menu___XsI_h.active___3VPGL")).isEmpty();
        } catch (Exception e) {
          return false;
        }
      });

    } catch (Exception e) {
      log.warn("언어 선택 설정 중 오류 발생: {}", e.getMessage());
    }
  }

  /**
   * 언어 선택자 생성 (XPath)
   */
  private String getLanguageSelector(TranslatorLanguage language) {
    String languageName = getLanguageDisplayName(language);
    return "//li[contains(@class, 'select_item')]//a/span[text()='" + languageName + "']";
  }

  /**
   * 언어 코드를 파파고 표시 이름으로 변환
   */
  private String getLanguageDisplayName(TranslatorLanguage language) {
    switch (language) {
      case AUTO:
        return "Detect language";
      case KO:
        return "Korean";
      case EN:
        return "English";
      case JA:
        return "Japanese";
      case ZH_CN:
        return "Chinese (Simplified)";
      case ZH_TW:
        return "Chinese (Traditional)";
      case ES:
        return "Spanish";
      case FR:
        return "French";
      case DE:
        return "German";
      case RU:
        return "Russian";
      case PT:
        return "Portuguese";
      case IT:
        return "Italian";
      case VI:
        return "Vietnamese";
      case TH:
        return "Thai";
      case ID:
        return "Indonesian";
      default:
        return "English";
    }
  }

  /**
   * 언어 이름을 Enum으로 매핑
   */
  private TranslatorLanguage mapLanguageNameToEnum(String languageName) {
    if (languageName == null) {
      return TranslatorLanguage.EN;
    }

    switch (languageName.toLowerCase()) {
      case "korean":
        return TranslatorLanguage.KO;
      case "english":
        return TranslatorLanguage.EN;
      case "japanese":
        return TranslatorLanguage.JA;
      case "chinese (simplified)":
        return TranslatorLanguage.ZH_CN;
      case "chinese":
        return TranslatorLanguage.ZH_CN;
      case "chinese (traditional)":
        return TranslatorLanguage.ZH_TW;
      case "spanish":
        return TranslatorLanguage.ES;
      case "french":
        return TranslatorLanguage.FR;
      case "german":
        return TranslatorLanguage.DE;
      case "russian":
        return TranslatorLanguage.RU;
      case "portuguese":
        return TranslatorLanguage.PT;
      case "italian":
        return TranslatorLanguage.IT;
      case "vietnamese":
        return TranslatorLanguage.VI;
      case "thai":
        return TranslatorLanguage.TH;
      case "indonesian":
        return TranslatorLanguage.ID;
      default:
        return TranslatorLanguage.EN;
    }
  }
}
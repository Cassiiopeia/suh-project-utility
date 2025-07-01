//package me.suhsaechan.suhprojectutility.util.deprecated;
//
//import lombok.extern.slf4j.Slf4j;
//import me.suhsaechan.suhprojectutility.config.WebDriverManager;
//import me.suhsaechan.suhprojectutility.object.request.TranslationRequest;
//import me.suhsaechan.suhprojectutility.object.response.TranslationResponse;
//import me.suhsaechan.suhprojectutility.object.constant.CommonStatus;
//import me.suhsaechan.suhprojectutility.object.constant.TranslatorLanguage;
//import me.suhsaechan.suhprojectutility.object.constant.TranslatorType;
//import okhttp3.OkHttpClient;
//import org.openqa.selenium.By;
//import org.openqa.selenium.JavascriptExecutor;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//public class OldTranslateService {
//
//  private final OkHttpClient okHttpClient;
//  private final WebDriverManager webDriverManager;
//
//  @Autowired
//  public OldTranslateService(OkHttpClient okHttpClient, WebDriverManager webDriverManager) {
//    this.okHttpClient = okHttpClient;
//    this.webDriverManager = webDriverManager;
//  }
//
//  public TranslationResponse translate(TranslationRequest request) {
//    if (request.getTranslatorType() == TranslatorType.PAPAGO) {
//      return translateWithPapago(request);
//    } else {
//      return TranslationResponse.builder()
//          .result(CommonStatus.FAIL)
//          .errorMessage("지원하지 않는 번역기 유형입니다.")
//          .build();
//    }
//  }
//
//  private TranslationResponse translateWithPapago(TranslationRequest request) {
//    TranslationResponse.TranslationResponseBuilder responseBuilder = TranslationResponse.builder();
//    WebDriver driver = webDriverManager.getDriver();
//    WebDriverWait wait = webDriverManager.getWait();
//
//    try {
//      String sourceCode = request.getSourceLang().getPapagoCode();
//      String targetCode = request.getTargetLang().getPapagoCode();
//      String text = request.getText();
//
//      if (text == null || text.trim().isEmpty()) {
//        return responseBuilder
//            .result(CommonStatus.FAIL)
//            .errorMessage("번역할 텍스트가 비어있습니다.")
//            .build();
//      }
//
//      // 파파고 웹사이트 접속
//      String url = "https://papago.naver.com/?sk=" + sourceCode + "&tk=" + targetCode;
//      driver.get(url);
//
//      // 페이지 로드 완료 대기
//      wait.until(webDriver -> ((JavascriptExecutor) webDriver)
//          .executeScript("return document.readyState").equals("complete"));
//
//      // 텍스트 입력 영역 대기 및 입력
//      WebElement textArea = wait.until(ExpectedConditions.elementToBeClickable(By.id("txtSource")));
//      textArea.clear();
//      textArea.sendKeys(text);
//
//      // 번역 결과 대기
//      WebElement resultElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtTarget")));
//      wait.until(webDriver -> {
//        String currentText = resultElement.getText();
//        return currentText != null && !currentText.isEmpty() && !currentText.endsWith("...");
//      });
//
//      // 번역 결과 추출
//      String translatedText = resultElement.getText();
//      if (translatedText == null || translatedText.isEmpty()) {
//        translatedText = (String) ((JavascriptExecutor) driver).executeScript(
//            "return document.getElementById('txtTarget').innerText");
//      }
//
//      // 감지된 언어 확인 (AUTO일 경우)
//      TranslatorLanguage detectedLang = request.getSourceLang();
//      if (request.getSourceLang() == TranslatorLanguage.AUTO) {
//        try {
//          WebElement detectedLangElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ddSourceLanguageButton")));
//          String detectedLangText = detectedLangElement.getText().toLowerCase();
//          if (detectedLangText.contains("영어")) detectedLang = TranslatorLanguage.EN;
//          else if (detectedLangText.contains("한국어")) detectedLang = TranslatorLanguage.KO;
//          else if (detectedLangText.contains("일본어")) detectedLang = TranslatorLanguage.JA;
//          else if (detectedLangText.contains("중국어 (간체)")) detectedLang = TranslatorLanguage.ZH_CN;
//          else if (detectedLangText.contains("중국어 (번체)")) detectedLang = TranslatorLanguage.ZH_TW;
//          else if (detectedLangText.contains("스페인어")) detectedLang = TranslatorLanguage.ES;
//          else if (detectedLangText.contains("프랑스어")) detectedLang = TranslatorLanguage.FR;
//          else if (detectedLangText.contains("독일어")) detectedLang = TranslatorLanguage.DE;
//          else if (detectedLangText.contains("러시아어")) detectedLang = TranslatorLanguage.RU;
//          else if (detectedLangText.contains("포르투갈어")) detectedLang = TranslatorLanguage.PT;
//          else if (detectedLangText.contains("이탈리아어")) detectedLang = TranslatorLanguage.IT;
//          else if (detectedLangText.contains("베트남어")) detectedLang = TranslatorLanguage.VI;
//          else if (detectedLangText.contains("태국어")) detectedLang = TranslatorLanguage.TH;
//          else if (detectedLangText.contains("인도네시아어")) detectedLang = TranslatorLanguage.ID;
//          else detectedLang = TranslatorLanguage.AUTO;
//        } catch (Exception e) {
//          log.warn("감지된 언어를 확인할 수 없습니다: {}", e.getMessage());
//          detectedLang = TranslatorLanguage.AUTO;
//        }
//      }
//
//      return responseBuilder
//          .translatedText(translatedText)
//          .detectedLang(detectedLang)
//          .targetLang(request.getTargetLang())
//          .result(CommonStatus.SUCCESS)
//          .build();
//
//    } catch (Exception e) {
//      log.error("파파고 번역 중 오류 발생: {}", e.getMessage(), e);
//      return responseBuilder
//          .result(CommonStatus.FAIL)
//          .errorMessage("번역 처리 중 오류가 발생했습니다: " + e.getMessage())
//          .build();
//    } finally {
//      webDriverManager.quitDriver(); // 요청 후 WebDriver 종료
//    }
//  }
//}
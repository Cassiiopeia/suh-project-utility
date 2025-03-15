//package me.suhsaechan.suhprojectutility.config;
//
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class WebDriverConfig {
//
//  @Bean
//  public WebDriver webDriver() {
//    ChromeOptions options = new ChromeOptions();
//    options.addArguments("--headless");
//    options.addArguments("--disable-gpu");
//    options.addArguments("--no-sandbox");
//    options.addArguments("--disable-dev-shm-usage");
//    options.addArguments("--window-size=1920,1080");
//    options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Safari/537.36");
//
//    return new ChromeDriver(options);
//  }
//}
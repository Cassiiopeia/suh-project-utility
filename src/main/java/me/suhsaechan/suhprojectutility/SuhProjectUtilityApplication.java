package me.suhsaechan.suhprojectutility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SuhProjectUtilityApplication {

  public static void main(String[] args) {
    SpringApplication.run(SuhProjectUtilityApplication.class, args);
  }

}

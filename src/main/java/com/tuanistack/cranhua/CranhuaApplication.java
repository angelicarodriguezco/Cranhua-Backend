package com.tuanistack.cranhua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CranhuaApplication {

  public static void main(String[] args) {
    SpringApplication.run(CranhuaApplication.class, args);
  }
}

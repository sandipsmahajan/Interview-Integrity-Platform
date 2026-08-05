package com.integrity.desktopclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the desktop client gateway (websocket session relay). */
@SpringBootApplication
public class DesktopClientServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DesktopClientServiceApplication.class, args);
  }
}

package com.integrity.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the storage service (object metadata, signed URLs). */
@SpringBootApplication
public class StorageServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(StorageServiceApplication.class, args);
  }
}

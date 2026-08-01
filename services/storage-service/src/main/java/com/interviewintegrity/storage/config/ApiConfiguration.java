package com.interviewintegrity.storage.config;

import com.interviewintegrity.storage.service.SignedUrlService;
import com.interviewintegrity.storage.service.StorageBucketService;
import com.interviewintegrity.storage.service.StorageObjectService;
import com.interviewintegrity.storage.web.SignedUrlController;
import com.interviewintegrity.storage.web.StorageBucketController;
import com.interviewintegrity.storage.web.StorageObjectController;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the REST controllers as beans and describes the OpenAPI surface of the service. */
@Configuration
public class ApiConfiguration {

  /** Exposes the storage bucket controller. */
  @Bean
  public StorageBucketController storageBucketController(StorageBucketService bucketService) {
    return new StorageBucketController(bucketService);
  }

  /** Exposes the storage object controller. */
  @Bean
  public StorageObjectController storageObjectController(StorageObjectService objectService) {
    return new StorageObjectController(objectService);
  }

  /** Exposes the signed URL controller. */
  @Bean
  public SignedUrlController signedUrlController(SignedUrlService signedUrlService) {
    return new SignedUrlController(signedUrlService);
  }

  /** Describes the OpenAPI document for the storage service. */
  @Bean
  public OpenAPI platformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Storage Service API")
                .version("v1")
                .description("Object metadata, buckets, versions and pre-signed URL grants"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}

package com.integrity.storage.config;

import com.integrity.storage.repository.ObjectVersionRepository;
import com.integrity.storage.repository.SignedUrlRepository;
import com.integrity.storage.repository.StorageBucketRepository;
import com.integrity.storage.repository.StorageObjectHistoryRepository;
import com.integrity.storage.repository.StorageObjectRepository;
import com.integrity.storage.service.SignedUrlService;
import com.integrity.storage.service.StorageBucketService;
import com.integrity.storage.service.StorageObjectService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit bean wiring for the storage service application services.
 *
 * <p>Services are plain classes instantiated here rather than discovered by component scanning,
 * keeping the dependency graph visible in one place.
 */
@Configuration
public class ApplicationConfiguration {

  /** Provides the storage bucket service. */
  @Bean
  public StorageBucketService storageBucketService(StorageBucketRepository bucketRepository) {
    return new StorageBucketService(bucketRepository);
  }

  /** Provides the storage object service. */
  @Bean
  public StorageObjectService storageObjectService(
      StorageBucketRepository bucketRepository,
      StorageObjectRepository objectRepository,
      ObjectVersionRepository versionRepository,
      StorageObjectHistoryRepository historyRepository) {
    return new StorageObjectService(
        bucketRepository, objectRepository, versionRepository, historyRepository);
  }

  /** Provides the signed URL service. */
  @Bean
  public SignedUrlService signedUrlService(
      StorageObjectRepository objectRepository, SignedUrlRepository signedUrlRepository) {
    return new SignedUrlService(objectRepository, signedUrlRepository);
  }
}

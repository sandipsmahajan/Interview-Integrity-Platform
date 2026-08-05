package com.integrity.storage.service;

import com.integrity.storage.domain.ObjectVersion;
import com.integrity.storage.domain.SignedUrl;
import com.integrity.storage.domain.StorageBucket;
import com.integrity.storage.domain.StorageObject;
import com.integrity.storage.domain.StorageObjectHistory;
import com.integrity.storage.web.dto.BucketResponse;
import com.integrity.storage.web.dto.ObjectResponse;
import com.integrity.storage.web.dto.ObjectVersionResponse;
import com.integrity.storage.web.dto.SignedUrlResponse;
import com.integrity.storage.web.dto.StorageObjectHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps storage-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface StorageMapper {

  /** Maps a storage bucket into its public response. */
  BucketResponse toResponse(StorageBucket bucket);

  /** Maps a signed URL into its public response, without a raw token. */
  @Mapping(target = "token", ignore = true)
  SignedUrlResponse toResponse(SignedUrl signedUrl);

  /** Maps a signed URL into its public response, with the one-time raw token. */
  SignedUrlResponse toResponse(SignedUrl signedUrl, String token);

  /** Maps a storage object into its public response. */
  ObjectResponse toResponse(StorageObject object);

  /** Maps an object version into its public response. */
  ObjectVersionResponse toVersionResponse(ObjectVersion version);

  /** Maps an object history snapshot into its public response. */
  @Mapping(target = "objectId", source = "id")
  StorageObjectHistoryResponse toHistoryResponse(StorageObjectHistory history);
}

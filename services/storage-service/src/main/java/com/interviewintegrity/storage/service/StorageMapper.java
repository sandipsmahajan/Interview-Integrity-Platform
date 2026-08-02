package com.interviewintegrity.storage.service;

import com.interviewintegrity.storage.domain.ObjectVersion;
import com.interviewintegrity.storage.domain.SignedUrl;
import com.interviewintegrity.storage.domain.StorageBucket;
import com.interviewintegrity.storage.domain.StorageObject;
import com.interviewintegrity.storage.domain.StorageObjectHistory;
import com.interviewintegrity.storage.web.dto.BucketResponse;
import com.interviewintegrity.storage.web.dto.ObjectResponse;
import com.interviewintegrity.storage.web.dto.ObjectVersionResponse;
import com.interviewintegrity.storage.web.dto.SignedUrlResponse;
import com.interviewintegrity.storage.web.dto.StorageObjectHistoryResponse;
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

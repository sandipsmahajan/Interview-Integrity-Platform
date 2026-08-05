package com.integrity.storage.web.dto;

import com.integrity.storage.domain.StorageClass;
import java.time.Instant;
import java.util.UUID;

/**
 * Public representation of a storage object history snapshot.
 *
 * @param historyId history identifier
 * @param historyAction database operation that produced the snapshot
 * @param changedBy user that applied the change
 * @param changedAt instant the change was applied
 * @param objectId snapshot object identifier
 * @param organizationId owning tenant
 * @param bucketId owning bucket
 * @param key object key
 * @param sizeBytes payload size
 * @param contentType media type
 * @param storageClass storage tier
 * @param version object version after the change
 */
public record StorageObjectHistoryResponse(
    Long historyId,
    String historyAction,
    UUID changedBy,
    Instant changedAt,
    UUID objectId,
    UUID organizationId,
    UUID bucketId,
    String key,
    Long sizeBytes,
    String contentType,
    StorageClass storageClass,
    Long version) {}

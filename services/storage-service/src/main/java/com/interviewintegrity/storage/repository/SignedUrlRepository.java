package com.interviewintegrity.storage.repository;

import com.interviewintegrity.storage.domain.SignedUrl;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive repository for {@link SignedUrl} entities. */
public interface SignedUrlRepository extends ReactiveCrudRepository<SignedUrl, UUID> {

  /** Finds a signed URL grant by id within an organization. */
  @Query("SELECT * FROM signed_urls WHERE id = :id AND organization_id = :organizationId")
  Mono<SignedUrl> findByIdAndOrganization(UUID id, UUID organizationId);

  /** Lists the signed URL grants of an object, newest first. */
  @Query(
      "SELECT * FROM signed_urls WHERE object_id = :objectId "
          + "AND organization_id = :organizationId ORDER BY created_at DESC")
  Flux<SignedUrl> listByObjectAndOrganization(UUID objectId, UUID organizationId);

  /** Lists the active signed URL grants of an object, newest first. */
  @Query(
      "SELECT * FROM signed_urls WHERE object_id = :objectId "
          + "AND organization_id = :organizationId AND revoked_at IS NULL "
          + "AND expires_at > now() ORDER BY created_at DESC")
  Flux<SignedUrl> listActiveByObject(UUID objectId, UUID organizationId);
}

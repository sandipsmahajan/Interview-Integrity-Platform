package com.interviewintegrity.storage.service;

import com.interviewintegrity.exception.NotFoundException;
import com.interviewintegrity.storage.domain.SignedUrl;
import com.interviewintegrity.storage.domain.StorageObject;
import com.interviewintegrity.storage.domain.UrlPurpose;
import com.interviewintegrity.storage.repository.SignedUrlRepository;
import com.interviewintegrity.storage.repository.StorageObjectRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages pre-signed URL grants; only token hashes are persisted. */
public class SignedUrlService {

  /** A grant together with the raw token issued to the caller. */
  public record SignedUrlGrant(SignedUrl signedUrl, String token) {}

  private final StorageObjectRepository objectRepository;
  private final SignedUrlRepository signedUrlRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  /** Wires the service with its repositories. */
  public SignedUrlService(
      StorageObjectRepository objectRepository, SignedUrlRepository signedUrlRepository) {
    this.objectRepository = objectRepository;
    this.signedUrlRepository = signedUrlRepository;
  }

  /** Issues a signed URL grant for an object, returning the raw token once. */
  @Transactional
  public Mono<SignedUrlGrant> create(
      UUID objectId,
      UUID organizationId,
      UrlPurpose purpose,
      Instant expiresAt,
      Integer maxUses,
      UUID createdBy) {
    return objectRepository
        .findLiveById(objectId)
        .switchIfEmpty(Mono.error(new NotFoundException("Object not found")))
        .flatMap(object -> assertObjectOrganization(object, organizationId))
        .then(
            Mono.defer(
                () -> {
                  String token = generateToken();
                  SignedUrl signedUrl =
                      new SignedUrl(
                          organizationId,
                          objectId,
                          purpose,
                          sha256(token),
                          expiresAt,
                          maxUses,
                          createdBy);
                  return signedUrlRepository
                      .save(signedUrl)
                      .map(saved -> new SignedUrlGrant(saved, token));
                }));
  }

  /** Lists the signed URL grants of an object. */
  @Transactional(readOnly = true)
  public Flux<SignedUrl> list(UUID objectId, UUID organizationId) {
    return objectRepository
        .findLiveById(objectId)
        .switchIfEmpty(Mono.error(new NotFoundException("Object not found")))
        .flatMap(object -> assertObjectOrganization(object, organizationId))
        .thenMany(signedUrlRepository.listByObjectAndOrganization(objectId, organizationId));
  }

  /** Returns a single signed URL grant of an organization. */
  @Transactional(readOnly = true)
  public Mono<SignedUrl> get(UUID urlId, UUID organizationId) {
    return signedUrlRepository
        .findByIdAndOrganization(urlId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Signed URL not found")));
  }

  /** Revokes a signed URL grant. */
  @Transactional
  public Mono<SignedUrl> revoke(UUID urlId, UUID organizationId, UUID byUser) {
    return signedUrlRepository
        .findByIdAndOrganization(urlId, organizationId)
        .switchIfEmpty(Mono.error(new NotFoundException("Signed URL not found")))
        .map(
            signedUrl -> {
              signedUrl.revoke(byUser);
              return signedUrl;
            })
        .flatMap(signedUrlRepository::save);
  }

  private Mono<StorageObject> assertObjectOrganization(StorageObject object, UUID organizationId) {
    if (!organizationId.equals(object.getOrganizationId())) {
      return Mono.error(new NotFoundException("Object not found"));
    }
    return Mono.just(object);
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}

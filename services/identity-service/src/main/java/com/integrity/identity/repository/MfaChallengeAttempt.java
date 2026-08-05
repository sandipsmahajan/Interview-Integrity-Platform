package com.integrity.identity.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Failed MFA verification attempts for a single login challenge. */
@Table("mfa_challenge_attempts")
public class MfaChallengeAttempt implements Persistable<String> {

  @Id
  @Column("challenge_id")
  private String challengeId;

  @Column("user_id")
  private UUID userId;

  private int attempts;

  @Column("last_attempt_at")
  private Instant lastAttemptAt;

  protected MfaChallengeAttempt() {}

  /** Creates a fresh attempt record for the challenge. */
  public MfaChallengeAttempt(String challengeId, UUID userId, Instant lastAttemptAt) {
    this.challengeId = challengeId;
    this.userId = userId;
    this.attempts = 1;
    this.lastAttemptAt = lastAttemptAt;
  }

  public String getChallengeId() {
    return challengeId;
  }

  @Override
  public String getId() {
    return challengeId;
  }

  public UUID getUserId() {
    return userId;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  @Version private long version = 1;

  public long getVersion() {
    return version;
  }

  @Override
  public boolean isNew() {
    return this.challengeId == null;
  }
}

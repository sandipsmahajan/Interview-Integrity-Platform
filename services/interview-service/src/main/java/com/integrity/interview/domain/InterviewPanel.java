package com.integrity.interview.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Row mapped membership of an interviewer on an interview panel.
 *
 * <p>The bridge table uses a composite primary key, so it is read through {@code DatabaseClient}
 * SQL.
 */
@Table("interview_panels")
public class InterviewPanel {

  @Column("interview_id")
  private UUID interviewId;

  @Column("interviewer_id")
  private UUID interviewerId;

  private String role;

  @Column("added_by")
  private UUID addedBy;

  @Column("added_at")
  private Instant addedAt;

  protected InterviewPanel() {}

  /** Returns the id of the interview. */
  public UUID getInterviewId() {
    return interviewId;
  }

  /** Returns the id of the panel interviewer. */
  public UUID getInterviewerId() {
    return interviewerId;
  }

  /** Returns the role of the interviewer on the panel. */
  public String getRole() {
    return role;
  }

  /** Returns the id of the user that added the interviewer. */
  public UUID getAddedBy() {
    return addedBy;
  }

  /** Returns the instant the interviewer joined the panel. */
  public Instant getAddedAt() {
    return addedAt;
  }
}

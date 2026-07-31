package com.interviewintegrity.security;

/** Well-known authorities (roles) used for authorization decisions. */
public final class Authorities {
  private Authorities() {}

  /** Global platform administrator. */
  public static final String PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

  /** Organization administrator (tenant admin). */
  public static final String ORG_ADMIN = "ROLE_ORG_ADMIN";

  /** Recruiter within an organization. */
  public static final String RECRUITER = "ROLE_RECRUITER";

  /** Candidate participating in interviews. */
  public static final String CANDIDATE = "ROLE_CANDIDATE";

  /** Auditor with read access to audit records. */
  public static final String AUDITOR = "ROLE_AUDITOR";
}

package com.interviewintegrity.storage.domain;

/** Operation a pre-signed URL grant authorizes. */
public enum UrlPurpose {
  /** Upload a new object version. */
  UPLOAD,
  /** Download the object payload. */
  DOWNLOAD,
  /** Delete the object. */
  DELETE
}

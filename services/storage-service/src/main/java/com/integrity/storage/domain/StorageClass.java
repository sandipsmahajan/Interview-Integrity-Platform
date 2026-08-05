package com.integrity.storage.domain;

/** Storage tier of an object, mirroring the backend object store. */
public enum StorageClass {
  /** Frequently accessed, replicated storage. */
  STANDARD,
  /** Rarely accessed, lower cost storage. */
  INFREQUENT,
  /** Long term retention storage. */
  ARCHIVE
}

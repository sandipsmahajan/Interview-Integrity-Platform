package com.interviewintegrity.audit.service;

import com.interviewintegrity.audit.domain.ApiAuditLog;
import com.interviewintegrity.audit.domain.AuditEvent;
import com.interviewintegrity.audit.domain.AuditEventChange;
import com.interviewintegrity.audit.web.dto.ApiAuditLogResponse;
import com.interviewintegrity.audit.web.dto.AuditEventChangeResponse;
import com.interviewintegrity.audit.web.dto.AuditEventResponse;
import org.mapstruct.Mapper;

/**
 * Maps audit-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface AuditMapper {

  /** Maps an API audit log entry into its public response. */
  ApiAuditLogResponse toResponse(ApiAuditLog entry);

  /** Maps an audit event into its public response. */
  AuditEventResponse toResponse(AuditEvent event);

  /** Maps an audit event change into its public response. */
  AuditEventChangeResponse toChangeResponse(AuditEventChange change);
}

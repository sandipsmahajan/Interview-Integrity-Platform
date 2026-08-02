package com.interviewintegrity.identity.service;

import com.interviewintegrity.identity.domain.MfaDevice;
import com.interviewintegrity.identity.domain.Permission;
import com.interviewintegrity.identity.domain.TrustedDevice;
import com.interviewintegrity.identity.domain.UserSession;
import com.interviewintegrity.identity.web.dto.MfaDeviceResponse;
import com.interviewintegrity.identity.web.dto.PermissionResponse;
import com.interviewintegrity.identity.web.dto.SessionResponse;
import com.interviewintegrity.identity.web.dto.TrustedDeviceResponse;
import org.mapstruct.Mapper;

/**
 * Maps identity-service domain entities into their wire DTO records.
 *
 * <p>Field names on the records match the entity accessors, so MapStruct generates the
 * implementations at compile time.
 */
@Mapper(componentModel = "spring")
public interface IdentityMapper {

  /** Maps a permission into its public response. */
  PermissionResponse toResponse(Permission permission);

  /** Maps a user session into its public response. */
  SessionResponse toResponse(UserSession session);

  /** Maps an MFA device into its public response. */
  MfaDeviceResponse toResponse(MfaDevice device);

  /** Maps a trusted device into its public response. */
  TrustedDeviceResponse toResponse(TrustedDevice device);
}

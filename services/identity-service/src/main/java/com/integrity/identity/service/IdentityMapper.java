package com.integrity.identity.service;

import com.integrity.identity.domain.MfaDevice;
import com.integrity.identity.domain.Permission;
import com.integrity.identity.domain.TrustedDevice;
import com.integrity.identity.domain.UserSession;
import com.integrity.identity.web.dto.MfaDeviceResponse;
import com.integrity.identity.web.dto.PermissionResponse;
import com.integrity.identity.web.dto.SessionResponse;
import com.integrity.identity.web.dto.TrustedDeviceResponse;
import java.net.InetAddress;
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

  /** Maps a stored {@code inet} address into its textual form for the session response. */
  default String toIpAddress(InetAddress address) {
    return address == null ? null : address.getHostAddress();
  }
}

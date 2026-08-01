package com.interviewintegrity.integration.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request to create a connection to an external account.
 *
 * @param integrationId parent integration
 * @param externalAccountId external account identifier
 * @param scopes granted scopes
 */
public record CreateConnectionRequest(
    @NotNull UUID integrationId,
    @NotBlank @Size(max = 255) String externalAccountId,
    List<@Size(max = 255) String> scopes) {}

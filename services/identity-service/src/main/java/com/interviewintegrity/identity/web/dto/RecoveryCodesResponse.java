package com.interviewintegrity.identity.web.dto;

import java.util.List;

/**
 * A freshly generated set of single-use MFA recovery codes.
 *
 * @param recoveryCodes the codes shown once to the user
 */
public record RecoveryCodesResponse(List<String> recoveryCodes) {}

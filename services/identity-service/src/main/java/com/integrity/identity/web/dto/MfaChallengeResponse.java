package com.integrity.identity.web.dto;

import java.util.List;

/**
 * Returned when a login requires an additional factor.
 *
 * @param mfaRequired always {@code true}, distinguishes the response from a token pair
 * @param challengeId short-lived purpose token authorizing completion of the challenge
 * @param expiresInSeconds challenge lifetime in seconds
 * @param channels supported verification channels, e.g. {@code TOTP}, {@code EMAIL}, {@code
 *     RECOVERY}
 */
public record MfaChallengeResponse(
    boolean mfaRequired, String challengeId, long expiresInSeconds, List<String> channels) {}

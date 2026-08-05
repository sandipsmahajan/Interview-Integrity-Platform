package com.integrity.recruiter.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to exit a candidate from a stage.
 *
 * @param stageId stage to exit
 */
public record ExitStageRequest(@NotNull UUID stageId) {}

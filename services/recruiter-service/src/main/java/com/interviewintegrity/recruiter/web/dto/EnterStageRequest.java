package com.interviewintegrity.recruiter.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to enter a candidate into a pipeline stage.
 *
 * @param stageId target stage
 * @param position position within the stage
 */
public record EnterStageRequest(@NotNull UUID stageId, int position) {}

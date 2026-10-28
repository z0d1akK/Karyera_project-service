package com.zodiakk.projectservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reason why work eligibility check failed")
public enum WorkEligibilityReasonCode {

    PROJECT_NOT_FOUND,
    TASK_NOT_FOUND,
    TASK_PROJECT_MISMATCH,
    NO_ACTIVE_PROJECT_ASSIGNMENT,
    PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING,
    TASK_ARCHIVED
}

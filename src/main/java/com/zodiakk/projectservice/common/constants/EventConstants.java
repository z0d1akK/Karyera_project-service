package com.zodiakk.projectservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventConstants {

    public static final String AGGREGATE_PROJECT = "Project";

    public static final String AGGREGATE_PROJECT_ASSIGNMENT = "ProjectAssignment";

    public static final String AGGREGATE_PROJECT_TASK = "ProjectTask";

    public static final String PROJECT_CREATED = "ProjectCreated";

    public static final String PROJECT_STATUS_CHANGED = "ProjectStatusChanged";

    public static final String PROJECT_ASSIGNMENT_CREATED = "ProjectAssignmentCreated";

    public static final String PROJECT_ASSIGNMENT_ENDED = "ProjectAssignmentEnded";

    public static final String PROJECT_TASK_COMPLETED = "ProjectTaskCompleted";

    public static final String PROJECT_TASK_REOPENED = "ProjectTaskReopened";

    public static final String PROJECT_TASK_ASSIGNEES_CHANGED = "ProjectTaskAssigneesChanged";

    public static final String PROJECT_TASK_COMPLEXITY_CHANGED = "ProjectTaskComplexityChanged";

    public static final int DEFAULT_EVENT_VERSION = 1;
}

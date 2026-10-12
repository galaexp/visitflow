package com.example.data.model

enum class VisitStatus {
    IN_PROGRESS,
    COMPLETED,
    DRAFT,
    CANCELLED
}

enum class ActionStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    UNABLE_TO_COMPLETE,
    DEFERRED,
    CANCELLED
}

enum class PriorityLevel {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

enum class VisitFrequency {
    ONCE,
    DAILY,
    WEEKLY,
    BI_WEEKLY,
    MONTHLY
}

enum class ChecklistStatus {
    NOT_CHECKED,
    PASS,
    FAIL,
    NA,
    NEEDS_FOLLOW_UP
}

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
    SYNCED
}

package com.anderson.contextreminders.model

data class Reminder(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: Category,
    val priority: Priority,
    val status: ReminderStatus,
    val dueDate: String? = null,
    val location: String? = null,
    // "SYNCED", "PENDING_SYNC", "FAILED_SYNC"
    val syncStatus: String = "SYNCED",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isPendingSync: Boolean get() = syncStatus == "PENDING_SYNC"
    val isCompleted: Boolean get() = status is ReminderStatus.Completed

    init {
        require(title.isNotBlank()) {
            "El título del recordatorio no puede estar vacío."
        }
    }
}

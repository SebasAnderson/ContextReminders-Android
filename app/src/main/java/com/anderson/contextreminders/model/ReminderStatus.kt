package com.anderson.contextreminders.model

/**
 * Sealed class para representar los posibles estados de un recordatorio.
 */
sealed class ReminderStatus(val displayName: String) {
    data object Pending : ReminderStatus("Pendiente")
    data object InProgress : ReminderStatus("En Progreso")
    data object Completed : ReminderStatus("Completado")
    data class Archived(val reason: String? = null) : ReminderStatus("Archivado")

    companion object {
        val all: List<ReminderStatus> = listOf(
            Pending,
            InProgress,
            Completed,
            Archived()
        )

        fun fromName(name: String?): ReminderStatus {
            return when (name?.lowercase()?.trim()) {
                "completado", "completed" -> Completed
                "en progreso", "inprogress" -> InProgress
                "archivado", "archived" -> Archived()
                else -> Pending
            }
        }
    }
}

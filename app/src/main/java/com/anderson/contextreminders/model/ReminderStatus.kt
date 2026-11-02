package com.anderson.contextreminders.model

/**
 * Sealed class para representar los posibles estados de un recordatorio.
 * A diferencia de un enum simple, las sealed classes permiten que subtipos
 * específicos almacenen datos adicionales (como la razón de archivo en Archived).
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
    }
}

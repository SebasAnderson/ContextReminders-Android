package com.anderson.contextreminders.model

/**
 * Representa la prioridad de un recordatorio mediante una sealed class.
 */
sealed class Priority(
    val displayName: String,
    val level: Int,
    val colorHex: Long
) {
    data object Low : Priority("Baja", 1, 0xFF4CAF50)      // Verde
    data object Medium : Priority("Media", 2, 0xFFFF9800)   // Naranja
    data object High : Priority("Alta", 3, 0xFFF44336)      // Rojo
    data object Urgent : Priority("Urgente", 4, 0xFF9C27B0) // Púrpura

    companion object {
        val all: List<Priority> = listOf(Low, Medium, High, Urgent)

        fun fromName(name: String?): Priority {
            return when (name?.lowercase()?.trim()) {
                "baja", "low" -> Low
                "alta", "high" -> High
                "urgente", "urgent" -> Urgent
                else -> Medium
            }
        }
    }
}

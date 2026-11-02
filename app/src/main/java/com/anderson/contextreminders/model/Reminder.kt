package com.anderson.contextreminders.model

/**
 * Data class inmutable que modela un recordatorio contextual.
 *
 * Características pedagógicas y técnicas:
 * 1. Inmutabilidad: Todas las propiedades son 'val'.
 * 2. Null Safety: 'description', 'dueDate' y 'location' son de tipo nullable (String?),
 *    garantizando que el compilador de Kotlin evite NullPointerExceptions en tiempo de compilación.
 * 3. Reglas de Validación: El bloque 'init' verifica que los datos cumplan las restricciones
 *    del dominio antes de permitir la instanciación del objeto.
 */
data class Reminder(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: Priority,
    val category: Category,
    val status: ReminderStatus,
    val dueDate: String? = null,
    val location: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        // Regla 1: El título no puede ser vacío ni contener solo espacios en blanco
        require(title.isNotBlank()) {
            "El título del recordatorio no puede estar vacío."
        }
        // Regla 2: El título debe tener al menos 3 caracteres significativos
        require(title.trim().length >= 3) {
            "El título debe contener al menos 3 caracteres (actual: ${title.trim().length})."
        }
        // Regla 3: El título no debe exceder los 80 caracteres
        require(title.trim().length <= 80) {
            "El título no puede exceder los 80 caracteres."
        }
    }
}

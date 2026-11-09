package com.anderson.contextreminders.model

/**
 * Data class inmutable que modela una Categoría para los recordatorios.
 */
data class Category(
    val id: String,
    val name: String,
    val description: String? = null
) {
    val displayName: String get() = name

    companion object {
        val Trabajo = Category(id = "cat_trabajo", name = "Trabajo", description = "Tareas laborales")
        val Estudio = Category(id = "cat_estudio", name = "Estudio", description = "Facultad y entregas")
        val Salud = Category(id = "cat_salud", name = "Salud", description = "Médicos y bienestar")
        val Personal = Category(id = "cat_personal", name = "Personal", description = "Hogar y trámites")
        val Finanzas = Category(id = "cat_finanzas", name = "Finanzas", description = "Pagos y economía")

        val all: List<Category> = listOf(Trabajo, Estudio, Salud, Personal, Finanzas)

        fun fromName(name: String?): Category {
            if (name.isNullOrBlank()) return Personal
            return all.find { it.name.equals(name, ignoreCase = true) || it.id.equals(name, ignoreCase = true) }
                ?: Category(id = name.lowercase().replace(" ", "_"), name = name)
        }
    }
}

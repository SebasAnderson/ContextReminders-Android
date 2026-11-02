package com.anderson.contextreminders.model

/**
 * Data class inmutable que modela una Categoría para los recordatorios.
 * Al ser data class, Kotlin genera automáticamente equals(), hashCode(),
 * toString(), copy() y componentN(), permitiendo su uso eficiente en Sets y Maps.
 */
data class Category(
    val id: String,
    val name: String,
    val description: String? = null
)

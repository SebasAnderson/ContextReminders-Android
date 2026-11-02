package com.anderson.contextreminders.data

import com.anderson.contextreminders.model.Category
import com.anderson.contextreminders.model.Priority
import com.anderson.contextreminders.model.Reminder
import com.anderson.contextreminders.model.ReminderStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ReminderRepository {
    fun getRemindersStream(): Flow<List<Reminder>>
    suspend fun getReminders(): List<Reminder>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun saveReminder(reminder: Reminder): Result<Unit>
    suspend fun deleteReminder(id: String): Result<Unit>
    fun getCategories(): Set<Category>
    fun getColorForCategory(category: Category): Long
    fun getTagForCategory(category: Category): String
}

class InMemoryReminderRepository : ReminderRepository {
    val trabajo = Category(id = "cat_trabajo", name = "Trabajo", description = "Tareas laborales")
    val estudio = Category(id = "cat_estudio", name = "Estudio", description = "Facultad y entregas")
    val salud = Category(id = "cat_salud", name = "Salud", description = "Médicos y bienestar")
    val personal = Category(id = "cat_personal", name = "Personal", description = "Hogar y trámites")
    val finanzas = Category(id = "cat_finanzas", name = "Finanzas", description = "Pagos y economía")

    private val categoriesSet: Set<Category> = setOf(trabajo, estudio, salud, personal, finanzas)

    private val categoryColorsMap: Map<Category, Long> = mapOf(
        trabajo to 0xFF1976D2, estudio to 0xFF7B1FA2, salud to 0xFF388E3C, personal to 0xFFF57C00, finanzas to 0xFF00796B
    )

    private val categoryTagsMap: Map<Category, String> = mapOf(
        trabajo to "💼 #Laboral", estudio to "🎓 #Facultad", salud to "🩺 #Bienestar", personal to "🏠 #Hogar", finanzas to "💳 #Economía"
    )

    private val remindersFlow = MutableStateFlow<List<Reminder>>(
        listOf(
            Reminder(
                id = "rem-1",
                title = "Subir Entrega 2 de Programación Mobile",
                description = "Completar ViewModel, Navigation Compose y PDF con evidencias",
                priority = Priority.Urgent,
                category = estudio,
                status = ReminderStatus.InProgress,
                dueDate = "Hoy, 23:59 hs",
                location = "Campus Virtual"
            ),
            Reminder(
                id = "rem-2",
                title = "Daily de sincronización con el equipo",
                description = "Revisar arquitectura inicial y backlog de tareas",
                priority = Priority.High,
                category = trabajo,
                status = ReminderStatus.Pending,
                dueDate = "Mañana, 09:30 hs",
                location = "Google Meet"
            ),
            Reminder(
                id = "rem-3",
                title = "Turno con el odontólogo",
                description = "Control semestral y limpieza",
                priority = Priority.Medium,
                category = salud,
                status = ReminderStatus.Pending,
                dueDate = "Jueves, 16:00 hs",
                location = "Consultorios Centro"
            ),
            Reminder(
                id = "rem-4",
                title = "Comprar insumos para la semana",
                description = "Frutas, verduras y café en grano",
                priority = Priority.Low,
                category = personal,
                status = ReminderStatus.Completed,
                dueDate = "Ayer",
                location = "Supermercado"
            ),
            Reminder(
                id = "rem-5",
                title = "Pagar resumen de tarjeta de crédito",
                description = "Verificar vencimiento y comprobante de pago",
                priority = Priority.High,
                category = finanzas,
                status = ReminderStatus.Completed,
                dueDate = "10 de Noviembre",
                location = "Home Banking"
            )
        )
    )

    override fun getRemindersStream(): Flow<List<Reminder>> = remindersFlow.asStateFlow()
    override suspend fun getReminders(): List<Reminder> { delay(400); return remindersFlow.value }
    override suspend fun getReminderById(id: String): Reminder? { delay(200); return remindersFlow.value.find { it.id == id } }
    override suspend fun saveReminder(reminder: Reminder): Result<Unit> {
        delay(500)
        remindersFlow.update { listOf(reminder) + it }
        return Result.success(Unit)
    }
    override suspend fun deleteReminder(id: String): Result<Unit> {
        delay(300)
        remindersFlow.update { it.filterNot { item -> item.id == id } }
        return Result.success(Unit)
    }
    override fun getCategories(): Set<Category> = categoriesSet
    override fun getColorForCategory(category: Category): Long = categoryColorsMap[category] ?: 0xFF607D8B
    override fun getTagForCategory(category: Category): String = categoryTagsMap[category] ?: "#General"

    companion object {
        val instance: InMemoryReminderRepository by lazy { InMemoryReminderRepository() }
    }
}
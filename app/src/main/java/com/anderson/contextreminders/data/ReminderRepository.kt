package com.anderson.contextreminders.data

import android.content.Context
import com.anderson.contextreminders.model.Category
import com.anderson.contextreminders.model.Priority
import com.anderson.contextreminders.model.Reminder
import com.anderson.contextreminders.model.ReminderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.TimeUnit

// =====================================================================
// A. DTOs: Contrato del Servicio Remoto (Consigna 3 y 4)
// =====================================================================

data class ReminderDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String,
    val priority: String,
    val status: String,
    val dueDate: String? = null,
    val location: String? = null
)

// =====================================================================
// B. RETROFIT: Interfaz de Servicio Remoto (Consigna 4)
// =====================================================================

interface ReminderApiService {

    @GET("reminders")
    suspend fun getReminders(): Response<List<ReminderDto>>

    @POST("reminders")
    suspend fun createReminder(@Body reminder: ReminderDto): Response<ReminderDto>

    @PUT("reminders/{id}")
    suspend fun updateReminder(
        @Path("id") id: String,
        @Body reminder: ReminderDto
    ): Response<ReminderDto>

    @DELETE("reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>
}

// Cliente OkHttp y Retrofit con Interceptor de Simulación Remota
object RetrofitClient {
    private const val BASE_URL = "https://api.contextreminders.local/v1/"

    private val mockInterceptor = Interceptor { chain ->
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val responseBuilder = okhttp3.Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .addHeader("content-type", "application/json")

        when {
            method == "GET" && path.endsWith("/reminders") -> {
                val mockJson = """
                    [
                        {
                            "id": "rem-remoto-1",
                            "title": "Sincronizar reporte de auditoría",
                            "description": "Tarea descargada del servidor remoto",
                            "category": "Trabajo",
                            "priority": "Alta",
                            "status": "Pendiente",
                            "dueDate": "Hoy, 19:30",
                            "location": "Sede Central"
                        }
                    ]
                """.trimIndent()
                responseBuilder.code(200).message("OK")
                    .body(mockJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            method == "POST" -> {
                responseBuilder.code(201).message("Created")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            method == "PUT" -> {
                responseBuilder.code(200).message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            method == "DELETE" -> {
                responseBuilder.code(204).message("No Content")
                    .body("".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            else -> chain.proceed(request)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(mockInterceptor)
        .build()

    val apiService: ReminderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReminderApiService::class.java)
    }
}

// =====================================================================
// C. MAPPERS EXPLÍCITOS ENTRE CAPAS (Consigna 3)
// =====================================================================

fun ReminderEntity.toDomain(): Reminder {
    return Reminder(
        id = this.id,
        title = this.title,
        description = this.description,
        category = Category.fromName(this.category),
        priority = Priority.fromName(this.priority),
        status = ReminderStatus.fromName(this.status),
        dueDate = this.dueDate,
        location = this.location,
        syncStatus = this.syncStatus,
        createdAt = this.createdAt
    )
}

fun Reminder.toEntity(syncStatus: String = this.syncStatus): ReminderEntity {
    return ReminderEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        category = this.category.name,
        priority = this.priority.displayName,
        status = this.status.displayName,
        dueDate = this.dueDate,
        location = this.location,
        syncStatus = syncStatus,
        createdAt = this.createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

fun ReminderEntity.toDto(): ReminderDto {
    return ReminderDto(
        id = this.id,
        title = this.title,
        description = this.description,
        category = this.category,
        priority = this.priority,
        status = this.status,
        dueDate = this.dueDate,
        location = this.location
    )
}

fun ReminderDto.toEntity(syncStatus: String = "SYNCED"): ReminderEntity {
    return ReminderEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        category = this.category,
        priority = this.priority,
        status = this.status,
        dueDate = this.dueDate,
        location = this.location,
        syncStatus = syncStatus
    )
}

// =====================================================================
// D. RESULTADO Y REPOSITORIO LOCAL-FIRST (Consigna 3)
// =====================================================================

sealed interface SyncResult {
    data class Success(val syncedCount: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}

interface ReminderRepository {
    fun getRemindersFlow(): Flow<List<Reminder>>
    fun getFilteredRemindersFlow(category: String, showCompleted: Boolean): Flow<List<Reminder>>
    fun getReminderByIdFlow(id: String): Flow<Reminder?>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun saveReminder(reminder: Reminder, simulateOffline: Boolean = false): Result<Unit>
    suspend fun updateReminder(reminder: Reminder): Result<Unit>
    suspend fun toggleStatus(id: String): Result<Unit>
    suspend fun deleteReminder(id: String): Result<Unit>
    suspend fun syncPending(): SyncResult
}

class OfflineFirstReminderRepository(
    private val dao: ReminderDao,
    private val api: ReminderApiService = RetrofitClient.apiService
) : ReminderRepository {

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                if (dao.getReminderById("rem-1") == null) {
                    val initialList = listOf(
                        ReminderEntity(
                            id = "rem-1",
                            title = "Completar Entrega 3 de Mobile",
                            description = "Verificar Room, DataStore y sincronización offline",
                            category = "Estudio",
                            priority = "Alta",
                            status = "Pendiente",
                            dueDate = "Hoy, 23:59 hs",
                            location = "Campus Virtual",
                            syncStatus = "SYNCED"
                        ),
                        ReminderEntity(
                            id = "rem-2",
                            title = "Reunión de arquitectura con el equipo",
                            description = "Revisar arquitectura offline-first y Retrofit",
                            category = "Trabajo",
                            priority = "Media",
                            status = "Pendiente",
                            dueDate = "Mañana, 10:00 hs",
                            location = "Google Meet",
                            syncStatus = "SYNCED"
                        ),
                        ReminderEntity(
                            id = "rem-3",
                            title = "Control médico de rutina",
                            description = "Llevar estudios previos de laboratorio",
                            category = "Salud",
                            priority = "Baja",
                            status = "Completado",
                            dueDate = "Jueves pasado",
                            location = "Hospital Italiano",
                            syncStatus = "SYNCED"
                        )
                    )
                    dao.insertReminders(initialList)
                }
            } catch (_: Exception) {}
        }
    }

    override fun getRemindersFlow(): Flow<List<Reminder>> {
        return dao.getAllRemindersFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFilteredRemindersFlow(category: String, showCompleted: Boolean): Flow<List<Reminder>> {
        return dao.getFilteredRemindersFlow(category, showCompleted).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getReminderByIdFlow(id: String): Flow<Reminder?> {
        return dao.getReminderByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getReminderById(id: String): Reminder? = withContext(Dispatchers.IO) {
        dao.getReminderById(id)?.toDomain()
    }

    override suspend fun saveReminder(reminder: Reminder, simulateOffline: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (simulateOffline) {
                val pendingEntity = reminder.toEntity(syncStatus = "PENDING_SYNC")
                dao.insertReminder(pendingEntity)
                Result.success(Unit)
            } else {
                val localEntity = reminder.toEntity(syncStatus = "PENDING_SYNC")
                dao.insertReminder(localEntity)

                try {
                    val response = api.createReminder(localEntity.toDto())
                    if (response.isSuccessful) {
                        dao.insertReminder(localEntity.copy(syncStatus = "SYNCED"))
                    }
                } catch (e: Exception) {
                    // Queda en Room marcado como PENDING_SYNC
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReminder(reminder: Reminder): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateReminder(reminder.toEntity(syncStatus = "SYNCED"))
            try {
                api.updateReminder(reminder.id, reminder.toEntity().toDto())
            } catch (e: Exception) {
                // Modo offline
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleStatus(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getReminderById(id) ?: return@withContext Result.failure(Exception("No encontrado"))
            val newStatus = if (entity.status.equals("Completado", ignoreCase = true)) "Pendiente" else "Completado"
            val updated = entity.copy(status = newStatus, syncStatus = "SYNCED", updatedAt = System.currentTimeMillis())
            dao.updateReminder(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReminder(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteReminderById(id)
            try {
                api.deleteReminder(id)
            } catch (e: Exception) {
                // Modo offline
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPending(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val pendingList = dao.getPendingSyncReminders()
            var syncedCount = 0

            for (item in pendingList) {
                val response = api.createReminder(item.toDto())
                if (response.isSuccessful) {
                    dao.insertReminder(item.copy(syncStatus = "SYNCED", updatedAt = System.currentTimeMillis()))
                    syncedCount++
                }
            }

            val remoteResponse = api.getReminders()
            if (remoteResponse.isSuccessful) {
                remoteResponse.body()?.forEach { dto ->
                    dao.insertReminder(dto.toEntity(syncStatus = "SYNCED"))
                }
            }

            SyncResult.Success(syncedCount)
        } catch (e: IOException) {
            SyncResult.Error("Sin conexión a Internet. Operaciones guardadas localmente.")
        } catch (e: Exception) {
            SyncResult.Error("Error al sincronizar: ${e.localizedMessage}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: OfflineFirstReminderRepository? = null

        fun getInstance(context: Context): OfflineFirstReminderRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val instance = OfflineFirstReminderRepository(db.reminderDao())
                INSTANCE = instance
                instance
            }
        }
    }
}

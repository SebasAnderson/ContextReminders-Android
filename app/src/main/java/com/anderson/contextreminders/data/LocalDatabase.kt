package com.anderson.contextreminders.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// =====================================================================
// 1. DATASTORE: Preferencias de Usuario Persistentes (Consigna 1)
// =====================================================================

private val Context.dataStore by preferencesDataStore(name = "user_settings")

data class AppPreferences(
    val isDarkMode: Boolean = false,
    val showCompleted: Boolean = true,
    val sortOrder: String = "DATE_DESC", // DATE_DESC, PRIORITY_DESC, TITLE_ASC
    val filterCategory: String = "TODAS"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FILTER_CATEGORY = stringPreferencesKey("filter_category")
    }

    // Expone las preferencias reactivas con valores por defecto seguros
    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                isDarkMode = preferences[PreferencesKeys.DARK_MODE] ?: false,
                showCompleted = preferences[PreferencesKeys.SHOW_COMPLETED] ?: true,
                sortOrder = preferences[PreferencesKeys.SORT_ORDER] ?: "DATE_DESC",
                filterCategory = preferences[PreferencesKeys.FILTER_CATEGORY] ?: "TODAS"
            )
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_MODE] = enabled }
    }

    suspend fun setShowCompleted(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_COMPLETED] = show }
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { it[PreferencesKeys.SORT_ORDER] = order }
    }

    suspend fun setFilterCategory(category: String) {
        context.dataStore.edit { it[PreferencesKeys.FILTER_CATEGORY] = category }
    }
}

// =====================================================================
// 2. ROOM: Entidad, DAO y Base de Datos (Consigna 2)
// =====================================================================

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val status: String, // "Pendiente", "Completado", etc.
    val dueDate: String?,
    val location: String?,
    // Campo fundamental para sincronización Offline-First (Consigna 3)
    val syncStatus: String = "SYNCED", // "SYNCED", "PENDING_SYNC", "FAILED_SYNC"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY updatedAt DESC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderByIdFlow(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncReminders(): List<ReminderEntity>

    // Filtros directos en Room
    @Query("SELECT * FROM reminders WHERE (:category = 'TODAS' OR category = :category) AND (:showCompleted = 1 OR status != 'Completado') ORDER BY updatedAt DESC")
    fun getFilteredRemindersFlow(category: String, showCompleted: Boolean): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)
}

@Database(entities = [ReminderEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "context_reminders_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

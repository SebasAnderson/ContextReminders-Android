package com.anderson.contextreminders.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anderson.contextreminders.data.AppPreferences
import com.anderson.contextreminders.data.OfflineFirstReminderRepository
import com.anderson.contextreminders.data.SyncResult
import com.anderson.contextreminders.data.UserPreferencesRepository
import com.anderson.contextreminders.model.Category
import com.anderson.contextreminders.model.Priority
import com.anderson.contextreminders.model.Reminder
import com.anderson.contextreminders.model.ReminderStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// =====================================================================
// ViewModels para la Arquitectura de Presentación (MVVM + StateFlow)
// =====================================================================

sealed interface ReminderListUiState {
    data object Loading : ReminderListUiState
    data class Success(
        val reminders: List<Reminder>,
        val preferences: AppPreferences,
        val totalCount: Int,
        val pendingCount: Int,
        val completedCount: Int,
        val isSyncing: Boolean = false,
        val syncMessage: String? = null
    ) : ReminderListUiState
    data class Error(val message: String) : ReminderListUiState
}

class ReminderListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OfflineFirstReminderRepository.getInstance(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReminderListUiState> = combine(
        repository.getRemindersFlow(),
        preferencesRepository.preferencesFlow,
        _isSyncing,
        _syncMessage
    ) { reminders, prefs, isSyncing, syncMsg ->
        // Aplicar filtros de preferencias
        var filtered = reminders
        if (prefs.filterCategory != "TODAS") {
            filtered = filtered.filter { it.category.name.equals(prefs.filterCategory, ignoreCase = true) }
        }
        if (!prefs.showCompleted) {
            filtered = filtered.filter { !it.isCompleted }
        }

        // Aplicar ordenamiento
        filtered = when (prefs.sortOrder) {
            "PRIORITY_DESC" -> filtered.sortedByDescending { it.priority.level }
            "TITLE_ASC" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.createdAt }
        }

        val total = reminders.size
        val pending = reminders.count { !it.isCompleted }
        val completed = reminders.count { it.isCompleted }

        ReminderListUiState.Success(
            reminders = filtered,
            preferences = prefs,
            totalCount = total,
            pendingCount = pending,
            completedCount = completed,
            isSyncing = isSyncing,
            syncMessage = syncMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderListUiState.Loading
    )

    fun toggleStatus(id: String) {
        viewModelScope.launch {
            repository.toggleStatus(id)
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val res = repository.syncPending()) {
                is SyncResult.Success -> {
                    _syncMessage.value = "Sincronización exitosa (${res.syncedCount} elementos)"
                }
                is SyncResult.Error -> {
                    _syncMessage.value = res.message
                }
            }
            _isSyncing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkMode(enabled) }
    }

    fun setShowCompleted(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowCompleted(show) }
    }

    fun setSortOrder(order: String) {
        viewModelScope.launch { preferencesRepository.setSortOrder(order) }
    }

    fun setFilterCategory(category: String) {
        viewModelScope.launch { preferencesRepository.setFilterCategory(category) }
    }
}

class CreateReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OfflineFirstReminderRepository.getInstance(application)

    sealed interface CreateState {
        data object Idle : CreateState
        data object Saving : CreateState
        data object Success : CreateState
        data class Error(val message: String) : CreateState
    }

    private val _state = MutableStateFlow<CreateState>(CreateState.Idle)
    val state: StateFlow<CreateState> = _state.asStateFlow()

    fun saveReminder(
        title: String,
        description: String?,
        priority: Priority,
        category: Category,
        dueDate: String?,
        location: String?,
        simulateOffline: Boolean
    ) {
        if (title.trim().length < 3) {
            _state.value = CreateState.Error("El título debe tener al menos 3 caracteres.")
            return
        }

        viewModelScope.launch {
            _state.value = CreateState.Saving
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description?.trim(),
                category = category,
                priority = priority,
                status = ReminderStatus.Pending,
                dueDate = dueDate?.trim(),
                location = location?.trim(),
                syncStatus = if (simulateOffline) "PENDING_SYNC" else "SYNCED"
            )
            val result = repository.saveReminder(reminder, simulateOffline)
            if (result.isSuccess) {
                _state.value = CreateState.Success
            } else {
                _state.value = CreateState.Error(result.exceptionOrNull()?.localizedMessage ?: "Error al guardar")
            }
        }
    }

    fun resetState() {
        _state.value = CreateState.Idle
    }
}

class ReminderDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OfflineFirstReminderRepository.getInstance(application)

    private val _reminder = MutableStateFlow<Reminder?>(null)
    val reminder: StateFlow<Reminder?> = _reminder.asStateFlow()

    fun loadReminder(id: String) {
        viewModelScope.launch {
            repository.getReminderByIdFlow(id).collect {
                _reminder.value = it
            }
        }
    }

    fun toggleStatus() {
        val current = _reminder.value ?: return
        viewModelScope.launch {
            repository.toggleStatus(current.id)
        }
    }

    fun deleteReminder(onSuccess: () -> Unit) {
        val current = _reminder.value ?: return
        viewModelScope.launch {
            repository.deleteReminder(current.id)
            onSuccess()
        }
    }
}

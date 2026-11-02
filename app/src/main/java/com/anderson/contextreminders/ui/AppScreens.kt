package com.anderson.contextreminders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anderson.contextreminders.data.InMemoryReminderRepository
import com.anderson.contextreminders.data.ReminderRepository
import com.anderson.contextreminders.model.Category
import com.anderson.contextreminders.model.Priority
import com.anderson.contextreminders.model.Reminder
import com.anderson.contextreminders.model.ReminderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ==========================================
// 1. RUTAS Y GRAFO DE NAVEGACIÓN (Consigna 5)
// ==========================================
sealed class Screen(val route: String) {
    data object List : Screen("reminders_list")
    data object Create : Screen("reminder_create")
    data object Detail : Screen("reminder_detail/{reminderId}") {
        fun createRoute(reminderId: String) = "reminder_detail/$reminderId"
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screen.List.route, modifier = modifier) {
        composable(route = Screen.List.route) {
            ReminderListScreen(
                onCrearRecordatorio = { navController.navigate(Screen.Create.route) },
                onRecordatorioClick = { reminderId -> navController.navigate(Screen.Detail.createRoute(reminderId)) }
            )
        }
        composable(route = Screen.Create.route) {
            CreateReminderScreen(
                onVolver = { navController.popBackStack() },
                onGuardarExito = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("reminderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId") ?: ""
            ReminderDetailScreen(
                reminderId = reminderId,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}

// ==========================================
// 2. PANTALLA DE LISTADO (Consigna 1, 3 y 4)
// ==========================================
sealed interface ReminderListUiState {
    data object Loading : ReminderListUiState
    data object Empty : ReminderListUiState
    data class Success(val reminders: List<Reminder>, val totalCount: Int, val pendingCount: Int, val completedCount: Int) : ReminderListUiState
    data class Error(val message: String) : ReminderListUiState
}

class ReminderListViewModel : ViewModel() {
    private val repository: ReminderRepository = InMemoryReminderRepository.instance
    private val _uiState = MutableStateFlow<ReminderListUiState>(ReminderListUiState.Loading)
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    init { loadReminders() }

    fun loadReminders() {
        viewModelScope.launch {
            _uiState.update { ReminderListUiState.Loading }
            try {
                val list = repository.getReminders()
                if (list.isEmpty()) {
                    _uiState.update { ReminderListUiState.Empty }
                } else {
                    _uiState.update {
                        ReminderListUiState.Success(
                            reminders = list,
                            totalCount = list.size,
                            pendingCount = list.count { it.status is ReminderStatus.Pending },
                            completedCount = list.count { it.status is ReminderStatus.Completed }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { ReminderListUiState.Error("Error al cargar datos: ${e.message}") }
            }
        }
    }

    fun simulateEmptyState() { _uiState.update { ReminderListUiState.Empty } }
    fun simulateErrorState() { _uiState.update { ReminderListUiState.Error("Fallo de conexión simulado con el repositorio de datos.") } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReminderListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "ContextReminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(text = "Sebastian Anderson - Programación Mobile", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Restablecer Listado Normal") }, onClick = { viewModel.loadReminders(); showMenu = false })
                        DropdownMenuItem(text = { Text("Simular Estado Vacío") }, onClick = { viewModel.simulateEmptyState(); showMenu = false })
                        DropdownMenuItem(text = { Text("Simular Estado de Error") }, onClick = { viewModel.simulateErrorState(); showMenu = false })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCrearRecordatorio,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Crear nuevo")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ReminderListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReminderListUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "No hay recordatorios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Todavía no registraste ninguna tarea. ¡Creá la primera ahora!", textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onCrearRecordatorio) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear Recordatorio")
                            }
                        }
                    }
                }
                is ReminderListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Ocurrió un error", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.loadReminders() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reintentar")
                            }
                        }
                    }
                }
                is ReminderListUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${state.totalCount}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "Total", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${state.pendingCount}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "Pendientes", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${state.completedCount}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "Completas", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(state.reminders, key = { it.id }) { reminder ->
                                ReminderItemCard(reminder = reminder, onClick = { onRecordatorioClick(reminder.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItemCard(reminder: Reminder, onClick: () -> Unit) {
    val repo = InMemoryReminderRepository.instance
    val categoryColor = Color(repo.getColorForCategory(reminder.category))
    val categoryTag = repo.getTagForCategory(reminder.category)
    val isCompleted = reminder.status is ReminderStatus.Completed

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = categoryColor.copy(alpha = 0.15f), contentColor = categoryColor) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(categoryColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${reminder.category.name} ($categoryTag)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val priorityColor = Color(reminder.priority.colorHex)
                    Surface(shape = RoundedCornerShape(8.dp), color = priorityColor.copy(alpha = 0.15f), contentColor = priorityColor) {
                        Text(text = "Prioridad: ${reminder.priority.displayName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
            reminder.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statusColor = when (reminder.status) {
                    is ReminderStatus.Completed -> Color(0xFF2E7D32)
                    is ReminderStatus.InProgress -> Color(0xFF1976D2)
                    is ReminderStatus.Pending -> Color(0xFFE65100)
                    is ReminderStatus.Archived -> Color(0xFF616161)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.12f), contentColor = statusColor) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = reminder.status.displayName, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                reminder.dueDate?.let { due ->
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = due, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. PANTALLA DE CREACIÓN (Consigna 1 y 2)
// ==========================================
data class CreateReminderUiState(
    val title: String = "",
    val titleError: String? = null,
    val description: String = "",
    val descriptionError: String? = null,
    val selectedCategory: Category? = null,
    val selectedPriority: Priority? = null,
    val dueDate: String = "",
    val location: String = "",
    val isSubmitting: Boolean = false,
    val saveSuccess: Boolean = false,
    val generalError: String? = null
) {
    val isFormValid: Boolean
        get() = title.trim().length >= 4 && description.length <= 200 && selectedCategory != null && selectedPriority != null && titleError == null && descriptionError == null && !isSubmitting
}

class CreateReminderViewModel : ViewModel() {
    private val repository: ReminderRepository = InMemoryReminderRepository.instance
    private val _uiState = MutableStateFlow(CreateReminderUiState())
    val uiState: StateFlow<CreateReminderUiState> = _uiState.asStateFlow()

    val availableCategories = repository.getCategories()
    val availablePriorities = listOf(Priority.Low, Priority.Medium, Priority.High, Priority.Urgent)

    fun onTitleChanged(newTitle: String) {
        val trimmed = newTitle.trim()
        val error = when {
            newTitle.isEmpty() -> "El título no puede estar vacío."
            trimmed.length < 4 -> "El título debe contener al menos 4 caracteres (actual: ${trimmed.length})."
            else -> null
        }
        _uiState.update { it.copy(title = newTitle, titleError = error) }
    }

    fun onDescriptionChanged(newDesc: String) {
        val error = if (newDesc.length > 200) "La descripción no puede superar los 200 caracteres (actual: ${newDesc.length})." else null
        _uiState.update { it.copy(description = newDesc, descriptionError = error) }
    }

    fun onCategorySelected(cat: Category) { _uiState.update { it.copy(selectedCategory = cat) } }
    fun onPrioritySelected(prio: Priority) { _uiState.update { it.copy(selectedPriority = prio) } }
    fun onDueDateChanged(d: String) { _uiState.update { it.copy(dueDate = d) } }
    fun onLocationChanged(l: String) { _uiState.update { it.copy(location = l) } }

    fun saveReminder() {
        val current = _uiState.value
        if (!current.isFormValid) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val newReminder = Reminder(
                    id = "rem-${UUID.randomUUID().toString().take(6)}",
                    title = current.title.trim(),
                    description = current.description.ifBlank { null },
                    priority = current.selectedPriority!!,
                    category = current.selectedCategory!!,
                    status = ReminderStatus.Pending,
                    dueDate = current.dueDate.ifBlank { null },
                    location = current.location.ifBlank { null }
                )
                repository.saveReminder(newReminder)
                _uiState.update { it.copy(isSubmitting = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, generalError = e.message) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    onVolver: () -> Unit,
    onGuardarExito: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateReminderViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onGuardarExito()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recordatorio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Título de la tarea *") },
                placeholder = { Text("Mínimo 4 caracteres") },
                isError = state.titleError != null,
                supportingText = { state.titleError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Descripción (opcional)") },
                isError = state.descriptionError != null,
                supportingText = {
                    Text(text = "${state.description.length}/200 caracteres")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("Categoría *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.availableCategories.forEach { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Prioridad *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.availablePriorities.forEach { priority ->
                    FilterChip(
                        selected = state.selectedPriority == priority,
                        onClick = { viewModel.onPrioritySelected(priority) },
                        label = { Text(priority.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = state.dueDate,
                onValueChange = viewModel::onDueDateChanged,
                label = { Text("Fecha / Plazo (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChanged,
                label = { Text("Ubicación contextual (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = viewModel::saveReminder,
                    modifier = Modifier.weight(1f),
                    enabled = state.isFormValid
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ==========================================
// 4. PANTALLA DE DETALLE (Consigna 1 y 5)
// ==========================================
sealed interface ReminderDetailUiState {
    data object Loading : ReminderDetailUiState
    data class Success(val reminder: Reminder) : ReminderDetailUiState
    data class Error(val message: String) : ReminderDetailUiState
}

class ReminderDetailViewModel : ViewModel() {
    private val repository: ReminderRepository = InMemoryReminderRepository.instance
    private val _uiState = MutableStateFlow<ReminderDetailUiState>(ReminderDetailUiState.Loading)
    val uiState: StateFlow<ReminderDetailUiState> = _uiState.asStateFlow()

    fun loadReminder(id: String) {
        viewModelScope.launch {
            _uiState.update { ReminderDetailUiState.Loading }
            val reminder = repository.getReminderById(id)
            if (reminder != null) {
                _uiState.update { ReminderDetailUiState.Success(reminder) }
            } else {
                _uiState.update { ReminderDetailUiState.Error("No se encontró el recordatorio") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminderId: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReminderDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(reminderId) {
        viewModel.loadReminder(reminderId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Recordatorio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is ReminderDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                is ReminderDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ReminderDetailUiState.Success -> {
                    val reminder = state.reminder
                    val repo = InMemoryReminderRepository.instance
                    val categoryColor = Color(repo.getColorForCategory(reminder.category))
                    val categoryTag = repo.getTagForCategory(reminder.category)

                    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(text = reminder.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = reminder.description ?: "Sin descripción adicional.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Metadatos y Contexto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailItemRow(icon = Icons.Default.Info, label = "Categoría", value = "${reminder.category.name} ($categoryTag)", tint = categoryColor)
                        DetailItemRow(icon = Icons.Default.Star, label = "Prioridad", value = "${reminder.priority.displayName} (Nivel ${reminder.priority.level})", tint = Color(reminder.priority.colorHex))
                        DetailItemRow(icon = Icons.Default.Check, label = "Estado actual", value = reminder.status.displayName, tint = MaterialTheme.colorScheme.primary)
                        reminder.dueDate?.let { DetailItemRow(icon = Icons.Default.DateRange, label = "Fecha / Plazo", value = it, tint = MaterialTheme.colorScheme.tertiary) }
                        reminder.location?.let { DetailItemRow(icon = Icons.Default.Place, label = "Ubicación", value = it, tint = MaterialTheme.colorScheme.secondary) }
                        Spacer(modifier = Modifier.height(30.dp))
                        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al Listado") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
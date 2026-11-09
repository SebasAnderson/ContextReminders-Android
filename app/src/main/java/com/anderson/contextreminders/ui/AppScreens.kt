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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.anderson.contextreminders.model.Category
import com.anderson.contextreminders.model.Priority
import com.anderson.contextreminders.model.Reminder
import com.anderson.contextreminders.model.ReminderStatus

// =====================================================================
// 1. PANTALLA DE LISTADO (Consigna 1, 2 y 3)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    navController: NavHostController,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit,
    viewModel: ReminderListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPreferencesDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is ReminderListUiState.Success) {
            val msg = (uiState as ReminderListUiState.Success).syncMessage
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearSyncMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ContextReminders", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Offline-First • Room • DataStore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showPreferencesDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Preferencias"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCrearRecordatorio,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Recordatorio")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ReminderListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReminderListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ReminderListUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Banner de Sincronización en proceso
                        if (state.isSyncing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Resumen de estado (Métricas)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryStatCard("Total", state.totalCount.toString(), Icons.AutoMirrored.Filled.FormatListBulleted, Modifier.weight(1f))
                            SummaryStatCard("Pendientes", state.pendingCount.toString(), Icons.Default.PendingActions, Modifier.weight(1f))
                            SummaryStatCard("Completados", state.completedCount.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                        }

                        // Chips de filtrado por categoría (Persistido con DataStore)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.preferences.filterCategory == "TODAS",
                                onClick = { viewModel.setFilterCategory("TODAS") },
                                label = { Text("Todas") }
                            )
                            Category.all.forEach { cat ->
                                FilterChip(
                                    selected = state.preferences.filterCategory == cat.name,
                                    onClick = { viewModel.setFilterCategory(cat.name) },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        if (state.reminders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No hay recordatorios que coincidan con los filtros.",
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.reminders, key = { it.id }) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        onToggleStatus = { viewModel.toggleStatus(reminder.id) },
                                        onClick = { onRecordatorioClick(reminder.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Diálogo de Preferencias (DataStore)
                    if (showPreferencesDialog) {
                        AlertDialog(
                            onDismissRequest = { showPreferencesDialog = false },
                            title = { Text("Preferencias (DataStore)") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tema Oscuro")
                                        Switch(
                                            checked = state.preferences.isDarkMode,
                                            onCheckedChange = { viewModel.setDarkMode(it) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Mostrar Completados")
                                        Switch(
                                            checked = state.preferences.showCompleted,
                                            onCheckedChange = { viewModel.setShowCompleted(it) }
                                        )
                                    }
                                    Text("Criterio de Ordenamiento", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = state.preferences.sortOrder == "DATE_DESC",
                                            onClick = { viewModel.setSortOrder("DATE_DESC") },
                                            label = { Text("Fecha") }
                                        )
                                        FilterChip(
                                            selected = state.preferences.sortOrder == "PRIORITY_DESC",
                                            onClick = { viewModel.setSortOrder("PRIORITY_DESC") },
                                            label = { Text("Prioridad") }
                                        )
                                        FilterChip(
                                            selected = state.preferences.sortOrder == "TITLE_ASC",
                                            onClick = { viewModel.setSortOrder("TITLE_ASC") },
                                            label = { Text("Título") }
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showPreferencesDialog = false }) {
                                    Text("Cerrar")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox para alternar estado
            IconButton(onClick = onToggleStatus) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Cambiar estado",
                    tint = if (reminder.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (reminder.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Indicador Offline-First (Consigna 3)
                    if (reminder.isPendingSync) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Pendiente de sincronizar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                if (!reminder.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge de Prioridad
                    Surface(
                        color = Color(reminder.priority.colorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = reminder.priority.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(reminder.priority.colorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Badge de Categoría
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = reminder.category.name,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    reminder.dueDate?.let {
                        Text(
                            text = "📅 $it",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 2. PANTALLA DE CREACIÓN (Consigna 2 y 3)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(
    navController: NavHostController,
    onVolver: () -> Unit,
    onGuardarExito: () -> Unit,
    viewModel: CreateReminderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf<Priority>(Priority.Medium) }
    var selectedCategory by remember { mutableStateOf<Category>(Category.Trabajo) }
    var dueDate by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var simulateOffline by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is CreateReminderViewModel.CreateState.Success) {
            viewModel.resetState()
            onGuardarExito()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recordatorio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state is CreateReminderViewModel.CreateState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (state as CreateReminderViewModel.CreateState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título *") },
                placeholder = { Text("Ej: Entregar Actividad 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Text("Categoría", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.all.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name) }
                    )
                }
            }

            Text("Prioridad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.all.forEach { prio ->
                    FilterChip(
                        selected = selectedPriority == prio,
                        onClick = { selectedPriority = prio },
                        label = { Text(prio.displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Fecha / Plazo (Opcional)") },
                placeholder = { Text("Ej: Mañana 18:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Ubicación contextual (Opcional)") },
                placeholder = { Text("Ej: Campus Virtual / Oficina") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Switch pedagógico para verificar Consigna 3 (Offline-First)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simular Guardado Offline", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Guarda en Room con estado PENDING_SYNC para probar sincronización posterior.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = simulateOffline,
                        onCheckedChange = { simulateOffline = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveReminder(
                        title = title,
                        description = description,
                        priority = selectedPriority,
                        category = selectedCategory,
                        dueDate = dueDate,
                        location = location,
                        simulateOffline = simulateOffline
                    )
                },
                enabled = title.isNotBlank() && state !is CreateReminderViewModel.CreateState.Saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state is CreateReminderViewModel.CreateState.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Text("Guardar Recordatorio")
                }
            }
        }
    }
}

// =====================================================================
// 3. PANTALLA DE DETALLE (Consigna 2 y 3)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    navController: NavHostController,
    reminderId: String,
    onVolver: () -> Unit,
    viewModel: ReminderDetailViewModel = viewModel()
) {
    val reminder by viewModel.reminder.collectAsStateWithLifecycle()

    LaunchedEffect(reminderId) {
        viewModel.loadReminder(reminderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteReminder(onSuccess = onVolver) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            reminder?.let { r ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(text = r.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = r.description ?: "Sin descripción adicional.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Estado de Sincronización
                    Surface(
                        color = if (r.isPendingSync) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (r.isPendingSync) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (r.isPendingSync) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (r.isPendingSync) "Pendiente de Sincronización" else "Sincronizado con el Servidor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (r.isPendingSync) "Los datos residen en Room y se enviarán al reconectar." else "Confirmado por el servicio remoto.",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text("Metadatos y Contexto", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    DetailRowItem(Icons.Default.Folder, "Categoría", r.category.name)
                    DetailRowItem(Icons.Default.Flag, "Prioridad", r.priority.displayName)
                    DetailRowItem(Icons.Default.Info, "Estado", r.status.displayName)
                    r.dueDate?.let { DetailRowItem(Icons.Default.DateRange, "Fecha límite", it) }
                    r.location?.let { DetailRowItem(Icons.Default.Place, "Ubicación", it) }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.toggleStatus() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (r.isCompleted) Icons.Default.Replay else Icons.Default.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (r.isCompleted) "Reabrir Recordatorio" else "Marcar como Completado")
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

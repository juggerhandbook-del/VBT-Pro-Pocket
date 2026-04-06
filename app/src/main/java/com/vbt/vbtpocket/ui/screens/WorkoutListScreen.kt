package com.vbt.vbtpocket.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vbt.vbtpocket.data.ExerciseLibraryEntity
import com.vbt.vbtpocket.ui.components.*
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel

@Composable
fun WorkoutListScreen(viewModel: MainViewModel) {
    val workouts by viewModel.workouts.observeAsState(emptyList())
    val library by viewModel.library.observeAsState(emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Rutinas, 1 = Ejercicios
    var searchQuery by remember { mutableStateOf("") }

    // ESTADOS DE NAVEGACIÓN
    var editWorkoutId by remember { mutableStateOf<Int?>(null) }
    var isCreatingWorkout by remember { mutableStateOf(false) }
    var isCreatingExercise by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    when {
        isCreatingExercise -> {
            NewExerciseScreen(viewModel) { isCreatingExercise = false }
        }
        isCreatingWorkout || editWorkoutId != null -> {
            WorkoutScreen(viewModel, editWorkoutId) {
                isCreatingWorkout = false
                editWorkoutId = null
            }
        }
        else -> {
            Scaffold(
                floatingActionButton = {
                    Box {
                        FloatingActionButton(onClick = { showMenu = true }, containerColor = HarbizBlue) {
                            Icon(Icons.Default.Add, "Añadir", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Nueva Rutina") },
                                leadingIcon = { Icon(Icons.Default.Assignment, null) },
                                onClick = { showMenu = false; isCreatingWorkout = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Nuevo Ejercicio") },
                                leadingIcon = { Icon(Icons.Default.FitnessCenter, null) },
                                onClick = { showMenu = false; isCreatingExercise = true }
                            )
                        }
                    }
                }
            ) { padding ->
                Column(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundGray)) {

                    // --- CABECERA ---
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Biblioteca", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // --- SELECTOR DE PESTAÑAS (TABS) ---
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = HarbizBlue,
                            indicator = { tabPositions ->
                                if (selectedTab < tabPositions.size) {
                                    // CORRECCIÓN: Usamos Indicator en lugar de SecondaryIndicator
                                    TabRowDefaults.Indicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = HarbizBlue
                                    )
                                }
                            },
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("MIS RUTINAS", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("EJERCICIOS", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // --- CONTENIDO DINÁMICO ---
                    Crossfade(targetState = selectedTab, label = "") { tab ->
                        when (tab) {
                            0 -> RoutinesTab(workouts, viewModel) { editWorkoutId = it }
                            1 -> ExercisesTab(library, searchQuery) { searchQuery = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoutinesTab(workouts: List<com.vbt.vbtpocket.data.WorkoutPlanEntity>, viewModel: MainViewModel, onEdit: (Int) -> Unit) {
    if (workouts.isEmpty()) {
        EmptyStateView("No tienes rutinas.", "Crea una plantilla para tus entrenamientos.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workouts) { workout ->
                HarbizCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(workout.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Creada el ${java.text.SimpleDateFormat("dd/MM/yy").format(java.util.Date(workout.dateCreated))}", color = Color.Gray, fontSize = 12.sp)
                        }
                        IconButton(onClick = { onEdit(workout.id) }) {
                            Icon(Icons.Default.Edit, null, tint = HarbizBlue)
                        }
                        IconButton(onClick = { viewModel.deleteWorkout(workout) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ExercisesTab(library: List<ExerciseLibraryEntity>, query: String, onQueryChange: (String) -> Unit) {
    val filteredList = library.filter { it.name.contains(query, ignoreCase = true) || it.muscleGroup.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        // Buscador
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Buscar ejercicio o músculo...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HarbizBlue)
        )

        if (filteredList.isEmpty()) {
            EmptyStateView("No se encontraron ejercicios.", "Prueba con otro nombre o crea uno nuevo.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredList) { exercise ->
                    HarbizCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = HarbizBlue.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    null,
                                    modifier = Modifier.padding(12.dp),
                                    tint = HarbizBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(exercise.muscleGroup.uppercase(), color = HarbizBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun EmptyStateView(title: String, sub: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Text(sub, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
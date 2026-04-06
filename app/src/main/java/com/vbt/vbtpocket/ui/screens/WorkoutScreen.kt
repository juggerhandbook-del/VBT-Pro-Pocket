package com.vbt.vbtpocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vbt.vbtpocket.ui.components.*
import com.vbt.vbtpocket.ui.viewmodel.ExerciseDraft
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel

@Composable
fun WorkoutScreen(viewModel: MainViewModel, workoutIdToEdit: Int? = null, onFinished: () -> Unit) {
    var routineName by remember { mutableStateOf("") }
    val exercises = remember { mutableStateListOf<ExerciseDraft>() }
    var isLoading by remember { mutableStateOf(workoutIdToEdit != null) }

    var showExerciseSelector by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val library by viewModel.library.observeAsState(emptyList())

    LaunchedEffect(workoutIdToEdit) {
        if (workoutIdToEdit != null) {
            val (title, loadedExercises) = viewModel.loadWorkoutForEditing(workoutIdToEdit)
            routineName = title
            exercises.clear()
            exercises.addAll(loadedExercises)
            isLoading = false
        } else {
            routineName = ""
            exercises.clear()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray).padding(20.dp)) {
        Text(if (workoutIdToEdit != null) "Editar Plantilla" else "Nueva Plantilla", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        HarbizCard { HarbizTextField("Nombre de la rutina", routineName) { routineName = it } }

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(exercises) { index, exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onUpdate = { updated -> exercises[index] = updated },
                    onDelete = { exercises.removeAt(index) }
                )
            }
            item {
                HarbizButton("+ Añadir Ejercicio") { showExerciseSelector = true }
            }
        }

        HarbizButton(if (workoutIdToEdit != null) "Guardar Cambios" else "Guardar Plantilla") {
            if (routineName.isNotEmpty() && exercises.isNotEmpty()) {
                if (workoutIdToEdit != null) viewModel.updateWorkout(workoutIdToEdit, routineName, exercises.toList())
                else viewModel.saveWorkout(routineName, exercises.toList())
                onFinished()
            }
        }
    }

    if (showExerciseSelector) {
        AlertDialog(
            onDismissRequest = { showExerciseSelector = false },
            title = { Text("Seleccionar Ejercicio") },
            text = {
                Column(modifier = Modifier.height(350.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        val filtered = library.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        items(filtered) { lib ->
                            TextButton(
                                onClick = {
                                    exercises.add(ExerciseDraft(id = exercises.size, name = lib.name))
                                    showExerciseSelector = false
                                    searchQuery = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(lib.name, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExerciseSelector = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
fun ExerciseCard(exercise: ExerciseDraft, onUpdate: (ExerciseDraft) -> Unit, onDelete: () -> Unit) {
    HarbizCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text("Modo VBT", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = Color.Gray)
                Switch(checked = exercise.isVBT, onCheckedChange = { onUpdate(exercise.copy(isVBT = it)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = exercise.sets, onValueChange = { onUpdate(exercise.copy(sets = it)) }, label = { Text("Series") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = exercise.restSeconds, onValueChange = { onUpdate(exercise.copy(restSeconds = it)) }, label = { Text("Desc. Ser(s)") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = exercise.restNextExerciseSeconds, onValueChange = { onUpdate(exercise.copy(restNextExerciseSeconds = it)) }, label = { Text("Desc. cambio ejercicio (s)") }, modifier = Modifier.fillMaxWidth())

            if (exercise.isVBT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = exercise.targetVelocity, onValueChange = { onUpdate(exercise.copy(targetVelocity = it)) }, label = { Text("Vel (m/s)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = exercise.velocityLoss, onValueChange = { onUpdate(exercise.copy(velocityLoss = it)) }, label = { Text("Pérdida (%)") }, modifier = Modifier.weight(1f))
                }
            } else {
                OutlinedTextField(value = exercise.reps, onValueChange = { onUpdate(exercise.copy(reps = it)) }, label = { Text("Reps Objetivo") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
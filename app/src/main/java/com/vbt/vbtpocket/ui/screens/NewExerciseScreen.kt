package com.vbt.vbtpocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vbt.vbtpocket.data.ExerciseLibraryEntity
import com.vbt.vbtpocket.ui.components.*
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel

@Composable
fun NewExerciseScreen(viewModel: MainViewModel, onFinished: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Crear Ejercicio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Añádelo a la base de datos para usarlo en tus rutinas.", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        HarbizCard {
            HarbizTextField("Nombre del Ejercicio", name) { name = it }
            HarbizTextField("Grupo Muscular (ej: Pierna)", muscle) { muscle = it }
            HarbizTextField("Descripción / Técnica", desc) { desc = it }
            HarbizTextField("URL de Imagen o Video", imageUrl) { imageUrl = it }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HarbizButton("Guardar en Biblioteca") {
            if (name.isNotEmpty()) {
                viewModel.insertExerciseToLibrary(
                    ExerciseLibraryEntity(
                        name = name,
                        muscleGroup = muscle,
                        description = desc,
                        imageUrl = imageUrl
                    )
                )
                onFinished()
            }
        }

        TextButton(
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Cancelar", color = Color.Gray)
        }
    }
}
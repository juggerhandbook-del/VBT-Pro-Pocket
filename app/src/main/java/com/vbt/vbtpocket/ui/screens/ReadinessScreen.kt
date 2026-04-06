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
import com.vbt.vbtpocket.ui.components.*
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel

@Composable
fun ReadinessScreen(viewModel: MainViewModel, onFinished: () -> Unit) {
    var sleep by remember { mutableFloatStateOf(3f) }
    var stress by remember { mutableFloatStateOf(3f) }
    var fatigue by remember { mutableFloatStateOf(3f) }
    var soreness by remember { mutableFloatStateOf(3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Bienestar Diario", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tu estado influye en tu velocidad.", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        HarbizCard {
            ReadinessSlider("Calidad de Sueño", sleep) { sleep = it }
            ReadinessSlider("Nivel de Estrés", stress) { stress = it }
            ReadinessSlider("Nivel de Fatiga", fatigue) { fatigue = it }
            ReadinessSlider("Dolor Muscular", soreness) { soreness = it }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HarbizButton("Confirmar y Empezar") {
            viewModel.saveReadiness(sleep.toInt(), stress.toInt(), fatigue.toInt(), soreness.toInt())
            onFinished()
        }
    }
}

@Composable
fun ReadinessSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(thumbColor = HarbizBlue, activeTrackColor = HarbizBlue)
        )
    }
}
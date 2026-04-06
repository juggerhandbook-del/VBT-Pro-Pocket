package com.vbt.vbtpocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
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
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(sessionId: Int, workoutTitle: String, viewModel: MainViewModel, onFinish: () -> Unit) {
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var weight by remember { mutableStateOf("") }
    var isSetRunning by remember { mutableStateOf(false) }

    var exercises by remember { mutableStateOf(listOf<com.vbt.vbtpocket.data.ExerciseEntity>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isResting by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(0) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf("") }

    // --- ESTADOS DEL ENCODER ---
    val isEncoderConnected by viewModel.isEncoderConnected.collectAsState()
    val encoderStatusMsg by viewModel.encoderStatusMsg.collectAsState()
    val repsInSet by viewModel.currentSetReps.collectAsState()

    LaunchedEffect(sessionId) {
        val loaded = viewModel.getExercisesForWorkoutSync(sessionId)
        if (loaded.isNotEmpty()) { exercises = loaded; isLoading = false } else { onFinish() }
    }

    LaunchedEffect(isResting) {
        while (isResting && timeLeft > 0) { delay(1000L); timeLeft-- }
        if (timeLeft <= 0) isResting = false
    }

    val currentExercise = if (exercises.isNotEmpty()) exercises[currentExerciseIndex] else null

    // =========================================================================
    // LÓGICA CENTRALIZADA DE TRANSICIÓN (CORRECCIÓN AQUÍ)
    // =========================================================================
    fun advanceWorkout() {
        isSetRunning = false
        viewModel.stopEncoder()

        val w = weight.toFloatOrNull() ?: 0f
        val avgVel = if (repsInSet.isNotEmpty()) repsInSet.average().toFloat() else 0f
        viewModel.logSet(sessionId, currentExercise?.name ?: "", currentSet, w, repsInSet.size, 7, avgVel)
        viewModel.clearCurrentSetReps()

        if (currentExercise != null) {
            if (currentSet < currentExercise.targetSets) {
                // Caso A: Quedan series en este ejercicio
                currentSet++
                timeLeft = currentExercise.restSeconds
                isResting = true
            } else {
                // Caso B: Última serie terminada. ¿Hay más ejercicios?
                if (currentExerciseIndex < exercises.size - 1) {
                    currentExerciseIndex++
                    currentSet = 1
                    timeLeft = currentExercise.restNextExerciseSeconds
                    isResting = true
                } else {
                    // Caso C: No hay más ejercicios. Fin del entreno.
                    showNotesDialog = true
                }
            }
        }
    }

    // --- AUTO-STOP POR FATIGA ---
    LaunchedEffect(repsInSet) {
        if (repsInSet.size >= 2 && currentExercise?.isVBT == true && isSetRunning) {
            val firstVel = repsInSet.first()
            val lastVel = repsInSet.last()
            val lossLimit = currentExercise.velocityLoss / 100f

            if (lastVel < (firstVel * (1 - lossLimit))) {
                delay(1000) // Pequeña pausa para que el usuario vea la última barra
                advanceWorkout()
            }
        }
    }

    if (isLoading || currentExercise == null) return Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray).padding(20.dp)) {

        // BARRA DE CONEXIÓN
        HarbizCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEncoderConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    tint = if (isEncoderConnected) Color(0xFF00C853) else Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("VBT POCKET", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(encoderStatusMsg, fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { if (!isEncoderConnected) viewModel.autoConnectEncoder() else viewModel.disconnectEncoder() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isEncoderConnected) Color.LightGray else HarbizBlue),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (isEncoderConnected) "Desconectar" else "Conectar", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = workoutTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = currentExercise.name, color = HarbizBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Spacer(modifier = Modifier.height(16.dp))

        if (isResting) {
            HarbizCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("RECUPERACIÓN", color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = HarbizBlue)
                    Button(onClick = { isResting = false }) { Text("Omitir") }
                }
            }
        } else {
            HarbizCard {
                Text("Serie $currentSet de ${currentExercise.targetSets}", fontWeight = FontWeight.Bold)

                // GRÁFICA DE REPETICIONES
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp)) {
                    if (repsInSet.isEmpty()) {
                        Text(text = if (isSetRunning) "Mueve la carga..." else "Pulsa Empezar Serie", modifier = Modifier.align(Alignment.Center), color = Color.LightGray)
                    } else {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                            repsInSet.forEachIndexed { index, vel ->
                                val barHeight = (vel * 60).dp
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(String.format("%.2f", vel), fontSize = 10.sp, color = HarbizBlue)
                                    Box(modifier = Modifier.fillMaxWidth().height(barHeight).background(HarbizBlue, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                    Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                HarbizTextField("Peso (kg)", weight) { weight = it }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isSetRunning) {
                    Button(
                        onClick = {
                            if (weight.isNotEmpty() && isEncoderConnected) {
                                viewModel.clearCurrentSetReps()
                                viewModel.startEncoder()
                                isSetRunning = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = isEncoderConnected,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(isEncoderConnected) Color(0xFF00C853) else Color.Gray)
                    ) {
                        Text(if(isEncoderConnected) "EMPEZAR SERIE" else "CONECTA EL ENCODER", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { advanceWorkout() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("FINALIZAR SERIE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = { showNotesDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Finalizar Entrenamiento", color = Color.Gray)
        }
    }

    // DIÁLOGO DE NOTAS FINALES
    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Resumen de la Sesión") },
            text = {
                OutlinedTextField(
                    value = sessionNotes,
                    onValueChange = { sessionNotes = it },
                    placeholder = { Text("¿Cómo te has sentido?") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            },
            confirmButton = {
                HarbizButton("Guardar y Salir") {
                    viewModel.finishWorkoutWithNotes(sessionId, sessionNotes)
                    viewModel.disconnectEncoder()
                    onFinish()
                }
            }
        )
    }
}
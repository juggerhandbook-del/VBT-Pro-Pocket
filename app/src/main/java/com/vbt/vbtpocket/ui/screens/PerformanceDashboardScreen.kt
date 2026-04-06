package com.vbt.vbtpocket.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.vbt.vbtpocket.data.AthleteEntity
import com.vbt.vbtpocket.ui.components.*
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDashboardScreen(viewModel: MainViewModel) {
    val athlete by viewModel.athlete.observeAsState()
    val loggedExercises by viewModel.loggedExercises.observeAsState(emptyList())
    var selectedExercise by remember { mutableStateOf("") }
    val logs by viewModel.getLogsForExercise(selectedExercise).observeAsState(emptyList())
    var isEditingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(loggedExercises) {
        if (selectedExercise.isEmpty() && loggedExercises.isNotEmpty()) {
            selectedExercise = loggedExercises.first()
        }
    }

    if (athlete == null || isEditingProfile) {
        ProfileEditForm(
            currentAthlete = athlete,
            onSave = { viewModel.saveAthlete(it); isEditingProfile = false },
            onCancel = { isEditingProfile = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // --- CABECERA PERFIL ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(HarbizBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        athlete?.name?.take(1)?.uppercase() ?: "?",
                        color = HarbizBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hola, ${athlete?.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Nivel: ${athlete?.level}", color = Color.Gray, fontSize = 14.sp)
                }
                IconButton(onClick = { isEditingProfile = true }) {
                    Icon(Icons.Default.Settings, null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- MÉTRICAS BIOMÉTRICAS ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBox(Modifier.weight(1f), "Peso", "${athlete?.weight} kg", Icons.Default.MonitorWeight)
                MetricBox(Modifier.weight(1f), "Altura", "${athlete?.height} cm", Icons.Default.Height)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN DE RENDIMIENTO VBT ---
            Text("Análisis de Fuerza", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            if (loggedExercises.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HarbizCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Timeline, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sin datos de entrenamiento", fontWeight = FontWeight.Bold)
                        Text("Tus gráficas aparecerán tras tu primera serie.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                    loggedExercises.forEach { name ->
                        FilterChip(
                            selected = selectedExercise == name,
                            onClick = { selectedExercise = name },
                            label = { Text(name) },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                HarbizCard {
                    val profilePoints = viewModel.calculateProfile(logs)
                    Text("Perfil Carga-Velocidad", fontWeight = FontWeight.Bold)
                    Text("Relación entre carga (kg) y velocidad (m/s)", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    if (profilePoints.size >= 2) {
                        val chartColor = HarbizBlue
                        val model = entryModelOf(*profilePoints.map { it.velocity }.toTypedArray())

                        // CONFIGURACIÓN DE LÍNEA PARA VICO 1.13.1
                        Chart(
                            chart = lineChart(
                                lines = listOf(
                                    LineChart.LineSpec(
                                        lineColor = chartColor.toArgb(),
                                        lineThicknessDp = 3f,
                                        lineBackgroundShader = verticalGradient(
                                            colors = arrayOf(chartColor.copy(alpha = 0.4f), Color.Transparent)
                                        ),
                                        point = shapeComponent(shape = Shapes.pillShape, color = chartColor),
                                        pointSizeDp = 8f
                                    )
                                )
                            ),
                            model = model,
                            startAxis = rememberStartAxis(
                                valueFormatter = { value, _ -> String.format("%.1f", value) }
                            ),
                            bottomAxis = rememberBottomAxis(
                                valueFormatter = { value, _ ->
                                    val index = value.toInt()
                                    if (index in profilePoints.indices) "${profilePoints[index].weight.toInt()}" else ""
                                }
                            ),
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )

                        val isSquat = selectedExercise.contains("Sentadilla", ignoreCase = true)
                        val est1RM = viewModel.calculate1RMByLinearRegression(profilePoints, isSquat)

                        Divider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("1RM ESTIMADO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${String.format("%.1f", est1RM)} kg", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = HarbizBlue)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("MVT (Límite)", fontSize = 10.sp, color = Color.Gray)
                                Text("${if(isSquat) 0.3 else 0.15} m/s", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("Registra al menos 2 pesos diferentes para generar la curva.", color = HarbizBlue, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MetricBox(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = HarbizBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ProfileEditForm(currentAthlete: AthleteEntity?, onSave: (AthleteEntity) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(currentAthlete?.name ?: "") }
    var weight by remember { mutableStateOf(currentAthlete?.weight?.toString() ?: "") }
    var height by remember { mutableStateOf(currentAthlete?.height?.toString() ?: "") }
    var squatRM by remember { mutableStateOf(currentAthlete?.squatRM?.toString() ?: "") }
    var benchRM by remember { mutableStateOf(currentAthlete?.benchRM?.toString() ?: "") }
    var deadliftRM by remember { mutableStateOf(currentAthlete?.deadliftRM?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Configurar Perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Mantén tus datos actualizados para cálculos precisos.", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        HarbizTextField("Nombre Completo", name) { name = it }
        HarbizTextField("Peso Corporal (kg)", weight) { weight = it }
        HarbizTextField("Altura (cm)", height) { height = it }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Records Personales (Manual)", fontWeight = FontWeight.Bold, color = HarbizBlue)
        Text("Tus mejores marcas históricas.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        HarbizTextField("RM Sentadilla", squatRM) { squatRM = it }
        HarbizTextField("RM Press Banca", benchRM) { benchRM = it }
        HarbizTextField("RM Peso Muerto", deadliftRM) { deadliftRM = it }

        Spacer(modifier = Modifier.height(32.dp))

        HarbizButton("Guardar Cambios") {
            onSave(AthleteEntity(
                id = currentAthlete?.id ?: 0,
                name = name,
                weight = weight.toFloatOrNull() ?: 0f,
                height = height.toIntOrNull() ?: 0,
                squatRM = squatRM.toFloatOrNull() ?: 0f,
                benchRM = benchRM.toFloatOrNull() ?: 0f,
                deadliftRM = deadliftRM.toFloatOrNull() ?: 0f
            ))
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Cancelar", color = Color.Gray)
        }
    }
}
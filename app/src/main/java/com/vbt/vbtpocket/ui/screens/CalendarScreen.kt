package com.vbt.vbtpocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.vbt.vbtpocket.data.ExerciseEntity
import com.vbt.vbtpocket.data.ScheduledSessionWithTitle
import com.vbt.vbtpocket.ui.components.*
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@Composable
fun CalendarScreen(viewModel: MainViewModel, onStartWorkout: (Int, String) -> Unit) {
    val sessions by viewModel.calendarSessions.observeAsState(emptyList())
    val templates by viewModel.workouts.observeAsState(emptyList())
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showScheduleList by remember { mutableStateOf(false) }

    // Estado para la vista previa de ejercicios
    var previewWorkoutId by remember { mutableStateOf<Int?>(null) }
    var previewExercises by remember { mutableStateOf<List<ExerciseEntity>>(emptyList()) }

    val currentMonth = remember { YearMonth.now() }
    val state = rememberCalendarState(
        startMonth = currentMonth.minusMonths(3),
        endMonth = currentMonth.plusMonths(6),
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray).padding(16.dp)) {
        val visibleMonth = state.firstVisibleMonth.yearMonth
        Text(
            text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${visibleMonth.year}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        DaysOfWeekHeader()

        VerticalCalendar(
            state = state,
            contentPadding = PaddingValues(bottom = 100.dp),
            dayContent = { day ->
                val daySessions = sessions.filter {
                    Instant.ofEpochMilli(it.session.date).atZone(ZoneId.systemDefault()).toLocalDate() == day.date
                }
                DayCell(day = day, sessions = daySessions) {
                    selectedDate = day.date
                    showDetailsDialog = true
                    showScheduleList = false
                }
            }
        )
    }

    // --- DIÁLOGO DE DETALLES DEL DÍA ---
    if (showDetailsDialog && selectedDate != null) {
        val daySessions = sessions.filter {
            Instant.ofEpochMilli(it.session.date).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
        }

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text(selectedDate.toString(), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (daySessions.isNotEmpty()) {
                        Text("Entrenamientos programados:", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        daySessions.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.workoutTitle, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)

                                // Botón Vista Previa
                                IconButton(onClick = {
                                    previewWorkoutId = item.session.workoutPlanId
                                    scope.launch {
                                        previewExercises = viewModel.getExercisesForWorkoutSync(item.session.workoutPlanId)
                                    }
                                }) {
                                    Icon(Icons.Default.Info, null, tint = HarbizBlue)
                                }

                                // Botón Empezar
                                IconButton(onClick = {
                                    onStartWorkout(item.session.id, item.workoutTitle)
                                    showDetailsDialog = false
                                }) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF00C853))
                                }
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Opción para añadir más
                    TextButton(onClick = { showScheduleList = !showScheduleList }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showScheduleList) "Ocultar lista" else "Programar nueva rutina")
                    }

                    if (showScheduleList) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(templates) { plan ->
                                TextButton(onClick = {
                                    val millis = selectedDate!!.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    viewModel.scheduleWorkout(plan.id, millis)
                                    showScheduleList = false
                                }) { Text(plan.title) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDetailsDialog = false }) { Text("Cerrar") } }
        )
    }

    // --- DIÁLOGO DE VISTA PREVIA (EJERCICIOS) ---
    if (previewWorkoutId != null) {
        AlertDialog(
            onDismissRequest = { previewWorkoutId = null },
            title = { Text("Vista Previa") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(previewExercises) { ex ->
                        ListItem(
                            headlineContent = { Text(ex.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${ex.targetSets} series - ${if(ex.isVBT) "VBT" else "${ex.targetReps} reps"}") }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { previewWorkoutId = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun DayCell(day: CalendarDay, sessions: List<ScheduledSessionWithTitle>, onClick: () -> Unit) {
    if (day.position == DayPosition.MonthDate) {
        Column(
            modifier = Modifier
                .aspectRatio(0.7f)
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(4.dp)
        ) {
            val isToday = day.date == LocalDate.now()
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isToday) HarbizBlue else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = if (isToday) Color.White else Color.Black,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))

            sessions.take(2).forEach { session ->
                EventPill(session.workoutTitle)
            }
            if (sessions.size > 2) {
                Text(
                    text = "+${sessions.size - 2}",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    } else {
        Box(modifier = Modifier.aspectRatio(0.7f))
    }
}

@Composable
private fun EventPill(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(HarbizBlue.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Text(
            text = title,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = HarbizBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val firstDayOfWeek = firstDayOfWeekFromLocale()
    val daysOfWeek = DayOfWeek.values()
    val sortedDays = daysOfWeek.slice(firstDayOfWeek.value - 1 until daysOfWeek.size) +
            daysOfWeek.slice(0 until firstDayOfWeek.value - 1)

    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in sortedDays) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
package com.vbt.vbtpocket.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.*
import com.vbt.vbtpocket.data.*
import com.vbt.vbtpocket.engine.VbtBleManager
import com.vbt.vbtpocket.engine.VbtPhysicsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class ExerciseDraft(
    val id: Int,
    var name: String = "",
    var sets: String = "3",
    var restSeconds: String = "90",
    var restNextExerciseSeconds: String = "120",
    var isVBT: Boolean = true,
    var targetVelocity: String = "0.50",
    var velocityLoss: String = "20",
    var reps: String = "10"
)

data class VelocityPoint(val weight: Float, val velocity: Float)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val athleteDao = db.athleteDao()

    val athlete = athleteDao.getAthlete().asLiveData()
    val workouts = workoutDao.getAllWorkouts().asLiveData()
    val calendarSessions = workoutDao.getScheduledSessions().asLiveData()
    val library = workoutDao.getLibrary().asLiveData()

    val loggedExercises = workoutDao.getLoggedExercises().asLiveData()
    val latestReadiness = workoutDao.getLatestReadiness().asLiveData()

    // =========================================================================
    // MOTORES DEL ENCODER (HARDWARE, FÍSICA Y VOZ)
    // =========================================================================

    @SuppressLint("StaticFieldLeak")
    private val bleManager = VbtBleManager(application)
    private val physicsEngine = VbtPhysicsEngine()
    private var tts: TextToSpeech? = null

    private val _lastRepVelocity = MutableStateFlow(0f)
    val lastRepVelocity = _lastRepVelocity.asStateFlow()

    private val _isEncoderConnected = MutableStateFlow(false)
    val isEncoderConnected = _isEncoderConnected.asStateFlow()

    private val _encoderStatusMsg = MutableStateFlow("Desconectado")
    val encoderStatusMsg = _encoderStatusMsg.asStateFlow()

    // NUEVO: Lista de velocidades para la gráfica de la serie actual
    private val _currentSetReps = MutableStateFlow<List<Float>>(emptyList())
    val currentSetReps = _currentSetReps.asStateFlow()

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }

        viewModelScope.launch {
            try {
                if (workoutDao.getLibraryCount() == 0) {
                    val defaults = listOf(
                        ExerciseLibraryEntity(name = "Sentadilla Trasera", muscleGroup = "Piernas", description = "Flexión de rodillas con barra"),
                        ExerciseLibraryEntity(name = "Press Banca", muscleGroup = "Pecho", description = "Empuje horizontal con barra"),
                        ExerciseLibraryEntity(name = "Peso Muerto", muscleGroup = "Espalda/Piernas", description = "Levantamiento desde el suelo")
                    )
                    defaults.forEach { workoutDao.insertToLibrary(it) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        bleManager.onStatusMessage = { msg -> _encoderStatusMsg.value = msg }
        bleManager.onConnectionStateChange = { isConnected -> _isEncoderConnected.value = isConnected }

        // --- PROCESAMIENTO DE DATOS EN TIEMPO REAL ---
        bleManager.onDataReceived = { frames ->
            if (!physicsEngine.isCalibrated) physicsEngine.calibrateGravity(frames)

            frames.forEach { frame ->
                val repVelocity = physicsEngine.processFrame(frame)

                if (repVelocity != null) {
                    // 1. Actualizar última velocidad
                    _lastRepVelocity.value = repVelocity

                    // 2. Añadir a la lista de la serie (para la gráfica de barras)
                    _currentSetReps.value = _currentSetReps.value + repVelocity

                    // 3. Cantar velocidad
                    val textToSpeak = String.format(Locale.US, "%.2f", repVelocity)
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    // --- CONTROLES DEL ENCODER ---
    fun autoConnectEncoder() = bleManager.scanAndConnect()
    fun disconnectEncoder() = bleManager.disconnect()
    fun startEncoder() = bleManager.startStreaming()
    fun stopEncoder() = bleManager.stopStreaming()

    // Limpia los datos para empezar una serie nueva
    fun clearCurrentSetReps() {
        _currentSetReps.value = emptyList()
        _lastRepVelocity.value = 0f
        physicsEngine.reset()
    }

    fun resetPhysics() {
        physicsEngine.reset()
        _lastRepVelocity.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
        tts?.stop()
        tts?.shutdown()
    }

    // =========================================================================
    // MATEMÁTICAS Y VALORACIÓN DEPORTIVA
    // =========================================================================

    fun getLogsForExercise(name: String) = workoutDao.getLogsForExercise(name).asLiveData()

    fun calculateProfile(logs: List<WorkoutLogEntity>): List<VelocityPoint> {
        return logs.groupBy { it.weight }
            .map { (weight, logsAtWeight) -> VelocityPoint(weight, logsAtWeight.maxOf { it.velocity }) }
            .sortedBy { it.weight }
    }

    fun calculate1RMByLinearRegression(points: List<VelocityPoint>, isSquat: Boolean = true): Float {
        if (points.size < 2) return 0f
        val mvt = if (isSquat) 0.3f else 0.15f
        val n = points.size
        var sumX = 0f; var sumY = 0f; var sumXY = 0f; var sumX2 = 0f
        for (p in points) {
            sumX += p.velocity
            sumY += p.weight
            sumXY += p.velocity * p.weight
            sumX2 += p.velocity * p.velocity
        }
        val m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val b = (sumY - m * sumX) / n
        return ((m * mvt) + b).coerceAtLeast(0f)
    }

    fun calculateReadinessScore(readiness: ReadinessEntity?): Int {
        if (readiness == null) return 100
        val score = (readiness.sleep + (6 - readiness.stress) + (6 - readiness.fatigue) + (6 - readiness.soreness)) / 20f
        return (score * 100).toInt()
    }

    // =========================================================================
    // CRUD BASE DE DATOS
    // =========================================================================

    fun saveAthlete(athlete: AthleteEntity) = viewModelScope.launch { athleteDao.saveAthlete(athlete) }

    fun saveWorkout(title: String, exercises: List<ExerciseDraft>) = viewModelScope.launch {
        val workoutId = workoutDao.insertWorkout(WorkoutPlanEntity(title = title, description = ""))
        val entities = exercises.map {
            ExerciseEntity(
                workoutId = workoutId.toInt(), name = it.name, targetSets = it.sets.toIntOrNull() ?: 0,
                restSeconds = it.restSeconds.toIntOrNull() ?: 90,
                restNextExerciseSeconds = it.restNextExerciseSeconds.toIntOrNull() ?: 120,
                isVBT = it.isVBT, targetVelocity = if (it.isVBT) it.targetVelocity.toFloatOrNull() ?: 0.5f else 0f,
                velocityLoss = if (it.isVBT) it.velocityLoss.toIntOrNull() ?: 20 else 0,
                targetReps = if (!it.isVBT) it.reps.toIntOrNull() ?: 0 else 0
            )
        }
        workoutDao.insertExercises(entities)
    }

    suspend fun loadWorkoutForEditing(workoutId: Int): Pair<String, List<ExerciseDraft>> {
        val workout = workouts.value?.find { it.id == workoutId }
        val title = workout?.title ?: ""
        val exercises = workoutDao.getExercisesForWorkout(workoutId).map {
            ExerciseDraft(
                id = it.id, name = it.name, sets = it.targetSets.toString(),
                restSeconds = it.restSeconds.toString(), restNextExerciseSeconds = it.restNextExerciseSeconds.toString(),
                isVBT = it.isVBT, targetVelocity = it.targetVelocity.toString(),
                velocityLoss = it.velocityLoss.toString(), reps = it.targetReps.toString()
            )
        }
        return Pair(title, exercises)
    }

    fun updateWorkout(workoutId: Int, title: String, exercises: List<ExerciseDraft>) = viewModelScope.launch {
        workoutDao.updateWorkoutTitle(workoutId, title)
        workoutDao.deleteExercisesForWorkout(workoutId)
        val entities = exercises.map {
            ExerciseEntity(
                workoutId = workoutId, name = it.name, targetSets = it.sets.toIntOrNull() ?: 0,
                restSeconds = it.restSeconds.toIntOrNull() ?: 90, restNextExerciseSeconds = it.restNextExerciseSeconds.toIntOrNull() ?: 120,
                isVBT = it.isVBT, targetVelocity = if (it.isVBT) it.targetVelocity.toFloatOrNull() ?: 0.5f else 0f,
                velocityLoss = if (it.isVBT) it.velocityLoss.toIntOrNull() ?: 20 else 0, targetReps = if (!it.isVBT) it.reps.toIntOrNull() ?: 0 else 0
            )
        }
        workoutDao.insertExercises(entities)
    }

    fun scheduleWorkout(planId: Int, date: Long) = viewModelScope.launch { workoutDao.scheduleSession(ScheduledSessionEntity(date = date, workoutPlanId = planId, isRecurring = false)) }
    fun deleteWorkout(workout: WorkoutPlanEntity) = viewModelScope.launch { workoutDao.deleteWorkout(workout) }
    fun deleteSession(session: ScheduledSessionEntity) = viewModelScope.launch { workoutDao.deleteSession(session) }
    fun saveReadiness(sleep: Int, stress: Int, fatigue: Int, soreness: Int) = viewModelScope.launch { workoutDao.insertReadiness(ReadinessEntity(sleep = sleep, stress = stress, fatigue = fatigue, soreness = soreness)) }

    fun logSet(sessionId: Int, exerciseName: String, setNum: Int, weight: Float, reps: Int, rpe: Int, velocity: Float = 0f) = viewModelScope.launch {
        workoutDao.insertWorkoutLog(WorkoutLogEntity(sessionId = sessionId, exerciseName = exerciseName, setNumber = setNum, weight = weight, reps = reps, rpe = rpe, velocity = velocity))
    }

    fun insertExerciseToLibrary(exercise: ExerciseLibraryEntity) = viewModelScope.launch { workoutDao.insertToLibrary(exercise) }
    fun finishWorkoutWithNotes(sessionId: Int, notes: String) = viewModelScope.launch { workoutDao.finishSessionWithNotes(sessionId, notes) }
    suspend fun getExercisesForWorkoutSync(workoutId: Int): List<ExerciseEntity> = workoutDao.getExercisesForWorkout(workoutId)
}
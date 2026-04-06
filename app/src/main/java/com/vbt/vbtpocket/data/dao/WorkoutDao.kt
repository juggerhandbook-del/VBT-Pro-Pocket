package com.vbt.vbtpocket.data.dao

import androidx.room.*
import com.vbt.vbtpocket.data.*
import kotlinx.coroutines.flow.Flow
import com.vbt.vbtpocket.data.ScheduledSessionWithTitle

@Dao
interface WorkoutDao {
    @Insert suspend fun insertWorkout(workout: WorkoutPlanEntity): Long
    @Insert suspend fun insertExercises(exercises: List<ExerciseEntity>)
    @Query("SELECT * FROM workout_plans ORDER BY dateCreated DESC") fun getAllWorkouts(): Flow<List<WorkoutPlanEntity>>
    @Delete suspend fun deleteWorkout(workout: WorkoutPlanEntity)

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId") suspend fun getExercisesForWorkout(workoutId: Int): List<ExerciseEntity>
    @Query("UPDATE workout_plans SET title = :newTitle WHERE id = :workoutId") suspend fun updateWorkoutTitle(workoutId: Int, newTitle: String)
    @Query("DELETE FROM exercises WHERE workoutId = :workoutId") suspend fun deleteExercisesForWorkout(workoutId: Int)

    @Insert suspend fun scheduleSession(session: ScheduledSessionEntity)
    @Query("SELECT scheduled_sessions.*, workout_plans.title as workoutTitle FROM scheduled_sessions INNER JOIN workout_plans ON scheduled_sessions.workoutPlanId = workout_plans.id ORDER BY date ASC")
    fun getScheduledSessions(): Flow<List<ScheduledSessionWithTitle>>
    @Delete suspend fun deleteSession(session: ScheduledSessionEntity)

    @Query("SELECT * FROM exercise_library ORDER BY name ASC") fun getLibrary(): Flow<List<ExerciseLibraryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertToLibrary(exercise: ExerciseLibraryEntity)
    @Query("SELECT COUNT(*) FROM exercise_library") suspend fun getLibraryCount(): Int

    @Insert suspend fun insertReadiness(readiness: ReadinessEntity)
    @Query("SELECT * FROM readiness_logs ORDER BY date DESC LIMIT 1") fun getLatestReadiness(): Flow<ReadinessEntity?>

    @Insert suspend fun insertWorkoutLog(log: WorkoutLogEntity)
    @Query("SELECT * FROM workout_logs WHERE sessionId = :sessionId ORDER BY id ASC") fun getLogsForSession(sessionId: Int): Flow<List<WorkoutLogEntity>>

    @Query("UPDATE scheduled_sessions SET notes = :notes, isCompleted = 1 WHERE id = :sessionId")
    suspend fun finishSessionWithNotes(sessionId: Int, notes: String)

    // --- NUEVAS QUERIES PARA EL PERFIL VELOCIDAD-CARGA ---
    @Query("SELECT * FROM workout_logs WHERE exerciseName = :exerciseName AND velocity > 0 ORDER BY weight ASC")
    fun getLogsForExercise(exerciseName: String): Flow<List<WorkoutLogEntity>>

    @Query("SELECT DISTINCT exerciseName FROM workout_logs")
    fun getLoggedExercises(): Flow<List<String>>
}

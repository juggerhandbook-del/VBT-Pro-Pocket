package com.vbt.vbtpocket.data.dao
import androidx.room.*
import com.vbt.vbtpocket.data.AthleteEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface AthleteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAthlete(athlete: AthleteEntity)
    @Query("SELECT * FROM athlete_profile LIMIT 1") fun getAthlete(): Flow<AthleteEntity?>
}
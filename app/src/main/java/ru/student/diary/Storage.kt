package ru.student.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Entity
data class TrainingSet(@PrimaryKey(autoGenerate = true) val id: Long = 0, @Embedded val record: SetRecord)
@Dao
interface SetDao {
    @Query("SELECT * FROM TrainingSet ORDER BY day DESC, id DESC")
    fun observe(): Flow<List<TrainingSet>>
    @Insert suspend fun insert(set: TrainingSet)
}
@Database(entities = [TrainingSet::class], version = 1, exportSchema = false)
abstract class DiaryDb : RoomDatabase() { abstract fun sets(): SetDao }

sealed interface DiaryState {
    data object Loading : DiaryState
    data object Error : DiaryState
    data class Ready(val sets: List<TrainingSet>) : DiaryState
}
sealed interface Saving {
    data object Idle : Saving
    data object Busy : Saving
    data object Done : Saving
    data class Failed(val message: String) : Saving
}
class DiaryModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(app, DiaryDb::class.java, "diary.db").build()
    private val retryCount = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val diary = retryCount.flatMapLatest {
        db.sets().observe().map<List<TrainingSet>, DiaryState> { DiaryState.Ready(it) }
            .onStart { emit(DiaryState.Loading) }.catch { emit(DiaryState.Error) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryState.Loading)
    private val status = MutableStateFlow<Saving>(Saving.Idle)
    val saving = status.asStateFlow()
    fun retry() { retryCount.value++ }
    fun reset() { status.value = Saving.Idle }
    fun save(date: String, name: String, reps: String, weight: String) {
        if (status.value == Saving.Busy || status.value == Saving.Done) return
        val record = try { readSet(date, name, reps, weight) }
        catch (e: IllegalArgumentException) {
            status.value = Saving.Failed(e.message.orEmpty())
            return
        }
        status.value = Saving.Busy
        viewModelScope.launch {
            try {
                db.sets().insert(TrainingSet(record = record))
                status.value = Saving.Done
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { status.value = Saving.Failed("Не удалось сохранить подход. Попробуйте ещё раз.") }
        }
    }
    override fun onCleared() { db.close(); super.onCleared() }
}

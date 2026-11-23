package ru.student.diary

import org.junit.Assert.*
import org.junit.Test

class LogicTest {
    private fun sample(date: String = "23.11.2026", name: String = "Жим лёжа",
                       reps: String = "8", kg: String = "42,5") = readSet(date, name, reps, kg)

    @Test fun parsesDecimalComma() { assertEquals(42.5, sample().kg, 0.0) }
    @Test fun allowsBodyweight() { assertEquals(0.0, sample(kg = "0").kg, 0.0) }
    @Test fun rejectsImpossibleDate() { assertThrows(IllegalArgumentException::class.java) { sample(date = "31.02.2026") } }
    @Test fun rejectsBlankName() { assertThrows(IllegalArgumentException::class.java) { sample(name = " ") } }
    @Test fun rejectsZeroReps() { assertThrows(IllegalArgumentException::class.java) { sample(reps = "0") } }
    @Test fun rejectsNegativeWeight() { assertThrows(IllegalArgumentException::class.java) { sample(kg = "-1") } }
    @Test fun combinesSameDayAndFiltersExercise() {
        val records = listOf(sample(kg = "45"), sample(date = "20.11.2026", kg = "40"),
            sample(name = "  ЖИМ   ЛЁЖА ", kg = "42,5"), sample(name = "Присед", kg = "60"))
        assertEquals(listOf(40.0, 45.0), dailyBest(records, "жим лёжа").map { it.kg })
    }
    @Test fun emptyHistory() { assertTrue(dailyBest(emptyList(), "Присед").isEmpty()) }
}

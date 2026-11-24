package ru.student.diary

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)
data class SetRecord(val day: Long, val exercise: String, val reps: Int, val kg: Double)
fun exerciseKey(name: String) = name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
fun readSet(date: String, name: String, reps: String, weight: String): SetRecord {
    val day = runCatching { LocalDate.parse(date.trim(), dateFormat) }.getOrNull()
    require(day != null && day.year in 1..9999) { "Введите существующую дату в формате ДД.ММ.ГГГГ." }
    val exercise = name.trim().replace(Regex("\\s+"), " ")
    require(exercise.isNotEmpty()) { "Укажите упражнение." }
    val count = reps.trim().toIntOrNull()
    require(count != null && count > 0) { "Введите целое число повторений больше нуля." }
    val text = weight.trim().replace(',', '.')
    val kg = text.toDoubleOrNull()
    require(text.matches(Regex("""\d{1,3}(\.\d{1,2})?""")) && kg != null) {
        "Введите вес от 0 до 999,99 кг, до двух знаков после запятой."
    }
    return SetRecord(day.toEpochDay(), exercise, count, kg)
}
fun <T> exerciseHistory(entries: List<T>, name: String, record: (T) -> SetRecord, id: (T) -> Long): List<T> {
    val key = exerciseKey(name)
    return entries.filter { exerciseKey(record(it).exercise) == key }
        .sortedWith(compareBy<T> { record(it).day }.thenBy(id))
}

fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

fun number(value: Double) = java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')

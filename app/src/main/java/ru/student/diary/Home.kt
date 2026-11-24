package ru.student.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthDay = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))
private val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@Composable
fun Home(sets: List<TrainingSet>, add: () -> Unit, showDay: (Long) -> Unit) {
    val today = LocalDate.now()
    var weekOffset by rememberSaveable { mutableStateOf(0L) }
    val monday = weekStart(today).plusWeeks(weekOffset)
    val firstDay = monday.toEpochDay()
    val weekSets = remember(sets, firstDay) { sets.filter { it.record.day in firstDay..firstDay + 6 } }
    val days = remember(weekSets) { weekSets.groupingBy { it.record.day }.eachCount() }
    val latest = sets.firstOrNull()?.record

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Дневник тренировок", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (days.containsKey(today.toEpochDay()) && weekOffset == 0L) "Сегодня уже есть запись."
                        else "Запишите подход после упражнения."
                    )
                    Button(onClick = add) { Text("Новый подход") }
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1.15f)) { WeekCalendar(monday, today, days, { weekOffset += it }, showDay) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        WeekTotals(days.size, weekSets.size)
                        Latest(latest, showDay)
                    }
                }
            } else {
                WeekCalendar(monday, today, days, { weekOffset += it }, showDay)
                WeekTotals(days.size, weekSets.size)
                Latest(latest, showDay)
            }
        }
    }
}

@Composable
private fun WeekCalendar(
    monday: LocalDate, today: LocalDate, days: Map<Long, Int>,
    shift: (Long) -> Unit, showDay: (Long) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeekArrow("‹", "Предыдущая неделя") { shift(-1L) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Календарь", style = MaterialTheme.typography.titleMedium)
                    Text("${monday.format(monthDay)} – ${monday.plusDays(6).format(monthDay)} ${monday.plusDays(6).year}",
                        style = MaterialTheme.typography.bodySmall)
                }
                WeekArrow("›", "Следующая неделя") { shift(1L) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                weekdays.forEachIndexed { index, weekday ->
                    val date = monday.plusDays(index.toLong())
                    val day = date.toEpochDay()
                    val trained = days.containsKey(day)
                    val selected = date == today
                    val label = date.format(dateFormat) +
                        (if (selected) ", сегодня" else "") +
                        (if (trained) ", есть подходы" else ", без подходов")
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(weekday, style = MaterialTheme.typography.labelSmall)
                        Surface(
                            onClick = { showDay(day) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                .semantics { contentDescription = label },
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                trained -> MaterialTheme.colorScheme.primary
                                selected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (trained || selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Text("Заливкой отмечены дни с подходами. Нажмите на дату, чтобы открыть записи.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WeekArrow(symbol: String, label: String, click: () -> Unit) {
    IconButton(onClick = click, modifier = Modifier.semantics { contentDescription = label }) {
        Text(symbol, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun WeekTotals(days: Int, sets: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WeekNumber("Дней с записями", days, Modifier.weight(1f))
        WeekNumber("Подходов за неделю", sets, Modifier.weight(1f))
    }
}

@Composable
private fun WeekNumber(label: String, value: Int, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Latest(record: SetRecord?, showDay: (Long) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Последний подход", style = MaterialTheme.typography.titleMedium)
            if (record == null) {
                Text("Здесь появится последняя запись.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(record.exercise, fontWeight = FontWeight.Bold)
                Text("${number(record.kg)} кг · ${record.reps} повт. · ${LocalDate.ofEpochDay(record.day).format(dateFormat)}")
                TextButton(onClick = { showDay(record.day) }) { Text("Открыть день") }
            }
        }
    }
}

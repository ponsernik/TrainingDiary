package ru.student.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun Journal(entries: List<TrainingSet>, day: Long?, clearDay: () -> Unit, add: () -> Unit) {
    val groups = remember(entries, day) {
        entries.filter { day == null || it.record.day == day }.groupBy { it.record.day }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Button(onClick = add) { Text("Новый подход") } }
        if (day != null) item {
            Text("Записи за ${LocalDate.ofEpochDay(day).format(dateFormat)}")
            TextButton(onClick = clearDay) { Text("Все даты") }
        }
        if (groups.isEmpty()) item {
            Text(if (day == null) "Пока нет подходов." else "За этот день подходов нет.")
        }
        groups.forEach { (date, sets) ->
            item(key = "day:$date") {
                Text(LocalDate.ofEpochDay(date).format(dateFormat), style = MaterialTheme.typography.titleMedium)
            }
            items(sets, key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entry.record.exercise, style = MaterialTheme.typography.titleMedium)
                        Text("${entry.record.reps} повт. · ${number(entry.record.kg)} кг")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSet(entries: List<TrainingSet>, status: Saving, save: (String, String, String, String) -> Unit) {
    var day by rememberSaveable { mutableStateOf(LocalDate.now().format(dateFormat)) }
    var name by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }
    val enabled = status != Saving.Busy && status != Saving.Done
    val names = remember(entries) { exerciseNames(entries) }
    if (picking) {
        ExercisePicker(names, allowNew = true, close = { picking = false }) {
            name = it
            picking = false
        }
    }
    Column(
        Modifier.widthIn(max = 570.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Field("Дата (ДД.ММ.ГГГГ)", "date", day, enabled) { day = it }
        Text("Упражнение", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = { picking = true }, modifier = Modifier.fillMaxWidth().testTag("chooseExercise"),
            enabled = enabled
        ) { Text(name.ifBlank { "Выбрать упражнение" }) }
        Field("Повторения", "reps", reps, enabled, KeyboardType.Number) { reps = it }
        Field("Вес, кг", "kg", kg, enabled, KeyboardType.Decimal) { kg = it }
        (status as? Saving.Failed)?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { save(day, name, reps, kg) }, enabled = enabled) {
            Text(if (status == Saving.Busy) "Сохранение…" else "Сохранить")
        }
    }
}

fun exerciseNames(entries: List<TrainingSet>): List<String> = entries
    .map { it.record.exercise }.distinctBy(::exerciseKey)

@Composable
fun ExercisePicker(names: List<String>, allowNew: Boolean, close: () -> Unit, choose: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val key = exerciseKey(query)
    val matching = remember(names, key) { names.filter { exerciseKey(it).contains(key) } }
    val canCreate = allowNew && key.isNotBlank() && matching.none { exerciseKey(it) == key }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Упражнение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(if (allowNew) "Поиск или новое название" else "Поиск", "exerciseSearch", query, true) { query = it }
                if (matching.isEmpty() && !canCreate) {
                    Text(if (names.isEmpty()) "Введите название первого упражнения." else "Упражнения не найдены.")
                }
                LazyColumn(Modifier.heightIn(max = 210.dp)) {
                    items(matching) { name ->
                        TextButton(onClick = { choose(name) }, modifier = Modifier.fillMaxWidth()) { Text(name) }
                    }
                }
            }
        },
        confirmButton = {
            if (canCreate) TextButton(onClick = { choose(query.trim()) }) { Text("Добавить упражнение") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Закрыть") } }
    )
}

@Composable
private fun Field(
    label: String, tag: String, value: String, enabled: Boolean,
    type: KeyboardType = KeyboardType.Text, change: (String) -> Unit
) {
    OutlinedTextField(
        value, change, Modifier.fillMaxWidth().testTag(tag), enabled = enabled,
        singleLine = true, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = type)
    )
}

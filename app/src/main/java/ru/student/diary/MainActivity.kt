package ru.student.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDate
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val model by viewModels<DiaryModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF82DAC2))
                         else lightColorScheme(primary = Color(0xFF006A57))
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize()) { Diary(model) }
            }
        }
    }
}

@Composable
fun Diary(model: DiaryModel) {
    val nav = rememberNavController()
    val state by model.diary.collectAsState()
    NavHost(nav, startDestination = "journal", modifier = Modifier.padding(16.dp)) {
        composable("journal") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Title("Журнал")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { model.reset(); nav.navigate("add") }) { Text("Новый подход") }
                    OutlinedButton(onClick = { nav.navigate("progress") }) { Text("Прогресс") }
                }
                Loaded(state, model::retry) { entries ->
                    if (entries.isEmpty()) Text("Пока нет подходов. Запишите первый подход после тренировки.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        entries.groupBy { it.record.day }.forEach { (day, sets) ->
                            item { Text(dateFormat.format(LocalDate.ofEpochDay(day)), style = MaterialTheme.typography.titleMedium) }
                            items(sets, key = { it.id }) { entry ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(entry.record.exercise, style = MaterialTheme.typography.titleMedium)
                                        Text("${entry.record.reps} повт. · ${number(entry.record.kg)} кг")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        composable("add") { Add(model) { nav.popBackStack() } }
        composable("progress") {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { nav.popBackStack() }) { Text("Назад") }
                Title("Прогресс")
                Loaded(state, model::retry) { Progress(it.map { entry -> entry.record }) }
            }
        }
    }
}
@Composable
private fun Title(text: String) { Text(text, style = MaterialTheme.typography.headlineLarge) }

@Composable
private fun Loaded(state: DiaryState, retry: () -> Unit, content: @Composable (List<TrainingSet>) -> Unit) {
    when (state) {
        DiaryState.Loading -> CircularProgressIndicator()
        DiaryState.Error -> {
            Text("Не удалось открыть журнал.")
            OutlinedButton(onClick = retry) { Text("Повторить") }
        }
        is DiaryState.Ready -> content(state.sets)
    }
}
@Composable
private fun Add(model: DiaryModel, back: () -> Unit) {
    var day by rememberSaveable { mutableStateOf(LocalDate.now().format(dateFormat)) }
    var name by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("") }
    val status by model.saving.collectAsState()
    val enabled = status != Saving.Busy && status != Saving.Done
    BackHandler(enabled = !enabled) {}
    LaunchedEffect(status) { if (status == Saving.Done) { model.reset(); back() } }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = back, enabled = enabled) { Text("Назад") }
        Title("Новый подход")
        Field("Дата (ДД.ММ.ГГГГ)", "date", day, enabled) { day = it }
        Field("Упражнение", "name", name, enabled) { name = it }
        Field("Повторения", "reps", reps, enabled, KeyboardType.Number) { reps = it }
        Field("Вес, кг", "kg", kg, enabled, KeyboardType.Decimal) { kg = it }
        (status as? Saving.Failed)?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { model.save(day, name, reps, kg) }, enabled = enabled) {
            Text(if (status == Saving.Busy) "Сохранение…" else "Сохранить")
        }
    }
}
@Composable
private fun Field(label: String, tag: String, value: String, enabled: Boolean,
                  type: KeyboardType = KeyboardType.Text, change: (String) -> Unit) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth().testTag(tag), enabled = enabled,
        singleLine = true, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = type))
}
@Composable
private fun Progress(records: List<SetRecord>) {
    if (records.isEmpty()) { Text("График появится после первого подхода."); return }
    val names = records.map { it.exercise }.distinctBy(::exerciseKey)
    var chosen by rememberSaveable { mutableStateOf(names.first()) }
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(chosen) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            names.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { chosen = name; open = false }) }
        }
    }
    val points = dailyBest(records, chosen)
    Text("Максимальный вес за день, кг", style = MaterialTheme.typography.titleMedium)
    val ink = MaterialTheme.colorScheme.onSurface.toArgb()
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(factory = { LineChart(it) }, modifier = Modifier.fillMaxWidth().height(240.dp), update = { chart ->
        val origin = points.first().day
        val entries = points.map { Entry((it.day - origin).toFloat(), it.kg.toFloat()) }
        val curve = LineDataSet(entries, chosen).apply {
            color = accent; setCircleColor(accent); setDrawCircleHole(false)
            lineWidth = 2.5f; circleRadius = 4f; setDrawValues(false)
        }
        chart.apply {
            data = LineData(curve)
            description.isEnabled = false; legend.isEnabled = false
            axisRight.isEnabled = false; axisLeft.textColor = ink; axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = maxOf(1f, entries.maxOf { it.y } * 1.15f)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM; textColor = ink; granularity = 1f
                axisMinimum = if (entries.size == 1) -1f else 0f
                axisMaximum = maxOf(1f, entries.last().x); setLabelCount(3, false)
                setAvoidFirstLastClipping(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        if (value < 0 || value > entries.last().x) return ""
                        return LocalDate.ofEpochDay(origin + value.roundToInt()).format(dateFormat).take(5)
                    }
                }
            }
            setTouchEnabled(false); notifyDataSetChanged(); invalidate()
        }
    })
    points.forEach { Text("${LocalDate.ofEpochDay(it.day).format(dateFormat)} — ${number(it.kg)} кг") }
}

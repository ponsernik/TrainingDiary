package ru.student.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun Charts(entries: List<TrainingSet>) {
    val names = remember(entries) { exerciseNames(entries) }
    if (names.isEmpty()) {
        Text("Добавьте подход в дневник, чтобы увидеть график.", Modifier.padding(16.dp))
        return
    }

    var selectedKey by rememberSaveable { mutableStateOf("") }
    val chosen = names.find { exerciseKey(it) == selectedKey } ?: names.first()
    var picking by rememberSaveable { mutableStateOf(false) }
    val history = remember(entries, chosen) { exerciseHistory(entries, chosen, TrainingSet::record, TrainingSet::id) }
    var focused by rememberSaveable(exerciseKey(chosen)) { mutableStateOf<Int?>(null) }
    val selected = focused?.takeIf { it in history.indices } ?: history.lastIndex

    if (picking) ExercisePicker(names, allowNew = false, close = { picking = false }) {
        selectedKey = exerciseKey(it)
        picking = false
    }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Вес по подходам", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = { picking = true }) { Text(chosen) }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Вес, кг", style = MaterialTheme.typography.labelLarge)
                    WeightChart(history) { focused = it }
                    Text("Номер подхода", style = MaterialTheme.typography.labelLarge)
                    if (history.size > 1) Text("Нажмите на точку, чтобы посмотреть подход.",
                        style = MaterialTheme.typography.bodySmall)
                    Text(setLabel(selected, history[selected].record), Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item { Text("История подходов", style = MaterialTheme.typography.titleMedium) }
        itemsIndexed(history, key = { _, entry -> entry.id }) { index, entry ->
            Text(setLabel(index, entry.record))
        }
    }
}

private fun setLabel(index: Int, record: SetRecord): String =
    "№${index + 1} · ${number(record.kg)} кг · ${record.reps} повт. · ${LocalDate.ofEpochDay(record.day).format(dateFormat)}"

@Composable
private fun WeightChart(history: List<TrainingSet>, select: (Int?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ink = colors.onSurface.toArgb()
    val grid = colors.outlineVariant.toArgb()
    val accent = colors.primary.toArgb()
    val onSelect by rememberUpdatedState(select)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val height = if (maxWidth >= 600.dp) 300.dp else 215.dp
        key(history) {
            AndroidView(factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    axisRight.isEnabled = false
                    setScaleYEnabled(false)
                    setDoubleTapToZoomEnabled(false)
                    isDragEnabled = history.size > 1
                    setExtraOffsets(4f, 5f, 10f, 4f)
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(entry: Entry, highlight: Highlight) {
                            onSelect(entry.x.roundToInt() - 1)
                        }
                        override fun onNothingSelected() { onSelect(null) }
                    })
                }
            }, modifier = Modifier.fillMaxWidth().height(height), update = { chart ->
                val points = history.mapIndexed { i, entry -> Entry(i + 1f, entry.record.kg.toFloat()) }
                val line = LineDataSet(points, "Вес, кг").apply {
                    mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                    color = accent
                    setCircleColor(accent)
                    setDrawCircleHole(false)
                    lineWidth = 2.75f
                    circleRadius = 4f
                    setDrawValues(false)
                    setDrawHorizontalHighlightIndicator(false)
                    highLightColor = accent
                }
                chart.axisLeft.apply {
                    textColor = ink
                    gridColor = grid
                    axisMinimum = 0f
                    axisMaximum = maxOf(1f, points.maxOf { it.y } * 1.15f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float) = java.math.BigDecimal(value.toString())
                            .setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')
                    }
                }
                chart.xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = ink
                    granularity = 1f
                    axisMinimum = if (points.size == 1) 0f else 0.8f
                    axisMaximum = points.last().x + if (points.size == 1) 1f else 0.2f
                    setLabelCount(minOf(6, maxOf(2, points.size)), false)
                    setAvoidFirstLastClipping(true)
                    setDrawGridLines(false)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String =
                            value.roundToInt().takeIf { it in 1..points.size }?.toString().orEmpty()
                    }
                }
                chart.data = LineData(line)
                chart.invalidate()
            })
        }
    }
}

package ru.student.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val model by viewModels<DiaryModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            var theme by remember { mutableStateOf(preferences.getString("theme", null)) }
            val dark = theme?.let { it == "dark" } ?: isSystemInDarkTheme()
            val colors = if (dark) darkColorScheme(
                primary = Color(0xFF8DDFC6), onPrimary = Color(0xFF00382D),
                primaryContainer = Color(0xFF174D40), onPrimaryContainer = Color(0xFFADFAE0),
                surface = Color(0xFF121B18), surfaceVariant = Color(0xFF283731),
                onSurface = Color(0xFFE3EFEB), onSurfaceVariant = Color(0xFFC0D1C9)
            ) else lightColorScheme(
                primary = Color(0xFF006653), onPrimary = Color.White,
                primaryContainer = Color(0xFFCBF3E5), onPrimaryContainer = Color(0xFF07392D),
                surface = Color(0xFFF7FAF8), surfaceVariant = Color(0xFFE6F0EB),
                onSurface = Color(0xFF172722), onSurfaceVariant = Color(0xFF405750)
            )
            MaterialTheme(colorScheme = colors) {
                Diary(model, dark) { useDark ->
                    val selected = if (useDark) "dark" else "light"
                    preferences.edit().putString("theme", selected).apply()
                    theme = selected
                }
            }
        }
    }
}

private enum class Tab(val title: String) {
    HOME("Главная"), JOURNAL("Дневник"), CHARTS("Графики")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Diary(model: DiaryModel, dark: Boolean, chooseTheme: (Boolean) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var appearance by rememberSaveable { mutableStateOf(false) }
    var filterDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val state by model.diary.collectAsState()
    val saving by model.saving.collectAsState()
    val canClose = saving != Saving.Busy && saving != Saving.Done
    val holder = rememberSaveableStateHolder()
    fun openForm() {
        model.reset()
        adding = true
    }
    LaunchedEffect(saving, adding) {
        if (adding && saving == Saving.Done) {
            adding = false
            filterDay = null
            tab = Tab.JOURNAL
            model.reset()
        }
    }
    BackHandler(enabled = adding || tab != Tab.HOME) {
        if (!adding) tab = Tab.HOME
        else if (canClose) adding = false
    }
    if (appearance) {
        AlertDialog(
            onDismissRequest = { appearance = false },
            title = { Text("Тема приложения") },
            text = {
                Column {
                    listOf(false to "Светлая", true to "Тёмная").forEach { (option, label) ->
                        TextButton(onClick = { chooseTheme(option); appearance = false }) {
                            Text(if (dark == option) "✓  $label" else label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { appearance = false }) { Text("Закрыть") } }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (adding) "Новый подход" else tab.title) },
                navigationIcon = {
                    if (adding) TextButton(onClick = { adding = false }, enabled = canClose) { Text("Назад") }
                },
                actions = {
                    if (!adding) IconButton(onClick = { appearance = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Тема приложения")
                    }
                }
            )
        },
        bottomBar = {
            if (!adding) NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = {
                            if (item == Tab.JOURNAL && item != tab) filterDay = null
                            tab = item
                        },
                        icon = {
                            when (item) {
                                Tab.HOME -> Icon(Icons.Default.Home, contentDescription = null)
                                Tab.JOURNAL -> Icon(Icons.Default.List, contentDescription = null)
                                Tab.CHARTS -> ChartIcon()
                            }
                        },
                        label = { Text(item.title, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 1000.dp).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                if (adding) {
                    val entries = (state as? DiaryState.Ready)?.sets.orEmpty()
                    AddSet(entries, saving, model::save)
                } else {
                    holder.SaveableStateProvider(tab.name) {
                        when (val current = state) {
                            DiaryState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            DiaryState.Error -> Column(Modifier.padding(20.dp)) {
                                Text("Не удалось открыть дневник.")
                                OutlinedButton(onClick = model::retry) { Text("Повторить") }
                            }
                            is DiaryState.Ready -> when (tab) {
                                Tab.HOME -> Home(current.sets, ::openForm) { day ->
                                    filterDay = day
                                    tab = Tab.JOURNAL
                                }
                                Tab.JOURNAL -> Journal(current.sets, filterDay, { filterDay = null }, ::openForm)
                                Tab.CHARTS -> Charts(current.sets)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartIcon() {
    val ink = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val points = listOf(Offset(3f, 18f), Offset(9f, 11f), Offset(14f, 14f), Offset(21f, 5f))
            .map { Offset(it.x.dp.toPx(), it.y.dp.toPx()) }
        points.zipWithNext().forEach { (start, end) -> drawLine(ink, start, end, strokeWidth = 2.dp.toPx()) }
    }
}

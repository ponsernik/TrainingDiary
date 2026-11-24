package ru.student.diary

import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class DemoTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val folder get() = File(instrumentation.targetContext.getExternalFilesDir(null), "demo")
    private fun hold(seconds: Int) = SystemClock.sleep(seconds * 1000L)
    private fun shot(name: String) {
        ui.waitForIdle()
        folder.mkdirs()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(folder, name + ".png").outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }
    private fun add(date: java.time.LocalDate, name: String, kg: String, create: Boolean = false) {
        ui.onNodeWithText("Новый подход").performClick()
        ui.onNodeWithTag("chooseExercise").performClick()
        if (create) {
            ui.onNodeWithTag("exerciseSearch").performTextInput(name)
            ui.onNodeWithText("Добавить упражнение").performClick()
        } else {
            ui.onNodeWithText(name).performClick()
        }
        listOf("date" to date.format(dateFormat), "reps" to "8", "kg" to kg).forEach { (tag, value) ->
            ui.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
        }
        if (kg == "40") { shot("02-form"); hold(5) }
        ui.onNodeWithText("Сохранить").performScrollTo().performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText("Новый подход").fetchSemanticsNodes().isNotEmpty() &&
            ui.onAllNodesWithTag("chooseExercise").fetchSemanticsNodes().isEmpty() }
        hold(2)
    }

    private fun tab(name: String) {
        ui.onAllNodesWithText(name).onLast().performClick()
    }

    @Test fun recordDemo() {
        val today = java.time.LocalDate.now()
        val monday = weekStart(today)
        ui.waitUntil(10000) { ui.onAllNodesWithText("Календарь").fetchSemanticsNodes().isNotEmpty() }
        shot("01-empty"); hold(7)
        add(monday, "Жим лёжа", "40", create = true)
        add(monday, "Жим лёжа", "42,5")
        add(today, "Жим лёжа", "45")
        add(today, "Присед", "60", create = true)
        tab("Главная")
        ui.onNodeWithText("Сегодня уже есть запись.").assertExists()
        shot("03-home"); hold(6)
        tab("Графики")
        ui.onNodeWithText("Присед").performClick()
        ui.onNodeWithText("Жим лёжа").performClick()
        ui.onAllNodesWithText("№3 · 45 кг", substring = true).onFirst().assertExists()
        shot("04-progress"); hold(9)
        ui.onNodeWithContentDescription("Тема приложения").performClick()
        ui.onNodeWithText("Тёмная", substring = true).performClick()
        shot("05-dark"); hold(7)
    }
}

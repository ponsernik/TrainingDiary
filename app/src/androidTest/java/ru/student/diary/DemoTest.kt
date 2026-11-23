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
    private fun add(date: String, name: String, kg: String, first: Boolean = false) {
        ui.onNodeWithText("Новый подход").performClick()
        listOf("date" to date, "name" to name, "reps" to "8", "kg" to kg).forEach { (tag, text) ->
            ui.onNodeWithTag(tag).performTextReplacement(text)
            if (first) hold(2)
        }
        if (first) { shot("02-form"); hold(4) }
        ui.onNodeWithText("Сохранить").performScrollTo().performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText("Журнал").fetchSemanticsNodes().isNotEmpty() }
        hold(2)
    }
    @Test fun recordDemo() {
        ui.waitUntil(10000) { ui.onAllNodesWithText("Пока нет подходов.", substring = true).fetchSemanticsNodes().isNotEmpty() }
        shot("01-empty"); hold(7)
        add("10.11.2026", "Жим лёжа", "40", true)
        add("12.11.2026", "Жим лёжа", "42,5")
        add("12.11.2026", "Жим лёжа", "45")
        add("14.11.2026", "Жим лёжа", "47,5")
        add("14.11.2026", "Присед", "60")
        shot("03-journal"); hold(6)
        ui.onNodeWithText("Прогресс").performClick()
        ui.onNodeWithText("Присед").performClick()
        ui.onNodeWithText("Жим лёжа").performClick()
        ui.onNodeWithText("12.11.2026 — 45 кг").assertExists()
        shot("04-progress"); hold(9)
        ui.onNodeWithText("Жим лёжа").performClick()
        ui.onNodeWithText("Присед").performClick()
        ui.onNodeWithText("14.11.2026 — 60 кг").assertExists()
        hold(5)
        ui.onNodeWithText("Назад").performClick()
        ui.onNodeWithText("Новый подход").performClick()
        ui.onNodeWithText("Сохранить").performScrollTo().performClick()
        ui.onNodeWithText("Укажите упражнение.").assertExists()
        shot("05-error"); hold(7)
    }
}

package com.simplejim.tracker

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simplejim.tracker.data.WorkoutDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Only run on a disposable test app or emulator. */
@RunWith(AndroidJUnit4::class)
class CardioLoggingSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun logsTwoActivitiesAndReopensTheirHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = WorkoutDatabase.getInstance(context).cardioDao()
        val before = runBlocking { dao.entries().map { it.id }.toSet() }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("Cardio", substring = false).performClick()
            type("Duration", "26:15")
            type("Distance (mi)", "2.5")
            type("Incline (%) (optional)", "-1.5")
            save(scenario, 1) { runBlocking { dao.entries().count { it.id !in before } } }

            scrollTo("Stair machine")
            compose.onNodeWithText("Stair machine", substring = false).performClick()
            type("Duration", "15:30")
            type("Floors", "70.5")
            type("Displayed level (optional)", "7")
            save(scenario, 2) { runBlocking { dao.entries().count { it.id !in before } } }
        }

        val saved = runBlocking { dao.entries().filter { it.id !in before }.associateBy { it.kind } }
        assertEquals(setOf("TREADMILL", "STAIRS"), saved.keys)
        assertEquals(-1500L, saved.getValue("TREADMILL").inclineThousandths)
        assertEquals(70500L, saved.getValue("STAIRS").floorThousandths)
        assertEquals(7000L, saved.getValue("STAIRS").levelThousandths)

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Cardio", substring = false).performClick()
            // Exercise the UI's history load and distinct optional fields.
            compose.waitUntil(timeoutMillis = 5_000) {
                runCatching { scrollTo("Stair machine · 15:30") }.isSuccess
            }
            compose.onNodeWithText("70.5 floors · 4.5 floors/min average · Level 7").assertIsDisplayed()
            scrollTo("Treadmill · 26:15")
            compose.onNodeWithText("2.5 mi · 10:30/mi average · -1.5% incline").assertIsDisplayed()
        }
    }

    private fun type(label: String, value: String) {
        scrollTo(label)
        compose.onNode(hasSetTextAction() and hasText(label, substring = false)).performTextReplacement(value)
    }

    private fun save(scenario: ActivityScenario<MainActivity>, count: Int, countEntries: () -> Int) {
        scenario.onActivity { activity ->
            ViewCompat.getWindowInsetsController(activity.window.decorView)?.hide(WindowInsetsCompat.Type.ime())
        }
        scrollTo("Save cardio")
        compose.onNodeWithText("Save cardio", substring = false).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { countEntries() == count }
    }

    private fun scrollTo(label: String) {
        compose.onNode(verticalList).performScrollToNode(hasText(label, substring = false))
    }

    private companion object {
        val verticalList = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    }
}

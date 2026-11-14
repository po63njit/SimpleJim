package com.simplejim.tracker

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simplejim.tracker.data.WorkoutRepository
import com.simplejim.tracker.data.WorkoutSet
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run only on a dedicated test emulator or device; this adds one synthetic workout. */
@RunWith(AndroidJUnit4::class)
class WorkoutLoggingSmokeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun copyEditAndSaveExactlyOneWorkoutThenRelaunchActivity() {
        val name = "Smoke Squat " + UUID.randomUUID().toString().take(8)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = WorkoutRepository(context)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Quick add", substring = false).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(verticalList).performScrollToNode(hasText("Name (e.g. Squat)"))
            compose.onNode(editable("Name (e.g. Squat)")).performTextReplacement(name)
            compose.onNode(editable("Weight (lb)")).performTextReplacement("95")
            compose.onNode(editable("Reps")).performTextReplacement("8")

            openAction("Copy last set")
            compose.onAllNodes(editable("Weight (lb)")).assertCountEquals(2)
            compose.onAllNodes(editable("Reps")).assertCountEquals(2)
            compose.onAllNodes(hasSetTextAction() and hasText("95")).assertCountEquals(2)
            compose.onAllNodes(hasSetTextAction() and hasText("8")).assertCountEquals(2)
            compose.onAllNodes(editable("Reps"))[1].performTextReplacement("6")

            // The fixed save dock is deliberately hidden while the keyboard is showing.
            scenario.onActivity { activity ->
                ViewCompat.getWindowInsetsController(activity.window.decorView)
                    ?.hide(WindowInsetsCompat.Type.ime())
            }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Save Workout", substring = false).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Save Workout", substring = false).performClick()

            // Wait for the durable session and cleared draft before closing the Activity.
            compose.waitUntil(timeoutMillis = 5_000) {
                runBlocking { repository.loadWorkouts().any { workout -> workout.exercises.any { it.name == name } } }
            }
            val saved = runBlocking {
                repository.loadWorkouts().filter { workout -> workout.exercises.any { it.name == name } }
            }
            assertEquals(1, saved.size)
            assertNull(runBlocking { repository.readDraft().payloadJson })
            assertEquals(listOf(WorkoutSet(95.0, 8), WorkoutSet(95.0, 6)), saved.single().exercises.single().sets)
            assertHistoryCard(name)
        }

        // New Activity/ViewModel; this is not an OS process restart.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Quick add", substring = false).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("History", substring = false).performClick()
            assertHistoryCard(name)
        }
    }

    private fun openAction(label: String) {
        compose.onNode(verticalList).performScrollToNode(hasText(label, substring = false))
        compose.onNodeWithText(label, substring = false).performClick()
    }

    private fun assertHistoryCard(name: String) {
        compose.onNode(verticalList).performScrollToNode(hasText(name, substring = false))
        compose.onAllNodesWithText(name, substring = false).assertCountEquals(1)
        compose.onAllNodesWithText("95.0 lb × 8  ·  95.0 lb × 6", substring = false)[0].assertIsDisplayed()
    }

    private fun editable(label: String) = hasSetTextAction() and hasText(label, substring = false)

    private companion object {
        val verticalList = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    }
}

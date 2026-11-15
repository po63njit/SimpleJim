package com.simplejim.tracker.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simplejim.tracker.StrengthHistoryState
import com.simplejim.tracker.strength.EnteredLoad
import com.simplejim.tracker.strength.LoadUnit
import com.simplejim.tracker.strength.SavedStrengthSet
import com.simplejim.tracker.strength.calculateStrengthHistory
import com.simplejim.tracker.ui.theme.SimpleJimTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrengthHistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyHistoryDoesNotSuggestAnUnsavedSetIsAvailable() {
        show(StrengthHistoryState(isOpen = true))
        compose.onNodeWithText("No complete saved strength sets to show.", substring = true).assertExists()
    }

    @Test
    fun actualSingleEstimateAndMissingEstimateHaveDifferentLabels() {
        val profile = calculateStrengthHistory(listOf(
            SavedStrengthSet(1, 100, "Squat", EnteredLoad(100.0, LoadUnit.Pounds), 1),
            SavedStrengthSet(2, 200, "Squat", EnteredLoad(90.0, LoadUnit.Pounds), 6),
        ))
        show(StrengthHistoryState(isOpen = true, exercises = profile, selectedKey = "squat"))
        showAssertion("Newest first.", substring = true)
        showAssertion("Show oldest first")
        compose.onNodeWithText("Show oldest first").performClick()
        showAssertion("Oldest first.", substring = true)
        showAssertion("Heaviest logged single: 100 lb, 1 rep,", substring = true)
        showAssertion("A logged single is not necessarily a tested max.")
        showAssertion("Estimated 1RM: no qualifying set")
        showAssertion("Estimated 1RM: 108 lb")
        showAssertion("Estimate from: 90 lb, 6 reps,", substring = true)
    }

    private fun show(state: StrengthHistoryState) {
        compose.setContent {
            SimpleJimTheme {
                StrengthHistoryScreen(Modifier, state, {}, {}, {})
            }
        }
    }

    private fun showAssertion(label: String, substring: Boolean = false) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText(label, substring = substring))
        compose.onNodeWithText(label, substring = substring).assertExists()
    }
}

package com.financetracker.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessDeathRestorationInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun addEditTransaction_stateSurvivesActivityRecreation() {
    // 1. Navigate to Add tab
    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("New Transaction").assertIsDisplayed()

    // 2. Select Income transaction type
    composeTestRule.onNodeWithText("Income").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Salary & Income").assertIsDisplayed()

    // 3. Add 50.000 via quick-add preset
    composeTestRule.onNodeWithText("+Rp50.000").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("50.000").assertIsDisplayed()

    // 4. Enter note in the Note text field
    composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput("SavedState death roundtrip")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("SavedState death roundtrip").assertIsDisplayed()

    // 5. Force real ActivityScenario recreate (onSaveInstanceState -> onDestroy -> onCreate round-trip)
    composeTestRule.activityRule.scenario.recreate()
    composeTestRule.waitForIdle()

    // 6. Verify restored state after recreation
    composeTestRule.onNodeWithText("New Transaction").assertIsDisplayed()
    composeTestRule.onNodeWithText("Salary & Income").assertIsDisplayed()
    composeTestRule.onNodeWithText("50.000").assertIsDisplayed()
    composeTestRule.onNodeWithText("SavedState death roundtrip").assertIsDisplayed()
  }
}

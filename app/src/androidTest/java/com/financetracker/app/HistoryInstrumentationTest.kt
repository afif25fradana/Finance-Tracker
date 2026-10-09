package com.financetracker.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private fun addTransaction(amountPreset: String, categoryName: String, note: String) {
    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(amountPreset).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(categoryName).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput(note)
    composeTestRule.waitForIdle()
    closeSoftKeyboard()

    composeTestRule.onNodeWithText("Save").performScrollTo().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun historyScreen_filtersTransactionsBySearchQueryAndRestoresOnClear() {
    addTransaction("+Rp25.000", "Dining & Cafes", "Coffee at cafe")
    addTransaction("+Rp50.000", "Transport & Fuel", "Monthly subway pass")

    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").assertIsSelected()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("history", it.navController?.currentBackStackEntry?.destination?.route)
    }

    composeTestRule.onNodeWithText("Coffee at cafe").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Monthly subway pass").performScrollTo().assertIsDisplayed()

    composeTestRule.onNode(hasSetTextAction()).performTextInput("Coffee")
    composeTestRule.waitForIdle()
    closeSoftKeyboard()

    composeTestRule.onNodeWithText("Coffee at cafe").assertIsDisplayed()
    composeTestRule.onNodeWithText("Monthly subway pass").assertDoesNotExist()

    composeTestRule.onNodeWithContentDescription("Clear").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Coffee at cafe").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Monthly subway pass").performScrollTo().assertIsDisplayed()
  }
}

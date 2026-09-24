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
    // 1. Seed two distinguishable transactions via the Add flow
    addTransaction("+Rp25.000", "Dining & Cafes", "Coffee at cafe")
    addTransaction("+Rp50.000", "Transport & Fuel", "Monthly subway pass")

    // 2. Navigate to History tab
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").assertIsSelected()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("history", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 3. Confirm both transactions are visible in the unfiltered list
    composeTestRule.onNodeWithText("Coffee at cafe").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Monthly subway pass").performScrollTo().assertIsDisplayed()

    // 4. Enter search query "Coffee"
    composeTestRule.onNode(hasSetTextAction()).performTextInput("Coffee")
    composeTestRule.waitForIdle()
    closeSoftKeyboard()

    // 5. Assert only the matching transaction is displayed
    composeTestRule.onNodeWithText("Coffee at cafe").assertIsDisplayed()
    composeTestRule.onNodeWithText("Monthly subway pass").assertDoesNotExist()

    // 6. Clear search query and confirm all transactions reappear
    composeTestRule.onNodeWithContentDescription("Clear").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Coffee at cafe").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Monthly subway pass").performScrollTo().assertIsDisplayed()
  }
}

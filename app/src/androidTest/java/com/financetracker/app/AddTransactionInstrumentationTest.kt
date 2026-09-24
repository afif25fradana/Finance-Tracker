package com.financetracker.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class AddTransactionInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun addTransaction_persistsAndNavigatesBackAndDisplaysInHistory() {
    // 1. Confirm initial screen is Dashboard
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsSelected()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("dashboard", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 2. Navigate to Add tab
    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New Transaction").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("add", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 3. Enter amount via quick-add preset (+Rp50.000)
    composeTestRule.onNodeWithText("+Rp50.000").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("50.000").assertIsDisplayed()

    // 4. Select category chip (Dining & Cafes)
    composeTestRule.onNodeWithText("Dining & Cafes").performClick()
    composeTestRule.waitForIdle()

    // 5. Enter note into the Note field
    composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput("Instrumented test coffee")
    composeTestRule.waitForIdle()
    androidx.test.espresso.Espresso.closeSoftKeyboard()
    composeTestRule.onNodeWithText("Instrumented test coffee").assertIsDisplayed()

    // 6. Scroll to and tap Save
    composeTestRule.onNodeWithText("Save").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // 7. Assert navigation returned to the previous screen (Dashboard)
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsSelected()
    composeTestRule.onNodeWithText("Finance Tracker").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("dashboard", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 8. Navigate to History screen
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").assertIsSelected()

    // 9. Assert real UI elements rendered from Room for the new transaction
    composeTestRule.onNodeWithText("Instrumented test coffee").assertIsDisplayed()
    // -Rp50.000 appears twice: once in the month header total, and once in the transaction row
    composeTestRule.onAllNodesWithText("-Rp50.000").assertCountEquals(2)
  }
}

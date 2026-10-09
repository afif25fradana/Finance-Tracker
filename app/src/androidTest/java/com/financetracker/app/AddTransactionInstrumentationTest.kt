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
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsSelected()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("dashboard", it.navController?.currentBackStackEntry?.destination?.route)
    }

    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New Transaction").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("add", it.navController?.currentBackStackEntry?.destination?.route)
    }

    composeTestRule.onNodeWithText("+Rp50.000").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("50.000").assertIsDisplayed()

    composeTestRule.onNodeWithText("Dining & Cafes").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onAllNodes(hasSetTextAction())[1].performTextInput("Instrumented test coffee")
    composeTestRule.waitForIdle()
    androidx.test.espresso.Espresso.closeSoftKeyboard()
    composeTestRule.onNodeWithText("Instrumented test coffee").assertIsDisplayed()

    composeTestRule.onNodeWithText("Save").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsSelected()
    composeTestRule.onNodeWithText("Finance Tracker").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("dashboard", it.navController?.currentBackStackEntry?.destination?.route)
    }

    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").assertIsSelected()

    composeTestRule.onNodeWithText("Instrumented test coffee").assertIsDisplayed()
    // -Rp50.000 appears twice: once in the month header total, and once in the transaction row
    composeTestRule.onAllNodesWithText("-Rp50.000").assertCountEquals(2)
  }
}

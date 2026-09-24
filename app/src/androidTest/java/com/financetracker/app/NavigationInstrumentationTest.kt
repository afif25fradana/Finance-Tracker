package com.financetracker.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun bottomNavigation_switchesAllTabsAndUpdatesCurrentRoute() {
    // 1. Initial launch state is Dashboard
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsSelected()
    composeTestRule.onNodeWithText("Finance Tracker").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("dashboard", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 2. Switch to History
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").assertIsSelected()
    composeTestRule.onNodeWithText("Search transactions...").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("history", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 3. Switch to Categories
    composeTestRule.onNodeWithTag("nav_categories").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_categories").assertIsSelected()
    composeTestRule.onNodeWithText("Add Category").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("categories", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 4. Switch to Add
    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_add").assertIsSelected()
    composeTestRule.onNodeWithText("New Transaction").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("add", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 5. Return to Dashboard
    composeTestRule.onNodeWithTag("nav_dashboard").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_dashboard").assertIsSelected()
    composeTestRule.onNodeWithText("Finance Tracker").assertIsDisplayed()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("dashboard", it.navController?.currentBackStackEntry?.destination?.route)
    }
  }

  @Test
  fun subRoutes_navigateFromDashboardAndBackPressRestoresDashboard() {
    // 1. Navigate to Reminders sub-route
    composeTestRule.onNodeWithText("Reminders").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertDoesNotExist()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("recurring", it.navController?.currentBackStackEntry?.destination?.route)
    }
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Finance Tracker").assertIsDisplayed()

    // 2. Navigate to Export sub-route
    composeTestRule.onNodeWithText("Export").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertDoesNotExist()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("export", it.navController?.currentBackStackEntry?.destination?.route)
    }
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()

    // 3. Navigate to Backup & Restore sub-route
    composeTestRule.onNodeWithText("Backup").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertDoesNotExist()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("backup_restore", it.navController?.currentBackStackEntry?.destination?.route)
    }
    composeTestRule.onNodeWithContentDescription("Back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
  }

  @Test
  fun addTab_leavingMidEntryDiscardsDraftForm() {
    // Navigate to Add
    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()

    // Enter draft amount
    composeTestRule.onNodeWithText("+Rp50.000").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("50.000").assertIsDisplayed()

    // Navigate away to History tab
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Search transactions...").assertIsDisplayed()

    // Navigate back to Add tab
    composeTestRule.onNodeWithTag("nav_add").performClick()
    composeTestRule.waitForIdle()

    // Draft amount must be discarded per design (reset to placeholder 0)
    composeTestRule.onAllNodesWithText("50.000").assertCountEquals(0)
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
  }
}

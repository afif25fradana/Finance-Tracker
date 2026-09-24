package com.financetracker.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun bottomNavigationBar_isDisplayedOnLaunch() {
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()
  }
}

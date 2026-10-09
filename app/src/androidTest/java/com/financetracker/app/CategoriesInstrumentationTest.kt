package com.financetracker.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriesInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private val expectedDefaultCategories = listOf(
    "Rent & Housing",
    "Groceries",
    "Dining & Cafes",
    "Transport & Fuel",
    "Entertainment",
    "Bills & Utilities",
    "Tech & Subs",
    "Health & Wellness",
    "Salary & Income"
  )

  @Test
  fun categoriesScreen_displaysDefaultCategoriesAndPersistsNewCategory() {
    composeTestRule.onNodeWithTag("nav_categories").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_categories").assertIsSelected()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("categories", it.navController?.currentBackStackEntry?.destination?.route)
    }

    expectedDefaultCategories.forEach { name ->
      composeTestRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
    }

    composeTestRule.onNodeWithText("Add Category").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New Category").assertIsDisplayed()

    composeTestRule.onNode(hasSetTextAction()).performTextInput("Freelance Consulting")
    composeTestRule.waitForIdle()

    composeTestRule.onNode(hasText("Income") and hasClickAction()).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Save").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Freelance Consulting").performScrollTo().assertIsDisplayed()
  }
}

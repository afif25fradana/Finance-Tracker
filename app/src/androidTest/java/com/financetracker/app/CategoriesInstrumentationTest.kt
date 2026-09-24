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
    // 1. Navigate to Categories tab
    composeTestRule.onNodeWithTag("nav_categories").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_categories").assertIsSelected()
    composeTestRule.activityRule.scenario.onActivity {
      assertEquals("categories", it.navController?.currentBackStackEntry?.destination?.route)
    }

    // 2. Confirm initial 9 default categories are rendered in the list
    expectedDefaultCategories.forEach { name ->
      composeTestRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
    }

    // 3. Scroll back to top and tap "Add Category"
    composeTestRule.onNodeWithText("Add Category").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New Category").assertIsDisplayed()

    // 4. Fill category name inside the dialog
    composeTestRule.onNode(hasSetTextAction()).performTextInput("Freelance Consulting")
    composeTestRule.waitForIdle()

    // 5. Select Income type inside the dialog (disambiguating from non-clickable section header)
    composeTestRule.onNode(hasText("Income") and hasClickAction()).performClick()
    composeTestRule.waitForIdle()

    // 6. Tap Save in the dialog
    composeTestRule.onNodeWithText("Save").performClick()
    composeTestRule.waitForIdle()

    // 7. Assert the newly added category appears in the Room-backed list
    composeTestRule.onNodeWithText("Freelance Consulting").performScrollTo().assertIsDisplayed()
  }
}

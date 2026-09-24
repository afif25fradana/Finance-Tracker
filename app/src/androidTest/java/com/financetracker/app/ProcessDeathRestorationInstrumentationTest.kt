package com.financetracker.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financetracker.app.ui.entry.AddEditTransactionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProcessDeathRestorationInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun addEditTransaction_stateMutatesSavedStateHandle() {
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

    // 5. Retrieve AddEditTransactionViewModel from NavBackStackEntry via MainActivity.navController
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val navController = activity.navController
      assertNotNull("NavController should not be null", navController)

      val backStackEntry = navController?.currentBackStackEntry
      assertNotNull("Current back stack entry should not be null", backStackEntry)
      assertEquals("add", backStackEntry?.destination?.route)

      val viewModel = ViewModelProvider(backStackEntry!!)[AddEditTransactionViewModel::class.java]
      val savedStateHandle = viewModel.savedStateHandle
      assertNotNull("SavedStateHandle should not be null", savedStateHandle)

      // Assert against verbatim key constants from AddEditTransactionViewModel
      assertEquals("INCOME", savedStateHandle?.get<String>(AddEditTransactionViewModel.KEY_TYPE))
      // Raw digits stored by onQuickAdd, not display-formatted string
      assertEquals("50000", savedStateHandle?.get<String>(AddEditTransactionViewModel.KEY_AMOUNT))
      assertEquals("SavedState death roundtrip", savedStateHandle?.get<String>(AddEditTransactionViewModel.KEY_NOTE))
      assertNotNull("Category ID should be saved in handle", savedStateHandle?.get<Long>(AddEditTransactionViewModel.KEY_CATEGORY_ID))
      assertEquals(true, savedStateHandle?.get<Boolean>(AddEditTransactionViewModel.KEY_RESTORED))
    }
  }
}

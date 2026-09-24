package com.financetracker.app

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.financetracker.app.backup.BackupCategory
import com.financetracker.app.backup.BackupCodec
import com.financetracker.app.backup.BackupTransaction
import com.financetracker.app.data.AppDatabase
import com.financetracker.app.data.entity.TransactionType
import com.financetracker.app.ui.backup.BackupRestoreViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupRestoreConfirmationInstrumentationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun restoreConfirmationFlow_displaysWarningDialogAndSafelyCancelsWithoutDataMutation() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val db = AppDatabase.getInstance(context)

    // 1. Record pre-test database baseline row counts
    val initialCategoriesCount = runBlocking { db.backupDao().getAllCategoriesOnce().size }
    val initialTransactionsCount = runBlocking { db.backupDao().getAllTransactionsOnce().size }

    // 2. Prepare a valid test backup JSON file in cacheDir
    val backupJson = BackupCodec.encode(
      appVersion = "1.0",
      currency = "IDR",
      createdAt = "2026-09-24T00:00:00Z",
      categories = listOf(
        BackupCategory(
          id = 999,
          name = "Replacement Test Category",
          type = "EXPENSE",
          color = 0xFF1B5543,
          icon = "category",
          isDefault = false
        )
      ),
      transactions = listOf(
        BackupTransaction(
          id = 999,
          amount = 99000,
          type = "EXPENSE",
          categoryId = 999,
          date = 20720,
          note = "Replacement transaction"
        )
      ),
      recurringItems = emptyList()
    )
    val testBackupFile = File(context.cacheDir, "test_restore_confirmation.json")
    testBackupFile.writeText(backupJson)

    // 3. Navigate from Dashboard to Backup & Restore screen
    composeTestRule.onNodeWithText("Backup").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()

    // 4. Trigger restore file picked via BackupRestoreViewModel
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val navController = activity.navController
      assertNotNull("NavController should not be null", navController)
      val backStackEntry = navController?.currentBackStackEntry
      assertNotNull("Current back stack entry should not be null", backStackEntry)
      assertEquals("backup_restore", backStackEntry?.destination?.route)

      val viewModel = ViewModelProvider(backStackEntry!!)[BackupRestoreViewModel::class.java]
      viewModel.onRestoreFilePicked(Uri.fromFile(testBackupFile))
    }
    composeTestRule.waitForIdle()

    // 5. Verify the destructive-replacement warning dialog appears with dynamically parsed counts
    composeTestRule.onNodeWithText("Restore backup?").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "1 categories, 1 transactions, 0 recurring items",
      substring = true
    ).assertIsDisplayed()
    composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

    // 6. Safely cancel the restore
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // 7. Verify the dialog is dismissed
    composeTestRule.onNodeWithText("Restore backup?").assertDoesNotExist()

    // 8. Verify database is completely untouched (counts identical to baseline)
    val postCancelCategoriesCount = runBlocking { db.backupDao().getAllCategoriesOnce().size }
    val postCancelTransactionsCount = runBlocking { db.backupDao().getAllTransactionsOnce().size }
    assertEquals("Category count must be unchanged after cancel", initialCategoriesCount, postCancelCategoriesCount)
    assertEquals("Transaction count must be unchanged after cancel", initialTransactionsCount, postCancelTransactionsCount)

    // Clean up temporary file
    testBackupFile.delete()
  }
}

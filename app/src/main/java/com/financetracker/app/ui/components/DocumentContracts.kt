package com.financetracker.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class CreateDocumentWithName : ActivityResultContract<CreateDocumentWithName.Request, Uri?>() {
  data class Request(val fileName: String, val mimeType: String)

  override fun createIntent(context: Context, input: Request): Intent =
    Intent(Intent.ACTION_CREATE_DOCUMENT)
      .setType(input.mimeType)
      .putExtra(Intent.EXTRA_TITLE, input.fileName)

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
    if (resultCode == Activity.RESULT_OK) intent?.data else null
}

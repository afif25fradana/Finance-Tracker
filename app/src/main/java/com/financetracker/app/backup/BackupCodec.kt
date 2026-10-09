package com.financetracker.app.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private val backupJson = Json {
  prettyPrint = true
  ignoreUnknownKeys = false
  isLenient = false
  coerceInputValues = false
}

private val PATH_PATTERN = Regex("""at path: (\S+)""")

object BackupCodec {

  fun encode(
    appVersion: String,
    currency: String,
    createdAt: String,
    categories: List<BackupCategory>,
    transactions: List<BackupTransaction>,
    recurringItems: List<BackupRecurringItem>
  ): String = backupJson.encodeToString(
    BackupFile(
      schemaVersion = BACKUP_SCHEMA_VERSION,
      appVersion = appVersion,
      currency = currency,
      createdAt = createdAt,
      categories = categories,
      transactions = transactions,
      recurringItems = recurringItems
    )
  )

  fun decode(text: String): BackupResult = decode(text.byteInputStream(Charsets.UTF_8))

  @OptIn(ExperimentalSerializationApi::class)
  fun decode(stream: InputStream): BackupResult {
    val root = try {
      backupJson.decodeFromStream<JsonElement>(stream)
    } catch (e: SerializationException) {
      return e.toBackupInvalid()
    }
    if (root !is JsonObject) {
      return BackupResult.Invalid("file", "Backup root must be a JSON object.")
    }
    checkIntegerTypes(root)?.let { return it }
    return try {
      BackupResult.Valid(backupJson.decodeFromJsonElement(root))
    } catch (e: SerializationException) {
      e.toBackupInvalid()
    }
  }
}

private fun checkIntegerTypes(root: JsonObject): BackupResult.Invalid? {
  fun check(obj: JsonObject, key: String, path: String): BackupResult.Invalid? {
    val element = obj[key] ?: return null
    val isInteger = element is JsonPrimitive && element !is JsonNull &&
      !element.isString && element.content.toLongOrNull() != null
    return if (isInteger) null else BackupResult.Invalid(path, "Expected an integer, got: $element")
  }

  fun checkAll(arrayName: String, fields: List<String>): BackupResult.Invalid? {
    (root[arrayName] as? JsonArray)?.forEachIndexed { index, element ->
      val obj = element as? JsonObject ?: return@forEachIndexed
      fields.forEach { field ->
        check(obj, field, "$arrayName[$index].$field")?.let { return it }
      }
    }
    return null
  }

  check(root, "schemaVersion", "schemaVersion")?.let { return it }
  checkAll("categories", listOf("id", "color"))?.let { return it }
  checkAll("transactions", listOf("id", "amount", "categoryId", "date"))?.let { return it }
  checkAll("recurringItems", listOf("id", "categoryId", "amount", "nextDueDate"))?.let { return it }
  return null
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerializationException.toBackupInvalid(): BackupResult.Invalid {
  (this as? MissingFieldException)?.missingFields?.firstOrNull()?.let {
    return BackupResult.Invalid(it, "Required field \"$it\" is missing.")
  }
  val path = PATH_PATTERN.find(message.orEmpty())?.groupValues?.get(1)
  val reason = message?.trim().takeUnless { it.isNullOrBlank() }
    ?: "The file is not a valid Finance Tracker backup."
  return BackupResult.Invalid(path ?: "file", reason)
}

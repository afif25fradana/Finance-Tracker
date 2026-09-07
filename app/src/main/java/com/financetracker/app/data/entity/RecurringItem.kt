package com.financetracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "recurring_items",
  foreignKeys = [
    ForeignKey(
      entity = Category::class,
      parentColumns = ["id"],
      childColumns = ["categoryId"],
      onDelete = ForeignKey.RESTRICT
    )
  ],
  indices = [Index("categoryId")]
)
data class RecurringItem(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val categoryId: Long,
  val amount: Long,
  val frequency: String,
  val nextDueDate: Long
)

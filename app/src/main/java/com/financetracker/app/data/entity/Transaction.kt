package com.financetracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "transactions",
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
data class Transaction(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val amount: Long,
  val type: TransactionType,
  val categoryId: Long,
  val date: Long,
  val note: String
)

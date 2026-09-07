package com.financetracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val type: TransactionType,
  val color: Long,
  val icon: String,
  val isDefault: Boolean = false
)

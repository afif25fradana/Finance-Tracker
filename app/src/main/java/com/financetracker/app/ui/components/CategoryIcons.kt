package com.financetracker.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

private val DEFAULT_CATEGORY_ICON: ImageVector = Icons.Default.ShoppingCart

// Mirrors the icon assignments made in the Frontend-Scafholding prototype model.
fun iconKeyToVector(key: String): ImageVector = when (key) {
  "payments" -> Icons.Default.Payments
  "home" -> Icons.Default.Home
  "shopping_cart" -> Icons.Default.ShoppingCart
  "restaurant" -> Icons.Default.Restaurant
  "directions_car" -> Icons.Default.DirectionsCar
  "movie" -> Icons.Default.Movie
  "bolt" -> Icons.Default.Bolt
  "devices" -> Icons.Default.Devices
  "medical_services" -> Icons.Default.MedicalServices
  else -> DEFAULT_CATEGORY_ICON
}

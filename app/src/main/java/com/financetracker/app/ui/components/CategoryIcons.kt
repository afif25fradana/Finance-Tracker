package com.financetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_CATEGORY_ICON: ImageVector = Icons.Default.ShoppingCart

// Mirrors the icon assignments from the original design mockup.
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

// Category glyph inside a rounded container with a low-opacity category-color halo.
@Composable
fun CategoryIconTile(
  iconKey: String,
  colorArgb: Long,
  containerSize: Dp = 20.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(containerSize)
      .clip(RoundedCornerShape(containerSize * 0.3f))
      .background(Color(colorArgb).copy(alpha = 0.18f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = iconKeyToVector(iconKey),
      contentDescription = null,
      tint = Color(colorArgb),
      modifier = Modifier.size(containerSize * 0.62f)
    )
  }
}

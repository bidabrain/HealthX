package com.healthx.bp.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val Primary = Color(0xFF2F6BFF)
val PrimaryDark = Color(0xFF1E4FD8)
val PrimaryContainer = Color(0xFFE8F0FF)

// Surfaces
val ScreenBg = Color(0xFFF4F6F9)
val CardBg = Color(0xFFFFFFFF)
val ScreenBgDark = Color(0xFF121417)
val CardBgDark = Color(0xFF1D2024)

// Text
val TextPrimary = Color(0xFF1C2330)
val TextSecondary = Color(0xFF8A93A6)
val Divider = Color(0xFFEDEFF3)

// Metric colors
val BpSystolic = Color(0xFFEF4444) // 收缩压 red
val BpDiastolic = Color(0xFF3B82F6) // 舒张压 blue
val BpHeart = Color(0xFF22C55E) // 心率 green

// Status colors (green → yellow → orange → red gradient)
val BpLow = Color(0xFF60A5FA)        // 偏低
val BpNormal = Color(0xFF22C55E)     // 正常
val BpHighNormal = Color(0xFFEAB308) // 正常高值
val BpWarn = Color(0xFFF59E0B)       // 偏高（高血压1级）
val BpHigh = Color(0xFFEF4444)       // 高（高血压≥2级）

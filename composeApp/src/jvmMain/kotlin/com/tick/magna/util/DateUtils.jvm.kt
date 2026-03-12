package com.tick.magna.util

actual fun currentYear(): Int = java.time.Year.now().value

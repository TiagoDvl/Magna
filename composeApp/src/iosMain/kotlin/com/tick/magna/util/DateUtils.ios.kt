package com.tick.magna.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun currentYear(): Int =
    NSCalendar.currentCalendar.component(NSCalendarUnitYear, fromDate = NSDate()).toInt()

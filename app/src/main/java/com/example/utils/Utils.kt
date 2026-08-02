package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun Double?.formatCurrency(): String {
    if (this == null || this == 0.0) return "0 ريال"
    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("#,##0.##", symbols)
    return "${formatter.format(this)} ريال"
}

fun Double?.formatQty(): String {
    if (this == null || this == 0.0) return "0"
    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("#,##0.##", symbols)
    return formatter.format(this)
}

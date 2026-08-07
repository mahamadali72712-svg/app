package com.example.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// Unified Money and Precision arithmetic extensions using BigDecimal
object Money {
    val ZERO: BigDecimal = BigDecimal.ZERO

    fun fromDouble(value: Double?): BigDecimal {
        if (value == null || value.isNaN() || value.isInfinite()) return BigDecimal.ZERO
        return BigDecimal.valueOf(value)
    }

    fun fromString(value: String?): BigDecimal {
        if (value.isNullOrBlank()) return BigDecimal.ZERO
        return try {
            // Remove any commas, spaces, or currency symbols
            val cleanValue = value.replace(Regex("[^0-9.-]"), "")
            BigDecimal(cleanValue)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}

fun Double?.toBigDecimalOrZero(): BigDecimal {
    return Money.fromDouble(this)
}

fun String?.toBigDecimalOrZero(): BigDecimal {
    return Money.fromString(this)
}

fun BigDecimal.toPreciseDouble(): Double {
    return this.toDouble()
}

// Precision operators for Double
fun Double.preciseAdd(other: Double): Double {
    return this.toBigDecimalOrZero().add(other.toBigDecimalOrZero()).toDouble()
}

fun Double.preciseSubtract(other: Double): Double {
    return this.toBigDecimalOrZero().subtract(other.toBigDecimalOrZero()).toDouble()
}

fun Double.preciseMultiply(other: Double): Double {
    return this.toBigDecimalOrZero().multiply(other.toBigDecimalOrZero()).toDouble()
}

fun Double.preciseDivide(other: Double, scale: Int = 4): Double {
    val otherBigDecimal = other.toBigDecimalOrZero()
    if (otherBigDecimal.compareTo(BigDecimal.ZERO) == 0) return 0.0
    return this.toBigDecimalOrZero().divide(otherBigDecimal, scale, RoundingMode.HALF_UP).toDouble()
}

fun Double.preciseRound(scale: Int = 2): Double {
    return this.toBigDecimalOrZero().setScale(scale, RoundingMode.HALF_UP).toDouble()
}

// Custom precise sumOf for precise accounting calculations
fun <T> Iterable<T>.preciseSumOf(selector: (T) -> Double): Double {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum = sum.add(Money.fromDouble(selector(element)))
    }
    return sum.toDouble()
}

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

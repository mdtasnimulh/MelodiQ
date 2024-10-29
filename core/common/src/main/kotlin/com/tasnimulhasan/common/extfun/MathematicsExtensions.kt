package com.tasnimulhasan.common.extfun

import android.text.InputFilter
import android.text.Spanned
import java.math.RoundingMode
import java.text.DecimalFormat

fun String.convertToInt(): Int {
    return try {
        this.toInt()
    } catch (ex: Exception) {
        0
    }
}

fun Double.doubleToInt() = Math.round(this).toInt()

fun Double.roundOfTwoDecimalPlaces(): Double {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.CEILING
    return df.format(this).toDouble()
}

class MinMaxFilter(minValue: Int, maxValue: Int) : InputFilter {
    private var intMin: Int = 0
    private var intMax: Int = 0

    init {
        this.intMin = minValue
        this.intMax = maxValue
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dStart: Int,
        dEnd: Int
    ): CharSequence? {
        try {
            val input = Integer.parseInt(dest.toString() + source.toString())
            if (isInRange(intMin, intMax, input)) {
                return null
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}
package com.alieba.app

import java.util.Calendar
import kotlin.math.*

object PrayerTimesCalculator {
    data class Times(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String
    )

    fun calculate(latitude: Double, longitude: Double, date: Calendar = Calendar.getInstance()): Times {
        val day = date.get(Calendar.DAY_OF_YEAR)
        val tz = date.timeZone.getOffset(date.timeInMillis) / 3600000.0
        val gamma = 2.0 * Math.PI / 365.0 * (day - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val noon = 720.0 - 4.0 * longitude - eqTime + tz * 60.0
        val lat = Math.toRadians(latitude.coerceIn(-89.0, 89.0))

        fun angleTime(altitudeDeg: Double, beforeNoon: Boolean): Double {
            val alt = Math.toRadians(altitudeDeg)
            val x = ((sin(alt) - sin(lat) * sin(decl)) / (cos(lat) * cos(decl))).coerceIn(-1.0, 1.0)
            val delta = Math.toDegrees(acos(x)) * 4.0
            return if (beforeNoon) noon - delta else noon + delta
        }

        // Ja'fari convention: Fajr 16°, Maghrib 4°, Isha 14°.
        val fajr = angleTime(-16.0, true)
        val sunrise = angleTime(-0.833, true)
        val sunset = angleTime(-0.833, false)
        val maghrib = angleTime(-4.0, false)
        val isha = angleTime(-14.0, false)

        // Asr display uses the standard one-shadow astronomical point.
        val asrAltitude = -Math.toDegrees(atan(1.0 / (1.0 + tan(abs(lat - decl)))))
        val asr = angleTime(asrAltitude, false)

        return Times(format(fajr), format(sunrise), format(noon), format(asr), format(maghrib), format(isha))
    }

    private fun format(raw: Double): String {
        var m = raw.roundToInt()
        while (m < 0) m += 1440
        while (m >= 1440) m -= 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }
}

package com.psikrometri.app.psy

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

// Sabitler Daikin Psychrometrics 4.1.0 exe'sinden (ASHRAE Fundamentals Bl. 1, buhar cp = 1.84).
const val P_DENIZ = 101325.0
const val EPS = 0.6219779385
const val RDA = 287.0551882

fun pws(t: Double): Double {
    val T = t + 273.15
    return if (t >= 0) exp(-5800.2206 / T + 1.3914993 - 0.048640239 * T + 4.1764768e-5 * T * T
        - 1.4452093e-8 * T * T * T + 6.5459673 * ln(T))
    else exp(-5674.5359 / T + 6.3925247 - 9.677843e-3 * T + 6.2215701e-7 * T * T
        + 2.0747825e-9 * T * T * T - 9.484024e-13 * T * T * T * T + 4.1635019 * ln(T))
}

fun nemOrani(pw: Double, p: Double) = EPS * pw / (p - pw)
fun buharBasinci(W: Double, p: Double) = W * p / (EPS + W)
fun doymaW(t: Double, p: Double) = nemOrani(pws(t), p)
fun cpm(W: Double) = 1.006 + 1.84 * W
fun entalpi(t: Double, W: Double) = 1.006 * t + W * (2501 + 1.84 * t)
fun tEntalpidan(h: Double, W: Double) = (h - 2501 * W) / cpm(W)
fun ozgulHacim(t: Double, W: Double, p: Double) = RDA * (t + 273.15) * (1 + 1.607774067 * W) / p
fun yogunluk(t: Double, W: Double, p: Double) = (1 + W) / ozgulHacim(t, W, p)

/** Yaş termometre tw'deki havanın nem oranı (ASHRAE eş. 35/37, cp buhar 1.84). */
fun yasW(t: Double, tw: Double, p: Double): Double {
    val ws = doymaW(tw, p)
    return if (tw >= 0) ((2501 - 2.346 * tw) * ws - 1.006 * (t - tw)) / (2501 + 1.84 * t - 4.186 * tw)
    else ((2834.4 - 0.26 * tw) * ws - 1.006 * (t - tw)) / (2834.4 + 1.84 * t - 2.1 * tw)
}

fun yasTermometre(t: Double, W: Double, p: Double): Double =
    kokBul(t, -100.0, 0.5) { tw -> yasW(t, tw, p) - W } ?: Double.NaN

/** ASHRAE eş. 39/40 (Peppers). */
fun ciyNoktasi(W: Double, p: Double): Double {
    if (W <= 0) return Double.NaN
    val pw = buharBasinci(W, p) / 1000
    val a = ln(pw)
    val td = 6.54 + 14.526 * a + 0.7389 * a * a + 0.09486 * a * a * a + 0.4569 * pw.pow(0.1984)
    return if (td >= 0) td else 6.09 + 12.608 * a + 0.4959 * a * a
}

// ponytail: standart atmosfer; Daikin'in rakım formülü Task 3 referansıyla doğrulanır.
fun basincRakimdan(z: Double) = P_DENIZ * (1 - 2.25577e-5 * z).pow(5.2559)
fun rakimBasinctan(p: Double) = (1 - (p / P_DENIZ).pow(1 / 5.2559)) / 2.25577e-5

fun kokBul(a: Double, b: Double, adim: Double, f: (Double) -> Double): Double? {
    val yon = if (b >= a) 1.0 else -1.0
    var x = a
    var fx = f(x)
    while ((b - x) * yon > 0) {
        val y = if (abs(b - x) < adim) b else x + yon * adim
        val fy = f(y)
        if (fx == 0.0) return x
        if (!fx.isNaN() && !fy.isNaN() && fx * fy <= 0) {
            var lo = x; var hi = y; var flo = fx
            repeat(80) {
                val m = (lo + hi) / 2; val fm = f(m)
                if (flo * fm > 0) { lo = m; flo = fm } else hi = m
            }
            return (lo + hi) / 2
        }
        x = y; fx = fy
    }
    return null
}

data class Durum(val t: Double, val W: Double, val p: Double = P_DENIZ) {
    val pw get() = buharBasinci(W, p)
    val rh get() = pw / pws(t)
    val h get() = entalpi(t, W)
    val twb get() = yasTermometre(t, W, p)
    val td get() = ciyNoktasi(W, p)
    val v get() = ozgulHacim(t, W, p)
    val rho get() = yogunluk(t, W, p)
}

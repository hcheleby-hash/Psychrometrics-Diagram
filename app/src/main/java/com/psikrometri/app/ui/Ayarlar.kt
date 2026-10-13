package com.psikrometri.app.ui

import android.content.Context
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Buyukluk

enum class EgriTuru(val etiket: String) { KURU("Kuru termometre"), MUTLAK("Mutlak nem"), BAGIL("Bağıl nem"), ENTALPI("Entalpi"), YAS("Yaş termometre"), HACIM("Özgül hacim"), YOGUNLUK("Yoğunluk") }

data class Ayarlar(val birimler: BirimSecimi = BirimSecimi(), val tMin: Double = -40.0, val tMax: Double = 65.0, val wMax: Double = 0.055,
                   val egriler: Set<EgriTuru> = EgriTuru.entries.toSet())

fun ayarlariOku(ctx: Context): Ayarlar {
    val sp = ctx.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)
    return Ayarlar(
        BirimSecimi(Buyukluk.entries.associateWith { sp.getInt("birim_${it.name}", 0).coerceIn(0, it.secenekler.lastIndex) }),
        sp.getFloat("tMin", -40f).toDouble(), sp.getFloat("tMax", 65f).toDouble(), sp.getFloat("wMax", 0.055f).toDouble(),
        EgriTuru.entries.filter { sp.getBoolean("egri_${it.name}", true) }.toSet())
}

fun ayarlariYaz(ctx: Context, a: Ayarlar) {
    val e = ctx.getSharedPreferences("ayarlar", Context.MODE_PRIVATE).edit()
    Buyukluk.entries.forEach { e.putInt("birim_${it.name}", a.birimler.secim[it] ?: 0) }
    e.putFloat("tMin", a.tMin.toFloat()).putFloat("tMax", a.tMax.toFloat()).putFloat("wMax", a.wMax.toFloat())
    EgriTuru.entries.forEach { e.putBoolean("egri_${it.name}", it in a.egriler) }
    e.apply()
}

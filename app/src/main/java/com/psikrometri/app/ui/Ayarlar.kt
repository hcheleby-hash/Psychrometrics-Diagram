package com.psikrometri.app.ui

import android.content.Context
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.model.Dil
import com.psikrometri.app.model.c

enum class EgriTuru(private val tr: String) { KURU("Kuru termometre"), MUTLAK("Mutlak nem"), BAGIL("Bağıl nem"), ENTALPI("Entalpi"), YAS("Yaş termometre"), HACIM("Özgül hacim"), YOGUNLUK("Yoğunluk");
    val etiket get() = tr.c() }

data class Ayarlar(val birimler: BirimSecimi = BirimSecimi(), val tMin: Double = -40.0, val tMax: Double = 65.0, val wMax: Double = 0.055,
                   val egriler: Set<EgriTuru> = EgriTuru.entries.toSet(), val dil: Dil = Dil.EN,
                   val yatay: Boolean = false) {
    val gecerli get() = aralikGecerli(tMin, tMax, wMax)
}

/** Diyagram aralığı (SI): dışındaki değerler eğri döngülerini kilitler ya da Eksen'de sıfıra böler. */
fun aralikGecerli(tMin: Double, tMax: Double, wMax: Double) = tMin >= -60 - TOL && tMin < tMax && tMax <= 100 + TOL && wMax > 0 && wMax <= 0.1 + TOL
private const val TOL = 1e-6  // °F dönüşümü ve Float saklama yuvarlaması sınır değerleri reddetmesin

fun ayarlariOku(ctx: Context): Ayarlar {
    val sp = ctx.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)
    val a = Ayarlar(
        BirimSecimi(Buyukluk.entries.associateWith { sp.getInt("birim_${it.name}", 0).coerceIn(0, it.secenekler.lastIndex) }),
        sp.getFloat("tMin", -40f).toDouble(), sp.getFloat("tMax", 65f).toDouble(), sp.getFloat("wMax", 0.055f).toDouble(),
        EgriTuru.entries.filter { sp.getBoolean("egri_${it.name}", true) }.toSet(),
        Dil.entries.firstOrNull { it.name == sp.getString("dil", null) } ?: Dil.EN,
        sp.getBoolean("yatay", false))
    return if (a.gecerli) a else a.copy(tMin = Ayarlar().tMin, tMax = Ayarlar().tMax, wMax = Ayarlar().wMax)
}

fun ayarlariYaz(ctx: Context, a: Ayarlar) {
    val e = ctx.getSharedPreferences("ayarlar", Context.MODE_PRIVATE).edit()
    Buyukluk.entries.forEach { e.putInt("birim_${it.name}", a.birimler.secim[it] ?: 0) }
    e.putFloat("tMin", a.tMin.toFloat()).putFloat("tMax", a.tMax.toFloat()).putFloat("wMax", a.wMax.toFloat())
    EgriTuru.entries.forEach { e.putBoolean("egri_${it.name}", it in a.egriler) }
    e.putString("dil", a.dil.name).putBoolean("yatay", a.yatay)
    e.apply()
}

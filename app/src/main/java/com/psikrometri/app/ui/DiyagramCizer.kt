package com.psikrometri.app.ui

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.model.c
import com.psikrometri.app.model.Hesap
import com.psikrometri.app.model.Proje
import com.psikrometri.app.psy.*
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

data class Eksen(val tMin: Double, val tMax: Double, val wMax: Double, val sol: Float, val ust: Float, val gen: Float, val yuk: Float) {
    fun x(t: Double) = sol + ((t - tMin) / (tMax - tMin) * gen).toFloat()
    fun y(w: Double) = ust + yuk - (w / wMax * yuk).toFloat()
    fun t(x: Float) = tMin + (x - sol) / gen * (tMax - tMin)
    fun w(y: Float) = (ust + yuk - y) / yuk * wMax
}

data class Egri(val tur: EgriTuru, val deger: Double, val noktalar: List<Pair<Double, Double>>)

/** W'yi 0..min(wMax, doyma) aralığında örnekleyip t(W) ile çizilen eğri. */
private fun wEgrisi(tur: EgriTuru, deger: Double, p: Double, tMin: Double, tMax: Double, wMax: Double, tBul: (Double) -> Double?): Egri {
    val nk = ArrayList<Pair<Double, Double>>()
    var w = 0.0
    while (w <= wMax + 1e-6) {
        val t = tBul(w)
        if (t != null && t in tMin..tMax && w <= doymaW(t, p)) nk += t to w
        w += wMax / 200
    }
    return Egri(tur, deger, nk)
}

fun egrileriUret(p: Double, tMin: Double, tMax: Double, wMax: Double, turler: Set<EgriTuru>): List<Egri> {
    if (!aralikGecerli(tMin, tMax, wMax)) return emptyList()
    val r = ArrayList<Egri>()
    val adimT = 5.0
    if (EgriTuru.KURU in turler) {
        var t = ceil(tMin / adimT) * adimT
        while (t <= tMax) { r += Egri(EgriTuru.KURU, t, listOf(t to 0.0, t to minOf(wMax, doymaW(t, p)))); t += adimT }
    }
    if (EgriTuru.MUTLAK in turler) {
        var w = 0.005
        while (w <= wMax + 1e-6) {
            val td = kokBul(tMin, tMax, 1.0) { doymaW(it, p) - w }
            if (td != null) r += Egri(EgriTuru.MUTLAK, w, listOf(td to w, tMax to w))
            w += 0.005
        }
    }
    if (EgriTuru.BAGIL in turler) for (rh in 10..100 step 10) {
        val nk = ArrayList<Pair<Double, Double>>(); var t = tMin
        while (t <= tMax) { val w = nemOrani(rh / 100.0 * pws(t), p); if (w <= wMax) nk += t to w; t += 0.5 }
        r += Egri(EgriTuru.BAGIL, rh.toDouble(), nk)
    }
    if (EgriTuru.ENTALPI in turler) {
        var h = floor(entalpi(tMin, 0.0) / 10) * 10
        while (h <= entalpi(tMax, minOf(wMax, doymaW(tMax, p)))) { r += wEgrisi(EgriTuru.ENTALPI, h, p, tMin, tMax, wMax) { tEntalpidan(h, it) }; h += 10 }
    }
    if (EgriTuru.YAS in turler) {
        var tw = ceil(tMin / adimT) * adimT
        while (tw <= tMax) { val twc = tw; r += wEgrisi(EgriTuru.YAS, tw, p, tMin, tMax, wMax) { w -> kokBul(twc, tMax + 1, 1.0) { yasW(it, twc, p) - w } }; tw += adimT }
    }
    if (EgriTuru.HACIM in turler) {
        var v = 0.70
        while (v <= 1.10) { val vc = v; r += wEgrisi(EgriTuru.HACIM, v, p, tMin, tMax, wMax) { w -> vc * p / (RDA * (1 + 1.607774067 * w)) - 273.15 }; v += 0.02 }
    }
    if (EgriTuru.YOGUNLUK in turler) {
        var ro = 0.95
        while (ro <= 1.50) { val rc = ro; r += wEgrisi(EgriTuru.YOGUNLUK, ro, p, tMin, tMax, wMax) { w -> (1 + w) * p / (rc * RDA * (1 + 1.607774067 * w)) - 273.15 }; ro += 0.05 }
    }
    // Doyma eğrisi her zaman
    val doyma = ArrayList<Pair<Double, Double>>(); var t = tMin
    while (t <= tMax) { val w = doymaW(t, p); if (w <= wMax) doyma += t to w; t += 0.25 }
    r += Egri(EgriTuru.BAGIL, 100.0, doyma)
    return r
}

val RENK_EGRI = mapOf(
    EgriTuru.KURU to 0xFFB0B0B0.toInt(), EgriTuru.MUTLAK to 0xFFB0B0B0.toInt(), EgriTuru.BAGIL to 0xFF29A3E0.toInt(),
    EgriTuru.ENTALPI to 0xFFE66A1F.toInt(), EgriTuru.YAS to 0xFF7FC8F0.toInt(), EgriTuru.HACIM to 0xFFF2C6A8.toInt(), EgriTuru.YOGUNLUK to 0xFF8BC34A.toInt())

fun EgriTuru.birimEtiketi(b: BirimSecimi) = when (this) {
    EgriTuru.KURU, EgriTuru.YAS -> b.birim(Buyukluk.SICAKLIK).etiket; EgriTuru.MUTLAK -> b.birim(Buyukluk.NEM).etiket
    EgriTuru.BAGIL -> "%"; EgriTuru.ENTALPI -> b.birim(Buyukluk.ENTALPI).etiket; EgriTuru.HACIM -> "m³/kg"; EgriTuru.YOGUNLUK -> "kg/m³"
}

fun cizDiyagram(c: Canvas, e: Eksen, p: Double, egriler: List<Egri>, hesap: Hesap, proje: Proje, secili: String?, onizleme: Durum?, birim: BirimSecimi) {
    val ince = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f }
    val yazi = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f }
    c.save(); c.clipRect(e.sol, e.ust, e.sol + e.gen, e.ust + e.yuk)
    for (g in egriler) {
        if (g.noktalar.size < 2) continue
        ince.color = RENK_EGRI.getValue(g.tur); ince.strokeWidth = if (g.tur == EgriTuru.BAGIL && g.deger == 100.0) 2.5f else 1.2f
        val path = Path(); g.noktalar.forEachIndexed { i, (t, w) -> if (i == 0) path.moveTo(e.x(t), e.y(w)) else path.lineTo(e.x(t), e.y(w)) }
        c.drawPath(path, ince)
    }
    c.restore()
    val tb = birim.birim(Buyukluk.SICAKLIK); val wb = birim.birim(Buyukluk.NEM); val hb = birim.birim(Buyukluk.ENTALPI)
    // eğri değerleri (Daikin gibi): bağıl nem üst/sağ uçta, entalpi ve yaş term. doyma ucunda, hacim/yoğunluk altta
    val etk = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f }
    val dolu = ArrayList<RectF>()
    // çakışmada öncelik büyük değerde: doyma (%100) etiketi hep kalır
    for (g in egriler.sortedWith(compareBy({ it.tur.ordinal }, { -it.deger }))) {
        if (g.noktalar.size < 2 || g.tur == EgriTuru.KURU || g.tur == EgriTuru.MUTLAK) continue
        val s = when (g.tur) {
            EgriTuru.ENTALPI -> hb.goster(g.deger).yaz(0); EgriTuru.YAS -> tb.goster(g.deger).yaz(0)
            EgriTuru.HACIM, EgriTuru.YOGUNLUK -> g.deger.yaz(2); else -> g.deger.yaz(0)
        }
        val alt = g.tur == EgriTuru.HACIM || g.tur == EgriTuru.YOGUNLUK
        val (t, w) = if (alt) g.noktalar.first() else g.noktalar.last()
        val gen = etk.measureText(s)
        val x = when (g.tur) { EgriTuru.ENTALPI -> e.x(t) - gen - 4; EgriTuru.YAS -> e.x(t) + 6; EgriTuru.BAGIL -> e.x(t) - gen / 2; else -> e.x(t) + 3 }
        val y = if (g.tur == EgriTuru.BAGIL || g.tur == EgriTuru.YAS) e.y(w) + 20 else e.y(w) - 4
        etk.color = if (g.tur == EgriTuru.YOGUNLUK || g.tur == EgriTuru.HACIM) 0xFF777777.toInt() else RENK_EGRI.getValue(g.tur)
        val lx = x.coerceIn(e.sol + 2, e.sol + e.gen - gen - 2); val ly = y.coerceIn(e.ust + 20, e.ust + e.yuk - 4)
        // sıkışık bölgede (sol alt, doyma ucu) üst üste binen etiket atlanır; önce gelen (bağıl nem, entalpi) kalır
        val kutu = RectF(lx - 2, ly - 18, lx + gen + 2, ly + 3)
        if (dolu.any { RectF.intersects(it, kutu) }) continue
        dolu += kutu
        c.drawText(s, lx, ly, etk)
    }
    // Eksen yazıları
    yazi.color = 0xFF333333.toInt()
    var t = ceil(e.tMin / 5) * 5
    while (t <= e.tMax) { c.drawText("%.0f".format(tb.goster(t)), e.x(t) - 12, e.ust + e.yuk + 24, yazi); t += 5 }
    var w = 0.0
    while (w <= e.wMax + 1e-6) { c.drawText(if (wb.carpan > 1) "%.0f".format(wb.goster(w)) else w.yaz(3), e.sol + e.gen + 6, e.y(w) + 8, yazi); w += 0.005 }
    yazi.isFakeBoldText = true
    c.drawText("${"Kuru termometre".c()} (${tb.etiket})", e.sol, e.ust + e.yuk + 52, yazi)
    val sag = "${"Mutlak nem".c()} (${wb.etiket})"
    c.drawText(sag, e.sol + e.gen + 80 - yazi.measureText(sag), e.ust - 18, yazi)
    yazi.isFakeBoldText = false
    // İşlem çizgileri
    val cizgi = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    proje.islemler.forEachIndexed { i, isl ->
        val r = hesap.sonuclar.getOrNull(i)?.getOrNull() ?: return@forEachIndexed
        cizgi.color = isl.renk
        for (cz in r.cizgiler) {
            cizgi.pathEffect = if (cz.kesikli) DashPathEffect(floatArrayOf(10f, 8f), 0f) else null
            val x1 = e.x(cz.a.t); val y1 = e.y(cz.a.W); val x2 = e.x(cz.b.t); val y2 = e.y(cz.b.W)
            c.drawLine(x1, y1, x2, y2, cizgi)
            if (!cz.kesikli) { // ok başı ortada
                val a = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()); val mx = (x1 + x2) / 2; val my = (y1 + y2) / 2
                cizgi.pathEffect = null
                for (s in listOf(2.6, -2.6)) c.drawLine(mx, my, (mx + 16 * cos(a + s)).toFloat(), (my + 16 * sin(a + s)).toFloat(), cizgi)
            }
        }
    }
    // Noktalar
    val dolgu = Paint(Paint.ANTI_ALIAS_FLAG)
    val renkler = proje.noktalar.associate { it.ad to it.renk } + proje.islemler.flatMap { i -> i.ciktilar.map { it to i.renk } }
    for ((ad, h) in hesap.hava) {
        dolgu.color = renkler[ad] ?: 0xFF1565C0.toInt(); yazi.color = dolgu.color
        val x = e.x(h.d.t); val y = e.y(h.d.W)
        c.drawCircle(x, y, if (ad == secili) 11f else 7f, dolgu)
        c.drawText(ad, x + 10, y - 10, yazi)
    }
    onizleme?.let { dolgu.color = 0x88C62828.toInt(); c.drawCircle(e.x(it.t), e.y(it.W), 9f, dolgu) }
}

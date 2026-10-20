package com.psikrometri.app.rapor

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.model.Hesap
import com.psikrometri.app.model.Proje
import com.psikrometri.app.model.c
import com.psikrometri.app.ui.*
import java.io.OutputStream

fun pdfYaz(cikti: OutputStream, proje: Proje, hesap: Hesap, egriler: List<Egri>, ayarlar: Ayarlar) {
    val pdf = PdfDocument()
    var sayfa = pdf.startPage(PdfDocument.PageInfo.Builder(842, 595, 1).create())
    sayfa.canvas.scale(0.5f, 0.5f)
    cizDiyagram(sayfa.canvas, Eksen(ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, 40f, 40f, 1560f, 1060f), proje.basinc, egriler, hesap, proje, null, null, ayarlar.birimler)
    pdf.finishPage(sayfa)
    val yazi = Paint().apply { textSize = 8f; isAntiAlias = true }
    val kalin = Paint(yazi).apply { isFakeBoldText = true }
    val cizgi = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 0.6f }
    val baslikZemin = Paint().apply { color = 0xFFDDE7F3.toInt() }
    var no = 2; var y = 0f
    fun yeniSayfa() { sayfa = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, no++).create()); y = 40f }
    fun satir(s: String, p: Paint = yazi) { if (y > 800) { pdf.finishPage(sayfa); yeniSayfa() }; sayfa.canvas.drawText(s, 40f, y, p); y += 15f }
    // hücre metni sütun genişliğine göre satırlara bölünür; satır yüksekliği en uzun hücreye göre
    fun sar(s: String, w: Float, p: Paint): List<String> {
        val r = mutableListOf<String>(); var k = s
        while (k.isNotEmpty()) { var n = p.breakText(k, true, w, null).coerceAtLeast(1); val bos = k.lastIndexOf(' ', n - 1)
            if (n < k.length && bos > 0) n = bos; r += k.take(n).trimEnd(); k = k.drop(n).trimStart() }
        return r.ifEmpty { listOf("") }
    }
    fun tablo(basliklar: List<String>, oranlar: List<Float>, satirlar: List<List<String>>, sola: Set<Int> = setOf(0)) {
        val gen = oranlar.map { it / oranlar.sum() * 515f }; val pd = 3f; val lh = 10f
        fun ciz(hucre: List<String>, p: Paint, zemin: Paint?) {
            val sat = hucre.mapIndexed { i, s -> sar(s, gen[i] - 2 * pd, p) }; val h = sat.maxOf { it.size } * lh + 2 * pd
            if (y + h > 802) { pdf.finishPage(sayfa); yeniSayfa(); if (zemin == null) ciz(basliklar, kalin, baslikZemin) }
            var x = 40f
            sat.forEachIndexed { i, l ->
                zemin?.let { sayfa.canvas.drawRect(x, y, x + gen[i], y + h, it) }
                sayfa.canvas.drawRect(x, y, x + gen[i], y + h, cizgi)
                l.forEachIndexed { j, t -> val tx = if (i in sola || zemin != null) x + pd else x + gen[i] - pd - p.measureText(t)
                    sayfa.canvas.drawText(t, tx, y + pd + lh * (j + 1) - 2f, p) }
                x += gen[i]
            }
            y += h
        }
        ciz(basliklar, kalin, baslikZemin); satirlar.forEach { ciz(it, yazi, null) }
    }
    yeniSayfa()
    satir("${"Psikrometri raporu".c()} — ${"rakım".c()} ${proje.rakim.yaz(0)} m, ${"basınç".c()} ${(proje.basinc / 1000).yaz(3)} kPa", kalin)
    y += 5f
    val b = ayarlar.birimler; val tb = b.birim(Buyukluk.SICAKLIK); val nb = b.birim(Buyukluk.NEM); val hb = b.birim(Buyukluk.ENTALPI); val db = b.birim(Buyukluk.DEBI)
    tablo(listOf("Nokta".c(), "t ${tb.etiket}", "twb ${tb.etiket}", "td ${tb.etiket}", "RH %", "W ${nb.etiket}", "h ${hb.etiket}", "ρ kg/m³", "v m³/kg", "${"debi".c()} ${db.etiket}"),
        listOf(1.2f, 1f, 1f, 1f, 1f, 1.2f, 1.1f, 1f, 1f, 1.2f),
        hesap.hava.map { (ad, hv) -> val d = hv.d
            listOf(ad, tb.goster(d.t).yaz(1), tb.goster(d.twb).yaz(1), tb.goster(d.td).yaz(1), (d.rh * 100).yaz(1), nb.goster(d.Wr).yaz(b.nemOndalik()),
                hb.goster(d.h).yaz(1), d.rho.yaz(3), d.v.yaz(3), db.goster(hv.debi).yaz(1)) })
    if (proje.islemler.isNotEmpty()) {
        y += 20f; satir("İşlemler".c(), kalin); y += 3f
        tablo(listOf("No", "İşlem".c(), "Giriş".c(), "Çıkış".c(), "Sonuç".c()), listOf(0.5f, 2f, 1.3f, 1.3f, 4f),
            proje.islemler.mapIndexed { i, isl -> listOf("${i + 1}", isl.tur.etiket, isl.girdiler.joinToString(", "), isl.ciktilar.joinToString(", "),
                hesap.sonuclar[i].fold({ it.kapasite.metin(b) }, { "${"Hata".c()}: ${it.message}" })) }, sola = setOf(0, 1, 2, 3, 4))
    }
    pdf.finishPage(sayfa)
    pdf.writeTo(cikti); pdf.close()
}

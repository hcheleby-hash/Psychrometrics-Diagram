package com.psikrometri.app.rapor

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.psikrometri.app.model.Hesap
import com.psikrometri.app.model.Proje
import com.psikrometri.app.ui.*
import java.io.OutputStream

fun pdfYaz(cikti: OutputStream, proje: Proje, hesap: Hesap, egriler: List<Egri>, ayarlar: Ayarlar) {
    val pdf = PdfDocument()
    var sayfa = pdf.startPage(PdfDocument.PageInfo.Builder(842, 595, 1).create())
    sayfa.canvas.scale(0.5f, 0.5f)
    cizDiyagram(sayfa.canvas, Eksen(ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, 40f, 40f, 1560f, 1060f), proje.basinc, egriler, hesap, proje, null, null, ayarlar.birimler)
    pdf.finishPage(sayfa)
    val yazi = Paint().apply { textSize = 9f; typeface = Typeface.MONOSPACE }
    val kalin = Paint(yazi).apply { isFakeBoldText = true }
    var no = 2; var y = 0f
    fun yeniSayfa() { sayfa = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, no++).create()); y = 40f }
    fun satir(s: String, p: Paint = yazi) { if (y > 800) { pdf.finishPage(sayfa); yeniSayfa() }; sayfa.canvas.drawText(s, 40f, y, p); y += 15f }
    yeniSayfa()
    satir("Psikrometri raporu — rakım ${proje.rakim.yaz(0)} m, basınç ${(proje.basinc / 1000).yaz(3)} kPa", kalin)
    val sutunlar = listOf("Nokta" to 6, "t °C" to 6, "twb °C" to 7, "td °C" to 6, "RH %" to 6, "W kg/kg" to 9, "h kJ/kg" to 8, "ρ kg/m³" to 7, "v m³/kg" to 7, "debi l/s" to 8)
    fun hizala(v: List<String>) = v.zip(sutunlar).mapIndexed { i, (s, c) -> if (i == 0) s.padEnd(c.second) else s.padStart(c.second) }.joinToString(" ")
    satir(hizala(sutunlar.map { it.first }), kalin)
    for ((ad, hv) in hesap.hava) {
        val d = hv.d
        satir(hizala(listOf(ad, d.t.yaz(1), d.twb.yaz(1), d.td.yaz(1), (d.rh * 100).yaz(1), d.Wr.yaz(5), d.h.yaz(1), d.rho.yaz(3), d.v.yaz(3), hv.debi.yaz(1))))
    }
    fun sar(s: String) { var k = s; var ek = ""; while (k.isNotEmpty()) { val n = yazi.breakText(ek + k, true, 515f, null) - ek.length; satir(ek + k.take(n.coerceAtLeast(1))); k = k.drop(n.coerceAtLeast(1)).trimStart(); ek = "    " } }
    y += 10f; satir("İşlemler", kalin)
    proje.islemler.forEachIndexed { i, isl ->
        sar("${isl.tur.etiket}: ${(isl.girdiler + isl.ciktilar).joinToString(", ")}")
        hesap.sonuclar[i].fold({ sar("    " + it.kapasite.metin()) }, { sar("    Hata: ${it.message}") })
    }
    pdf.finishPage(sayfa)
    pdf.writeTo(cikti); pdf.close()
}

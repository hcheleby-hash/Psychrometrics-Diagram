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
    satir("Nokta   t °C twb °C  td °C  RH %   W kg/kg  h kJ/kg ρ kg/m³ v m³/kg debi l/s", kalin)
    for ((ad, hv) in hesap.hava) {
        val d = hv.d
        satir("%-6s %6s %7s %6s %6s %9s %8s %7s %7s %8s".format(ad, d.t.yaz(1), d.twb.yaz(1), d.td.yaz(1), (d.rh * 100).yaz(1), d.Wr.yaz(5), d.h.yaz(1), d.rho.yaz(3), d.v.yaz(3), hv.debi.yaz(1)))
    }
    y += 10f; satir("İşlemler", kalin)
    proje.islemler.forEachIndexed { i, isl ->
        satir("${isl.tur.etiket}: ${(isl.girdiler + isl.ciktilar).joinToString(", ")}")
        hesap.sonuclar[i].fold({ satir("    " + it.kapasite.metin()) }, { satir("    Hata: ${it.message}") })
    }
    pdf.finishPage(sayfa)
    pdf.writeTo(cikti); pdf.close()
}

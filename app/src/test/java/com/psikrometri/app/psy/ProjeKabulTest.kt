package com.psikrometri.app.psy

import com.psikrometri.app.model.Proje
import org.junit.Assert.assertTrue
import org.junit.Test

/** tools/ppp_psy.py çıktısı (test1.psy) Proje.jsonOku + hesapla üzerinden Daikin raporuyla (test1) eşleşmeli. */
class ProjeKabulTest {
    @Test fun test1PsyDaikinRaporuyla() {
        val sn = Referans.yukle().getJSONArray("senaryolar").let { s -> (0 until s.length()).map { s.getJSONObject(it) }.first { it.getString("ad") == "test1" } }
        val proje = Proje.jsonOku(ProjeKabulTest::class.java.getResource("/test1.psy")!!.readText())
        val h = proje.hesapla()
        assertTrue(h.noktaHatalari.toString(), h.noktaHatalari.isEmpty())
        h.sonuclar.forEachIndexed { i, r -> assertTrue("işlem $i: ${r.exceptionOrNull()}", r.isSuccess) }
        val bek = sn.getJSONObject("beklenen_noktalar")
        for (ad in bek.keys()) Referans.noktaKarsilastir("test1.$ad", bek.getJSONObject(ad), (h.hava[ad] ?: error("$ad yok")).d)
        val bi = sn.getJSONObject("beklenen_islemler")
        for (ad in bi.keys()) {
            val i = proje.islemler.indexOfFirst { (it.ciktilar.firstOrNull() ?: "${it.tur.daikinKod}:" + it.girdiler.joinToString(":")) == ad }
            ReferansTest().kapasiteKarsilastir("test1.$ad", bi.getJSONObject(ad), h.sonuclar[i].getOrThrow().kapasite)
        }
        println("KABUL nokta=${bek.length()} islem=${bi.length()}")
    }
}

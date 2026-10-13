package com.psikrometri.app.model

import com.psikrometri.app.psy.IslemTuru
import com.psikrometri.app.psy.cpm
import com.psikrometri.app.psy.Ozellik
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjeTest {
    private val proje = Proje(
        noktalar = listOf(Nokta("P1", Ozellik.KURU, 27.0, Ozellik.MUTLAK, 0.010, 150.0), Nokta("P2", Ozellik.KURU, 15.0, Ozellik.MUTLAK, 0.008, 150.0)),
        islemler = listOf(
            Islem(IslemTuru.KARISIM, listOf("P1", "P2"), listOf("PP1"), emptyList()),
            Islem(IslemTuru.ISITMA_KAPASITE, listOf("PP1"), listOf("PP2"), listOf(5.0)),   // önceki işlemin çıktısını kullanır
            Islem(IslemTuru.SOGUTMA_KAPASITE, listOf("P1"), listOf("PP3", "PP4"), listOf(50.0, 75.0, 1.0)),  // hata verir
        ))

    @Test fun zincirliHesap() {
        val h = proje.hesapla()
        assertEquals(20.9, h.hava.getValue("PP1").d.t, 0.05)
        assertEquals(300.0, h.hava.getValue("PP1").debi, 1e-9)
        val pp1 = h.hava.getValue("PP1").d
        assertEquals(pp1.t + 5 / (0.36 * cpm(pp1.W)), h.hava.getValue("PP2").d.t, 1e-6)  // m = 1,2·300/1000
        assertTrue(h.sonuclar[2].isFailure)
    }

    @Test fun noktaDegisinceBagliIslemDegisir() {
        val p2 = proje.copy(noktalar = proje.noktalar.map { if (it.ad == "P2") it.copy(v1 = 12.0) else it })
        assertTrue(p2.hesapla().hava.getValue("PP1").d.t < proje.hesapla().hava.getValue("PP1").d.t)
    }

    @Test fun jsonGidisDonus() {
        assertEquals(proje, Proje.jsonOku(proje.copy(rakim = 1500.0).json()).copy(rakim = 0.0))
    }

    @Test fun ppAdi() { assertEquals("PP5", proje.yeniPPAdi()) }
}

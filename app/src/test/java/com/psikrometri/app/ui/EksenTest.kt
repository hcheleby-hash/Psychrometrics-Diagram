package com.psikrometri.app.ui

import com.psikrometri.app.model.CEVIRI
import com.psikrometri.app.model.Dil
import com.psikrometri.app.model.c
import com.psikrometri.app.model.dil
import com.psikrometri.app.psy.Durum
import com.psikrometri.app.psy.IslemTuru
import com.psikrometri.app.psy.P_DENIZ
import com.psikrometri.app.psy.entalpi
import com.psikrometri.app.psy.yasTermometre
import com.psikrometri.app.psy.doymaW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EksenTest {
    private val e = Eksen(-40.0, 65.0, 0.055, 10f, 20f, 1050f, 550f)

    @Test fun gidisDonus() {
        assertEquals(10f, e.x(-40.0), 1e-4f); assertEquals(1060f, e.x(65.0), 1e-4f)
        assertEquals(570f, e.y(0.0), 1e-4f); assertEquals(20f, e.y(0.055), 1e-4f)
        assertEquals(27.0, e.t(e.x(27.0)), 1e-4); assertEquals(0.01, e.w(e.y(0.01)), 1e-7)
    }

    @Test fun ayarAraligi() {
        assertTrue(aralikGecerli(-40.0, 65.0, 0.055)); assertTrue(aralikGecerli(-60.0, 100.0, 0.1))
        assertTrue(aralikGecerli((-76.0 - 32) / 1.8, (212.0 - 32) / 1.8, 0.1f.toDouble()))  // °F ve Float sınırları
        assertFalse(aralikGecerli(-40.0, 65.0, 0.0)); assertFalse(aralikGecerli(20.0, 20.0, 0.05)); assertFalse(aralikGecerli(30.0, 20.0, 0.05))
        assertFalse(aralikGecerli(-61.0, 65.0, 0.05)); assertFalse(aralikGecerli(-40.0, 101.0, 0.05)); assertFalse(aralikGecerli(-40.0, 65.0, 0.11))
        assertFalse(aralikGecerli(Double.NaN, 65.0, 0.05)); assertFalse(Ayarlar(wMax = -1.0).gecerli)
    }

    @Test(timeout = 2000) fun gecersizAraliktaEgriYok() {
        for ((a, b, w) in listOf(Triple(-40.0, 65.0, 0.0), Triple(20.0, 20.0, 0.05), Triple(-1e9, 1e9, 0.05), Triple(-40.0, 65.0, 1e9)))
            assertTrue(egrileriUret(P_DENIZ, a, b, w, EgriTuru.entries.toSet()).isEmpty())
    }

    @Test fun egrilerDoymaUstuneTasmaz() {
        for (g in egrileriUret(P_DENIZ, -40.0, 65.0, 0.055, EgriTuru.entries.toSet()))
            for ((t, w) in g.noktalar) {
                assertTrue("${g.tur} ${g.deger} t=$t", t >= -40.0 - 1e-6 && t <= 65.0 + 1e-6)
                assertTrue("${g.tur} ${g.deger} w=$w", w >= -1e-9 && w <= minOf(0.055, doymaW(t, P_DENIZ)) + 1e-6)
            }
    }

    @Test fun egriNoktalariDegerineUyar() {
        val p = P_DENIZ
        for (g in egrileriUret(p, -40.0, 65.0, 0.055, EgriTuru.entries.toSet())) for ((t, w) in g.noktalar) {
            // yasW Daikin gibi t = 0 °C'de buz/su dalı değiştirir: tw < 0 eğrileri orada küçük bir sıçramayla birleşir
            if (g.tur == EgriTuru.YAS && kotlin.math.abs(t) < 1e-6) continue
            val d = Durum(t, w, p)
            val (gercek, tol) = when (g.tur) {
                EgriTuru.KURU -> t to 1e-9; EgriTuru.MUTLAK -> w to 1e-9; EgriTuru.BAGIL -> d.rh * 100 to 0.05
                EgriTuru.ENTALPI -> entalpi(t, w) to 0.05; EgriTuru.YAS -> yasTermometre(t, w, p) to 0.05
                EgriTuru.HACIM -> d.v to 1e-4; EgriTuru.YOGUNLUK -> d.rho to 1e-4
            }
            assertEquals("${g.tur} ${g.deger} t=$t w=$w", g.deger, gercek, tol)
        }
    }

    @Test fun cevirilerTam() {
        for ((tr, l) in CEVIRI) { assertEquals(tr, Dil.entries.size - 1, l.size); assertTrue(tr, l.all { it.isNotBlank() }) }
        try {
            dil = Dil.DE; assertEquals("Kühlung", IslemTuru.SOGUTMA.etiket); assertEquals("Trockentemperatur", EgriTuru.KURU.etiket)
            assertEquals("bilinmeyen", "bilinmeyen".c())
            // UI'da kullanılan tüm enum etiketleri çevrilmiş olmalı (anahtar yazım hatası yakalanır)
            for (e in IslemTuru.entries) assertTrue(e.name, e.etiket != e.name && CEVIRI.values.any { e.etiket in it })
            for (e in EgriTuru.entries) assertTrue(e.name, CEVIRI.values.any { e.etiket in it })
        } finally { dil = Dil.TR }
    }
}

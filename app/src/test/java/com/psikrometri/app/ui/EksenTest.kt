package com.psikrometri.app.ui

import com.psikrometri.app.psy.P_DENIZ
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
}

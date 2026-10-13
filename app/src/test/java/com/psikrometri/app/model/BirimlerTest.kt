package com.psikrometri.app.model

import com.psikrometri.app.psy.Ozellik
import org.junit.Assert.assertEquals
import org.junit.Test

class BirimlerTest {
    @Test fun donusumler() {
        val f = Buyukluk.SICAKLIK.secenekler.first { it.etiket == "°F" }
        assertEquals(212.0, f.goster(100.0), 1e-9); assertEquals(100.0, f.siye(212.0), 1e-9)
        assertEquals(540.0, Buyukluk.DEBI.secenekler.first { it.etiket == "m³/h" }.goster(150.0), 1e-9)
        assertEquals(10.0, Buyukluk.NEM.secenekler.first { it.etiket == "g/kg" }.goster(0.010), 1e-12)
        assertEquals(101.325, Buyukluk.BASINC.secenekler.first { it.etiket == "kPa" }.goster(101325.0), 1e-9)
    }

    @Test fun ozellikBirimi() {
        val s = BirimSecimi(mapOf(Buyukluk.SICAKLIK to 1))
        assertEquals("°F", s.ozellikBirimi(Ozellik.YAS).etiket)
        assertEquals("%", s.ozellikBirimi(Ozellik.BAGIL).etiket)
    }
}

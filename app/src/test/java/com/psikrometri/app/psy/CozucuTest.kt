package com.psikrometri.app.psy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CozucuTest {
    @Test fun yirmiBirIkiliGidisDonus() {
        for ((t, W) in listOf(27.0 to 0.010, 5.0 to 0.003, -10.0 to 0.0013, 40.0 to 0.020)) {
            val d = Durum(t, W)
            for (i in 0 until 7) for (j in i + 1 until 7) {
                val o1 = Ozellik.entries[i]; val o2 = Ozellik.entries[j]
                val c = coz(o1, d.deger(o1), o2, d.deger(o2), P_DENIZ)
                assertEquals("$o1+$o2 t", t, c.t, 1e-4)
                assertEquals("$o1+$o2 W", W, c.W, 1e-7)
            }
        }
    }

    @Test fun ayniOzellikHata() {
        assertThrows(CozumHatasi::class.java) { coz(Ozellik.KURU, 20.0, Ozellik.KURU, 25.0, P_DENIZ) }
    }

    @Test fun doymaUstuHata() {
        assertThrows(CozumHatasi::class.java) { coz(Ozellik.KURU, 20.0, Ozellik.BAGIL, 120.0, P_DENIZ) }
        assertThrows(CozumHatasi::class.java) { coz(Ozellik.KURU, 20.0, Ozellik.MUTLAK, 0.05, P_DENIZ) }
    }
}

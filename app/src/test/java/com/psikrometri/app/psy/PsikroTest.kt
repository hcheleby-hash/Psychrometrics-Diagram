package com.psikrometri.app.psy

import org.junit.Assert.assertEquals
import org.junit.Test

class PsikroTest {
    private fun yakin(beklenen: Double, gercek: Double, ondalik: Int) =
        assertEquals(beklenen, gercek, 0.5 * Math.pow(10.0, -ondalik.toDouble()) + 1e-9)

    @Test fun doymaBasinciAshraeTablosu() {
        assertEquals(2339.0, pws(20.0), 2.0)
        assertEquals(259.9, pws(-10.0), 0.5)
    }

    @Test fun bagilNemDaikinPppOnbellegiyle15Hane() {  // tools/daikin/veri/test1.ppp
        assertEquals(44.9441962635556, Durum(27.0, 0.010).rh * 100, 1e-8)
        assertEquals(75.4471482056509, Durum(15.0, 0.008).rh * 100, 1e-8)
        assertEquals(42.3978002755472, Durum(35.0, 0.015).rh * 100, 1e-8)
    }

    @Test fun raporDegerleri() {  // rapor1.txt / rapor3.txt: (t, W) -> twb, td, h, rho, v
        val satirlar = listOf(
            doubleArrayOf(27.0, 0.010, 18.6, 14.1, 52.7, 1.169, 0.864),
            doubleArrayOf(15.0, 0.008, 12.5, 10.7, 35.3, 1.219, 0.827),
            doubleArrayOf(35.0, 0.015, 24.5, 20.3, 73.7, 1.135, 0.894),
            doubleArrayOf(5.0, 0.003, 1.8, -2.7, 12.6, 1.267, Double.NaN),
            doubleArrayOf(22.0, 0.006, 13.4, 6.5, 37.4, 1.192, Double.NaN),
        )
        for (r in satirlar) {
            val d = Durum(r[0], r[1])
            yakin(r[2], d.twb, 1); yakin(r[3], d.td, 1); yakin(r[4], d.h, 1); yakin(r[5], d.rho, 3)
            if (!r[6].isNaN()) yakin(r[6], d.v, 3)
        }
    }

    @Test fun doymusHavadaUcSicaklikEsit() {
        val w = doymaW(20.0, P_DENIZ)  // formüller yuvarlanmamış W ile (Durum.twb/td Daikin gibi Wr kullanır)
        assertEquals(20.0, yasTermometre(20.0, w, P_DENIZ), 1e-3); assertEquals(20.0, ciyNoktasi(w, P_DENIZ), 0.05)
    }

    @Test fun rakimBasincGidisDonus() {
        assertEquals(P_DENIZ, basincRakimdan(0.0), 1e-6)
        assertEquals(1500.0, rakimBasinctan(basincRakimdan(1500.0)), 1e-6)
    }
    @Test fun rakimDaikin() = assertEquals(84555.9, basincRakimdan(1500.0), 0.05)  // ref_rakim raporu

    @Test fun kokBulAzalanYonde() {
        assertEquals(2.0, kokBul(10.0, -10.0, 0.5) { it - 2.0 }!!, 1e-9)
    }
}

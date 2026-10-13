package com.psikrometri.app.psy

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

object Referans {
    fun yukle(): JSONObject = JSONObject(Referans::class.java.getResource("/daikin_referans.json")!!.readText())
    private val ondalik = mapOf("t" to 1, "twb" to 1, "td" to 1, "rh" to 1, "W" to 4, "h" to 1, "rho" to 3, "v" to 3, "p" to 1)
    fun noktaKarsilastir(etiket: String, beklenen: JSONObject, d: Durum) {
        for (k in beklenen.keys()) {
            val n = ondalik[k] ?: continue
            val g = when (k) { "t" -> d.t; "twb" -> d.twb; "td" -> d.td; "rh" -> d.rh * 100; "W" -> d.W
                "h" -> d.h; "rho" -> d.rho; "v" -> d.v; else -> d.p }
            assertEquals("$etiket.$k", beklenen.getDouble(k), g, 0.5 * Math.pow(10.0, -n.toDouble()) + 1e-9)
        }
    }
}

class ReferansTest {
    @Test fun durumSenaryosu() {
        val s = Referans.yukle().getJSONArray("senaryolar")
        for (i in 0 until s.length()) {
            val sn = s.getJSONObject(i)
            if (sn.getString("ad") != "ref_durum") continue
            val p = sn.getDouble("basinc")
            val bek = sn.getJSONObject("beklenen_noktalar")
            val nk = sn.getJSONArray("noktalar")
            for (j in 0 until nk.length()) {
                val n = nk.getJSONObject(j)
                val d = coz(Ozellik.entries[n.getInt("o1")], n.getDouble("v1"), Ozellik.entries[n.getInt("o2")], n.getDouble("v2"), p)
                Referans.noktaKarsilastir(n.getString("ad"), bek.getJSONObject(n.getString("ad")), d)
            }
        }
    }
}

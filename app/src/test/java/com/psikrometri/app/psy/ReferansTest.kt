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
            val g = when (k) { "t" -> d.t; "twb" -> d.twb; "td" -> d.td; "rh" -> d.rh * 100; "W" -> d.Wr
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
    private val parametreSirasi: Map<Int, (List<Double>) -> List<Double>> = mapOf(
        4 to { p -> listOf(p[0], p.getOrElse(1) { 1.0 }) },        // buhar, ara
        5 to { p -> listOf(p[0], p[1], p.getOrElse(2) { 1.0 }) },  // kW, SHF, ara
        9 to { p -> listOf(p[1], p.getOrElse(2) { 1.0 }) },        // (0), SHF, ara -> SHF, ara
    )

    /**
     * ppp sırasıyla tüm işlemleri koşturur. Çözülemeyen işlem: Daikin de çözememişse (çıktısı raporda yok) atlanır,
     * çözmüşse test hata mesajıyla düşer. Daikin'de hatalı işlemden hemen sonraki işlemin çıktıları varsayılan
     * nokta (25 °C, %50, 100 l/s) olur ve kapasitesi yazılmaz (test4: S2, S3, S6) — rapordaki bu durum taklit edilir.
     */
    fun calistir(sn: JSONObject): Pair<Map<String, Hava>, Map<String, IslemSonucu>> {
        val p = sn.getDouble("basinc"); val bek = sn.getJSONObject("beklenen_noktalar")
        val varsayilan = Hava(coz(Ozellik.KURU, 25.0, Ozellik.BAGIL, 50.0, p), 100.0)
        val hava = LinkedHashMap<String, Hava>(); val sonuc = HashMap<String, IslemSonucu>()
        var oncekiHata = false
        val nk = sn.getJSONArray("noktalar")
        for (j in 0 until nk.length()) { val n = nk.getJSONObject(j)
            hava[n.getString("ad")] = Hava(coz(Ozellik.entries[n.getInt("o1")], n.getDouble("v1"), Ozellik.entries[n.getInt("o2")], n.getDouble("v2"), p), n.getDouble("debi")) }
        val isl = sn.getJSONArray("islemler")
        for (j in 0 until isl.length()) {
            val o = isl.getJSONObject(j); val kod = o.getInt("kod")
            val tur = IslemTuru.entries.first { it.daikinKod == kod }
            val girdi = (0 until o.getJSONArray("girdi").length()).map { hava[o.getJSONArray("girdi").getString(it)] ?: return@map null }
            if (girdi.any { it == null }) continue
            val ham = (0 until o.getJSONArray("param").length()).map { o.getJSONArray("param").getDouble(it) }
            val param = parametreSirasi[kod]?.invoke(ham) ?: ham
            val cikti = (0 until o.getJSONArray("cikti").length()).map { o.getJSONArray("cikti").getString(it) }
            var r = try { islemHesapla(tur, girdi.map { it!! }, param) } catch (e: CozumHatasi) {
                if (cikti.any { bek.has(it) }) throw AssertionError("${sn.getString("ad")}: işlem $kod ${cikti} çözülemedi: ${e.message}")
                oncekiHata = true; continue
            }
            if (oncekiHata) r = IslemSonucu(cikti.map { varsayilan }, emptyList(), Kapasite())
            oncekiHata = false
            cikti.forEachIndexed { k, ad -> hava[ad] = r.ciktilar[k] }
            sonuc[cikti.firstOrNull() ?: "$kod:" + girdi.indices.joinToString(":") { o.getJSONArray("girdi").getString(it) }] = r
        }
        return hava to sonuc
    }

    private fun kapasiteKarsilastir(etiket: String, b: JSONObject, k: Kapasite) {
        val es = mapOf("Total" to k.toplam, "Sensible" to k.duyulur, "Latent" to k.gizli, "Heating" to k.isitici,
            "Dehumidification" to k.nemKgH, "Steam humidification" to k.nemKgH, "Water humidification" to k.nemKgH,
            "Discharge" to k.cikisT, "Bypass factor" to k.bypass, "SHF" to k.shf)
        for (key in b.keys()) {
            val g = es[key] ?: error("$etiket: $key hesaplanmadı")
            val tol = if (key == "Discharge" || key == "Bypass factor" || key == "SHF") 0.05 else 0.005
            assertEquals("$etiket.$key", b.getDouble(key), g, tol + 1e-9)
        }
    }

    private fun senaryoKontrol(sn: JSONObject) {
        val (hava, sonuc) = calistir(sn)
        val bek = sn.getJSONObject("beklenen_noktalar")
        for (ad in bek.keys()) {
            val h = hava[ad] ?: throw AssertionError("${sn.getString("ad")}.$ad: Daikin hesapladı, biz hesaplayamadık")
            Referans.noktaKarsilastir("${sn.getString("ad")}.$ad", bek.getJSONObject(ad), h.d)
        }
        val bi = sn.getJSONObject("beklenen_islemler")
        for (ad in bi.keys()) {
            val r = sonuc[ad] ?: throw AssertionError("${sn.getString("ad")}.$ad: işlem sonucu yok")
            kapasiteKarsilastir("${sn.getString("ad")}.$ad", bi.getJSONObject(ad), r.kapasite)
        }
    }

    private fun senaryolar(rakim: Boolean) = Referans.yukle().getJSONArray("senaryolar").let { s ->
        (0 until s.length()).map { s.getJSONObject(it) }.filter { (it.getString("ad") == "ref_rakim") == rakim && it.getString("ad") != "ref_durum" }
    }

    @Test fun islemSenaryolari() { senaryolar(false).forEach(::senaryoKontrol) }

    @Test fun rakimSenaryosu() { senaryolar(true).forEach(::senaryoKontrol) }
}

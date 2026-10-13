package com.psikrometri.app.model

import com.psikrometri.app.psy.*
import org.json.JSONArray
import org.json.JSONObject

const val MAVI = 0xFF1565C0.toInt()
val RENKLER = listOf(MAVI, 0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFEF6C00.toInt(), 0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFF4E342E.toInt(), 0xFF212121.toInt())

data class Nokta(val ad: String, val o1: Ozellik, val v1: Double, val o2: Ozellik, val v2: Double, val debi: Double, val renk: Int = MAVI)
data class Islem(val tur: IslemTuru, val girdiler: List<String>, val ciktilar: List<String>, val param: List<Double>, val renk: Int = MAVI)
data class Hesap(val hava: Map<String, Hava>, val sonuclar: List<Result<IslemSonucu>>, val noktaHatalari: Map<String, String>)

data class Proje(val rakim: Double = 0.0, val noktalar: List<Nokta> = emptyList(), val islemler: List<Islem> = emptyList()) {
    val basinc get() = basincRakimdan(rakim)

    fun hesapla(): Hesap {
        val hava = LinkedHashMap<String, Hava>(); val hatalar = HashMap<String, String>()
        for (n in noktalar) try { hava[n.ad] = Hava(coz(n.o1, n.v1, n.o2, n.v2, basinc), n.debi) } catch (e: CozumHatasi) { hatalar[n.ad] = e.message!! }
        val sonuclar = islemler.map { i ->
            runCatching {
                val girdi = i.girdiler.map { hava[it] ?: throw CozumHatasi("$it noktası yok") }
                val r = islemHesapla(i.tur, girdi, i.param)
                if (r.ciktilar.size < i.ciktilar.size) throw CozumHatasi("İşlem beklenen çıktıları üretmedi")
                i.ciktilar.zip(r.ciktilar).forEach { (ad, h) -> hava[ad] = h }
                r
            }
        }
        return Hesap(hava, sonuclar, hatalar)
    }

    fun yeniPPAdi(): String = "PP" + ((islemler.flatMap { it.ciktilar }.mapNotNull { it.removePrefix("PP").toIntOrNull() }.maxOrNull() ?: 0) + 1)

    fun json(): String = JSONObject().put("surum", 1).put("rakim", rakim)
        .put("noktalar", JSONArray(noktalar.map { JSONObject().put("ad", it.ad).put("o1", it.o1.name).put("v1", it.v1).put("o2", it.o2.name).put("v2", it.v2).put("debi", it.debi).put("renk", it.renk) }))
        .put("islemler", JSONArray(islemler.map { JSONObject().put("tur", it.tur.name).put("girdiler", JSONArray(it.girdiler)).put("ciktilar", JSONArray(it.ciktilar)).put("param", JSONArray(it.param)).put("renk", it.renk) }))
        .toString(1)

    companion object {
        fun jsonOku(s: String): Proje {
            val o = JSONObject(s)
            fun JSONArray.str() = (0 until length()).map { getString(it) }
            fun JSONArray.dbl() = (0 until length()).map { getDouble(it) }
            // sondaki eksik "ara çizgiler" seçeneği (ppp dönüştürücüsü yazmaz) varsayılan 1 ile tamamlanır
            fun tamamla(tur: IslemTuru, p: List<Double>) = tur.parametreler.drop(p.size).let { e -> if (e.all { it == Parametre.ARA }) p + e.map { 1.0 } else p }
            val n = o.getJSONArray("noktalar"); val i = o.getJSONArray("islemler")
            val p = Proje(o.getDouble("rakim"),
                (0 until n.length()).map { n.getJSONObject(it) }.map { Nokta(it.getString("ad"), Ozellik.valueOf(it.getString("o1")), it.getDouble("v1"), Ozellik.valueOf(it.getString("o2")), it.getDouble("v2"), it.getDouble("debi"), it.getInt("renk")) },
                (0 until i.length()).map { i.getJSONObject(it) }.map { Islem(IslemTuru.valueOf(it.getString("tur")), it.getJSONArray("girdiler").str(), it.getJSONArray("ciktilar").str(), tamamla(IslemTuru.valueOf(it.getString("tur")), it.getJSONArray("param").dbl()), it.getInt("renk")) })
            for (isl in p.islemler) require(isl.girdiler.size == isl.tur.girdiSayisi && isl.ciktilar.size == isl.tur.ciktiSayisi && isl.param.size == isl.tur.parametreler.size) { "${isl.tur} sayıları uyuşmuyor" }
            require(p.noktalar.all { it.debi > 0 }) { "debi > 0 olmalı" }
            val adlar = p.noktalar.map { it.ad } + p.islemler.flatMap { it.ciktilar }
            require(adlar.size == adlar.toSet().size) { "ad tekrarı" }
            return p
        }
    }
}

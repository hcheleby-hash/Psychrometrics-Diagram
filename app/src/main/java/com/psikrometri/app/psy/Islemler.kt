package com.psikrometri.app.psy

import com.psikrometri.app.model.c
import kotlin.math.abs

data class Hava(val d: Durum, val debi: Double)
data class Kapasite(val toplam: Double? = null, val duyulur: Double? = null, val gizli: Double? = null, val isitici: Double? = null,
                    val nemKgH: Double? = null, val cikisT: Double? = null, val bypass: Double? = null, val shf: Double? = null)
data class Cizgi(val a: Durum, val b: Durum, val kesikli: Boolean)
data class IslemSonucu(val ciktilar: List<Hava>, val cizgiler: List<Cizgi>, val kapasite: Kapasite)

enum class Parametre(private val tr: String, val birim: String) {
    KW("Kapasite", "kW"), SHF("SHF", "%"), KGH("Miktar", "kg/h"), BF("Bypass faktörü", "%"), BUHAR("Buharlı", ""), ARA("Ara çizgiler", "");
    val etiket get() = tr.c()
}

enum class IslemTuru(val daikinKod: Int, private val tr: String, val girdiSayisi: Int, val ciktiSayisi: Int, val parametreler: List<Parametre>) {
    NOKTA_GOSTER(0, "Noktayı göster", 1, 0, emptyList()),
    BAGLA(1, "Bağla", 2, 0, emptyList()),
    KARISIM(2, "Hava karışımı", 2, 1, emptyList()),
    SOGUTMA(3, "Soğutma", 2, 1, listOf(Parametre.ARA)),
    ISITMA(4, "Isıtma", 2, 1, listOf(Parametre.BUHAR, Parametre.ARA)),
    SOGUTMA_KAPASITE(5, "Soğutma kapasitesi", 1, 2, listOf(Parametre.KW, Parametre.SHF, Parametre.ARA)),
    ISITMA_KAPASITE(6, "Isıtma kapasitesi", 1, 1, listOf(Parametre.KW)),
    BUHAR_NEM(7, "Buharlı nemlendirme", 1, 1, listOf(Parametre.KGH)),
    SU_NEM(8, "Sulu nemlendirme", 1, 1, listOf(Parametre.KGH)),
    DOYMAYA_SOGUTMA(9, "Doymaya kadar soğutma", 1, 2, listOf(Parametre.SHF, Parametre.ARA)),
    YOGUSMA(10, "Yoğuşma", 2, 0, emptyList()),
    NEM_ALMA(11, "Nem alma", 1, 1, listOf(Parametre.KGH)),
    BYPASS(12, "Bypass faktörü", 1, 2, listOf(Parametre.KW, Parametre.SHF)),
    CIKIS(13, "Üfleme (discharge)", 1, 2, listOf(Parametre.SHF, Parametre.BF)),
    SHF(14, "Duyulur ısı faktörü", 1, 1, listOf(Parametre.SHF));
    val etiket get() = tr.c()
}

fun kutle(debi: Double) = 1.2 * debi / 1000

private fun sogutmaKapasitesi(m: Double, a: Durum, b: Durum) = Kapasite(
    toplam = m * (a.h - b.h), duyulur = m * cpm(b.W) * (a.t - b.t),
    gizli = m * (a.h - b.h) - m * cpm(b.W) * (a.t - b.t), shf = cpm(b.W) * (a.t - b.t) / (a.h - b.h) * 100)

/**
 * Cool to saturation ADP'si (exe 0x9b40a4): td(W1r)'den 5 °C adımla aşağı, oran = cpm(Wx)·|t1−tx|/|hx−h1| (Wx = Ws(tx) yuvarlanmış);
 * oran < SHF ise geri dön, adımı yarıla; |SHF−oran| <= 1e-4'te dur. Dönüş: ADP ve son hesaplanan T = m·|hx−h1|.
 */
fun doymaNoktasi(a: Durum, m: Double, shf: Double): Pair<Durum, Double> {
    val h1 = a.h; var tx = ciyNoktasi(a.Wr, a.p)
    var top = m * abs(entalpi(tx, a.Wr) - h1)
    if (shf >= 1) return Durum(tx, doymaW(tx, a.p), a.p) to top
    var adim = 5.0
    while (true) {
        tx -= adim; val wx = w5(doymaW(tx, a.p))
        if (tx <= -40 || wx <= 0) throw CozumHatasi("İstenen SHF ile doğru %100 nem eğrisini kesmiyor. SHF'yi düşürün ya da SHF işlemini kullanın.")
        top = m * abs(entalpi(tx, wx) - h1)
        val oran = cpm(wx) * m * abs(a.t - tx) / top
        if (oran < shf) { tx += adim; adim /= 2 }
        if (abs(shf - oran) <= 1e-4 || adim < 1e-10) return Durum(tx, doymaW(tx, a.p), a.p) to top
    }
}

/** Cooling capacity sonu (exe 0x9b3b34): t duyulurdan cpm(W1r)·1,2·Q ile, W gizliden 2500·Q·ρ_kuru(t1) ile. */
private fun sogutmaSonu(a: Durum, q: Double, kw: Double, shf: Double): Durum {
    val qm = q / 1000; val w = a.Wr - (kw - shf * kw) / (2500 * qm * kuruHavaYogunlugu(a.t, a.p))
    val c = Durum(a.t - shf * kw / (cpm(a.Wr) * 1.2 * qm), w, a.p)
    if (w <= 0 || c.rh > 1) throw CozumHatasi("İstenen soğutma ve SHF %100 nem eğrisini aşıyor. Kapasiteyi azaltın, SHF'yi değiştirin ya da doymaya soğutma + yoğuşma kullanın.")
    return c
}

private fun kapasite(top: Double, shf: Double, ek: Kapasite = Kapasite()) =
    ek.copy(toplam = top, duyulur = shf * top, gizli = top - shf * top, shf = shf * 100)

private fun doymaKontrol(d: Durum, mesaj: String) { if (d.W > doymaW(d.t, d.p) * (1 + 1e-9) || d.W < 0) throw CozumHatasi(mesaj) }

fun islemHesapla(tur: IslemTuru, girdi: List<Hava>, param: List<Double>): IslemSonucu {
    val a = girdi[0].d; val p = a.p; val m = kutle(girdi[0].debi); val q = girdi[0].debi
    fun hava(d: Durum, debi: Double = q) = Hava(d, debi)
    fun ara(vararg d: Durum) = d.toList().zipWithNext { x, y -> Cizgi(x, y, kesikli = true) }
    val araAcik = { i: Int -> param.getOrElse(i) { 1.0 } != 0.0 }
    return when (tur) {
        IslemTuru.NOKTA_GOSTER -> IslemSonucu(emptyList(), emptyList(), Kapasite())
        IslemTuru.BAGLA -> IslemSonucu(emptyList(), listOf(Cizgi(a, girdi[1].d, false)), Kapasite())
        IslemTuru.KARISIM -> {
            val b = girdi[1].d
            val m1 = girdi[0].debi / a.v; val m2 = girdi[1].debi / b.v  // exe 0x8449dc: yuvarlanmamış W ve h
            val W = (m1 * a.W + m2 * b.W) / (m1 + m2); val h = (m1 * entalpi(a.t, a.W) + m2 * entalpi(b.t, b.W)) / (m1 + m2)
            val c = Durum(tEntalpidan(h, W), W, p)
            IslemSonucu(listOf(hava(c, girdi[0].debi + girdi[1].debi)), listOf(Cizgi(a, c, false), Cizgi(b, c, false)), Kapasite())
        }
        IslemTuru.SOGUTMA -> {
            val b = girdi[1].d; val pp = Durum(a.t, b.W, p)
            IslemSonucu(listOf(hava(pp)), listOf(Cizgi(a, b, false)) + if (araAcik(0)) ara(a, pp, b) else emptyList(), sogutmaKapasitesi(m, a, b))
        }
        IslemTuru.ISITMA -> {
            // Daikin hedef daha soğuksa uçları yer değiştirir (test1: P1 27 °C -> P2 15 °C, PP = (27, W2), kapasiteler pozitif)
            val ters = girdi[1].d.t < a.t
            val x = if (ters) girdi[1].d else a; val b = if (ters) a else girdi[1].d
            val mh = kutle(if (ters) girdi[0].debi else girdi[1].debi)
            val buhar = param.getOrElse(0) { 1.0 } != 0.0
            val pp = if (buhar) Durum(b.t, x.W, p) else Durum(tEntalpidan(b.h, x.W), x.W, p)
            val duy = mh * cpm(b.W) * (b.t - x.t)
            IslemSonucu(listOf(hava(pp)), listOf(Cizgi(a, pp, false), Cizgi(pp, b, false)),
                Kapasite(toplam = mh * (b.h - x.h), duyulur = duy, gizli = mh * (b.h - x.h) - duy,
                    isitici = mh * (pp.h - x.h), nemKgH = kutle(girdi[0].debi) * (b.W - x.W) * 3600))
        }
        IslemTuru.ISITMA_KAPASITE -> {
            val c = Durum(a.t + param[0] / (m * cpm(a.W)), a.W, p)
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(isitici = param[0]))
        }
        IslemTuru.BUHAR_NEM -> {
            val kg = param[0] / 3600; val c = Durum(a.t, w5(a.W + kg / m), p)
            doymaKontrol(c, "Buharlı nemlendirme sonucu %100 nem eğrisini aşıyor. Miktarı azaltın.")
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(gizli = kg * (2501 + 1.84 * a.t), nemKgH = param[0]))
        }
        IslemTuru.SU_NEM, IslemTuru.NEM_ALMA -> {
            val kg = param[0] / 3600; val W = if (tur == IslemTuru.SU_NEM) a.W + kg / m else a.W - kg / m
            val c = Durum(tEntalpidan(a.h, W), w5(W), p)  // t yuvarlanmamış W'den, gösterilen W yuvarlanmış
            doymaKontrol(c, if (tur == IslemTuru.SU_NEM) "Sulu nemlendirme sonucu diyagram dışında. Miktarı azaltın ya da buharlı nemlendirme kullanın." else "Nem alma sonucu nem oranı negatif. Miktarı azaltın.")
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(gizli = kg * 2501, nemKgH = param[0]))
        }
        IslemTuru.SOGUTMA_KAPASITE -> {
            val kw = param[0]; val shf = param[1] / 100
            val c = sogutmaSonu(a, q, kw, shf); val pp2 = Durum(a.t, c.Wr, p)
            IslemSonucu(listOf(hava(c), hava(pp2)), listOf(Cizgi(a, c, false)) + if (araAcik(2)) ara(a, pp2, c) else emptyList(), kapasite(kw, shf))
        }
        IslemTuru.DOYMAYA_SOGUTMA -> {
            val shf = param[0] / 100; val (adp, top) = doymaNoktasi(a, m, shf); val pp2 = Durum(a.t, adp.Wr, p)
            IslemSonucu(listOf(hava(adp), hava(pp2)), listOf(Cizgi(a, adp, false)) + if (araAcik(1)) ara(a, pp2, adp) else emptyList(), kapasite(top, shf))
        }
        IslemTuru.CIKIS -> {
            // exe: T = (1−BF)·T_ADP; çıkış noktası T kW'lık Cooling capacity'nin sonu
            val shf = param[0] / 100; val (adp, topAdp) = doymaNoktasi(a, m, shf)
            val top = topAdp - param[1] / 100 * topAdp; val c = sogutmaSonu(a, q, top, shf)
            IslemSonucu(listOf(hava(c), hava(adp)), listOf(Cizgi(a, c, false), Cizgi(c, adp, true)),
                kapasite(top, shf, Kapasite(cikisT = c.t, bypass = param[1])))
        }
        IslemTuru.BYPASS -> {
            // exe: son = Cooling capacity sonu; ADP aynı doğruda, gerçek debiyle kW'tan başlayıp doymaya; BF = 1 − kW/T_ADP
            val kw = param[0]; val shf = param[1] / 100
            val c = sogutmaSonu(a, q, kw, shf); val (adp, topAdp) = shfAra(a, shf, q / 1000, kw, true)
            IslemSonucu(listOf(hava(c), hava(adp)), listOf(Cizgi(a, c, false), Cizgi(c, adp, true)),
                kapasite(kw, shf, Kapasite(cikisT = c.t, bypass = 100 - kw / topAdp * 100)))
        }
        IslemTuru.SHF -> {
            val c = shfKesisim(a, param[0] / 100)
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(shf = param[0]))
        }
        IslemTuru.YOGUSMA -> {
            val b = girdi[1].d
            if (a.rh < 0.999 || b.rh < 0.999) throw CozumHatasi("Yoğuşma işlemi için iki nokta da %100 nem eğrisinde olmalı.")
            val x = Durum(a.t, a.Wr, p); val y = Durum(b.t, b.Wr, p)  // kapasite yuvarlanmış W ile
            IslemSonucu(emptyList(), listOf(Cizgi(a, b, false)), sogutmaKapasitesi(m, x, y).copy(nemKgH = m * (x.W - y.W) * 3600))
        }
    }
}

package com.psikrometri.app.psy

data class Hava(val d: Durum, val debi: Double)
data class Kapasite(val toplam: Double? = null, val duyulur: Double? = null, val gizli: Double? = null, val isitici: Double? = null,
                    val nemKgH: Double? = null, val cikisT: Double? = null, val bypass: Double? = null, val shf: Double? = null)
data class Cizgi(val a: Durum, val b: Durum, val kesikli: Boolean)
data class IslemSonucu(val ciktilar: List<Hava>, val cizgiler: List<Cizgi>, val kapasite: Kapasite)

enum class Parametre(val etiket: String, val birim: String) {
    KW("Kapasite", "kW"), SHF("SHF", "%"), KGH("Miktar", "kg/h"), BF("Bypass faktörü", "%"), BUHAR("Buharlı", ""), ARA("Ara çizgiler", "")
}

enum class IslemTuru(val daikinKod: Int, val etiket: String, val girdiSayisi: Int, val ciktiSayisi: Int, val parametreler: List<Parametre>) {
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
    SHF(14, "Duyulur ısı faktörü", 1, 1, listOf(Parametre.SHF)),
}

fun kutle(debi: Double) = 1.2 * debi / 1000

private fun sogutmaKapasitesi(m: Double, a: Durum, b: Durum) = Kapasite(
    toplam = m * (a.h - b.h), duyulur = m * cpm(b.W) * (a.t - b.t),
    gizli = m * (a.h - b.h) - m * cpm(b.W) * (a.t - b.t), shf = cpm(b.W) * (a.t - b.t) / (a.h - b.h) * 100)

/** Doyma eğrisinde cpm(Wx)·(t1−tx)/(h1−hx) = SHF; t1'e en yakın kök. */
fun adpKapasite(a: Durum, shf: Double): Durum {
    val t = kokBul(a.t - 1e-6, -60.0, 0.05) { tx ->
        val w = doymaW(tx, a.p); val dh = a.h - entalpi(tx, w)
        if (dh <= 0) Double.NaN else cpm(w) * (a.t - tx) / dh - shf
    } ?: throw CozumHatasi("İstenen SHF ile doğru %100 nem eğrisini kesmiyor. SHF'yi düşürün ya da SHF işlemini kullanın.")
    return Durum(t, doymaW(t, a.p), a.p)
}

/** Duyulur kısım cpm(W1) ile t'yi, toplam enerji (yoğuşan suyun entalpisi 4.186·t1 düşülerek) W'yi belirler. */
private fun sogutmaSonu(a: Durum, m: Double, duyulur: Double, toplam: Double): Durum {
    val t = a.t - duyulur / (m * cpm(a.W)); val hw = 4.186 * a.t
    val W = (a.h - toplam / m - a.W * hw - 1.006 * t) / (2501 + 1.84 * t - hw)
    return Durum(t, W, a.p)
}

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
            val m1 = girdi[0].debi / a.v; val m2 = girdi[1].debi / b.v
            val W = (m1 * a.W + m2 * b.W) / (m1 + m2); val h = (m1 * a.h + m2 * b.h) / (m1 + m2)
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
            val kg = param[0] / 3600; val c = Durum(a.t, a.W + kg / m, p)
            doymaKontrol(c, "Buharlı nemlendirme sonucu %100 nem eğrisini aşıyor. Miktarı azaltın.")
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(gizli = kg * (2501 + 1.84 * a.t), nemKgH = param[0]))
        }
        IslemTuru.SU_NEM, IslemTuru.NEM_ALMA -> {
            val kg = param[0] / 3600; val W = if (tur == IslemTuru.SU_NEM) a.W + kg / m else a.W - kg / m
            val c = Durum(tEntalpidan(a.h, W), W, p)
            doymaKontrol(c, if (tur == IslemTuru.SU_NEM) "Sulu nemlendirme sonucu diyagram dışında. Miktarı azaltın ya da buharlı nemlendirme kullanın." else "Nem alma sonucu nem oranı negatif. Miktarı azaltın.")
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(gizli = kg * 2501, nemKgH = param[0]))
        }
        IslemTuru.SOGUTMA_KAPASITE -> {
            val kw = param[0]; val shf = param[1] / 100
            val c = sogutmaSonu(a, m, kw * shf, kw); val pp2 = Durum(a.t, c.W, p)
            doymaKontrol(c, "İstenen soğutma ve SHF %100 nem eğrisini aşıyor. Kapasiteyi azaltın, SHF'yi değiştirin ya da doymaya soğutma + yoğuşma kullanın.")
            IslemSonucu(listOf(hava(c), hava(pp2)), listOf(Cizgi(a, c, false)) + if (araAcik(2)) ara(a, pp2, c) else emptyList(),
                Kapasite(toplam = kw, duyulur = kw * shf, gizli = kw * (1 - shf), shf = param[1]))
        }
        IslemTuru.DOYMAYA_SOGUTMA -> {
            val adp = adpKapasite(a, param[0] / 100); val pp2 = Durum(a.t, adp.W, p)
            IslemSonucu(listOf(hava(adp), hava(pp2)), listOf(Cizgi(a, adp, false)) + if (araAcik(1)) ara(a, pp2, adp) else emptyList(), sogutmaKapasitesi(m, a, adp))
        }
        IslemTuru.CIKIS -> {
            val shf = param[0] / 100; val adp = adpKapasite(a, shf); val bf = param[1] / 100
            val tc = adp.t + bf * (a.t - adp.t); val duy = m * cpm(a.W) * (a.t - tc)
            val c = sogutmaSonu(a, m, duy, duy / shf)
            IslemSonucu(listOf(hava(c), hava(adp)), listOf(Cizgi(a, c, false), Cizgi(c, adp, true)),
                Kapasite(toplam = duy / shf, duyulur = duy, gizli = duy / shf - duy, cikisT = c.t, bypass = param[1]))
        }
        IslemTuru.YOGUSMA -> {
            val b = girdi[1].d
            if (a.rh < 0.999 || b.rh < 0.999) throw CozumHatasi("Yoğuşma işlemi için iki nokta da %100 nem eğrisinde olmalı.")
            IslemSonucu(emptyList(), listOf(Cizgi(a, b, false)), sogutmaKapasitesi(m, a, b).copy(nemKgH = m * (a.W - b.W) * 3600))
        }
        IslemTuru.SHF, IslemTuru.BYPASS -> throw CozumHatasi("Task 6")
    }
}

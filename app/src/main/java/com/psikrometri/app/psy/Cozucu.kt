package com.psikrometri.app.psy

enum class Ozellik(val daikinKod: Int, val etiket: String) {
    KURU(0, "Kuru termometre"), MUTLAK(1, "Mutlak nem"), BAGIL(2, "Bağıl nem"), ENTALPI(3, "Entalpi"),
    YAS(4, "Yaş termometre"), HACIM(5, "Özgül hacim"), YOGUNLUK(6, "Yoğunluk")
}

class CozumHatasi(mesaj: String) : Exception(mesaj)

/** Çözücü için yuvarlanmamış değerler (Durum.h/twb Daikin gösterimi gibi Wr kullanır). */
fun Durum.deger(o: Ozellik) = when (o) {
    Ozellik.KURU -> t; Ozellik.MUTLAK -> W; Ozellik.BAGIL -> rh * 100; Ozellik.ENTALPI -> entalpi(t, W)
    Ozellik.YAS -> yasTermometre(t, W, p); Ozellik.HACIM -> v; Ozellik.YOGUNLUK -> rho
}

/** t bilinirken o=v sağlayan W (hepsi kapalı form). */
private fun wBul(t: Double, o: Ozellik, v: Double, p: Double): Double {
    val T = t + 273.15
    return when (o) {
        Ozellik.MUTLAK -> v
        Ozellik.BAGIL -> nemOrani(v / 100 * pws(t), p)
        Ozellik.ENTALPI -> (v - 1.006 * t) / (2501 + 1.84 * t)
        Ozellik.YAS -> yasW(t, v, p)
        Ozellik.HACIM -> (v * p / (RDA * T) - 1) / 1.607774067
        Ozellik.YOGUNLUK -> (p - v * RDA * T) / (1.607774067 * v * RDA * T - p)
        Ozellik.KURU -> error("KURU wBul'a gelmez")
    }
}

private fun gecerli(t: Double, W: Double, p: Double) = W >= 0 && W <= doymaW(t, p) * (1 + 1e-9)

fun coz(o1: Ozellik, v1: Double, o2: Ozellik, v2: Double, p: Double): Durum {
    if (o1 == o2) throw CozumHatasi("Aynı özellik iki kez seçilemez")
    // KURU, sonra MUTLAK öne alınır: biri varsa diğeri kapalı formdan ya da tek boyutlu aramayla bulunur.
    val ters = o2 == Ozellik.KURU || (o2 == Ozellik.MUTLAK && o1 != Ozellik.KURU)
    val a = if (ters) o2 else o1; val va = if (ters) v2 else v1
    val b = if (ters) o1 else o2; val vb = if (ters) v1 else v2
    val d = when (a) {
        Ozellik.KURU -> Durum(va, wBul(va, b, vb, p), p)
        Ozellik.MUTLAK -> {
            val t = kokBul(-60.0, 100.0, 0.25) { t -> Durum(t, va, p).deger(b) - vb }
                ?: throw CozumHatasi("Bu değerlerle nokta bulunamadı")
            Durum(t, va, p)
        }
        else -> {
            val t = kokBul(-60.0, 100.0, 0.25) { t ->
                val W = wBul(t, a, va, p)
                if (!gecerli(t, W, p)) Double.NaN else Durum(t, W, p).deger(b) - vb
            } ?: throw CozumHatasi("Bu iki özellik birlikte noktayı belirlemiyor")
            Durum(t, wBul(t, a, va, p), p)
        }
    }
    if (d.t < -60 || d.t > 100) throw CozumHatasi("Sıcaklık -60…100 °C dışında")
    if (!gecerli(d.t, d.W, p)) throw CozumHatasi(if (d.W < 0) "Nem oranı negatif" else "Nokta doyma eğrisinin üstünde (RH > %100)")
    return d
}

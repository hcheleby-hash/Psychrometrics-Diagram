package com.psikrometri.app.model

import com.psikrometri.app.psy.Ozellik

data class Birim(val etiket: String, val carpan: Double, val ofset: Double = 0.0) {
    fun goster(si: Double) = si * carpan + ofset
    fun siye(v: Double) = (v - ofset) / carpan
}

enum class Buyukluk(val secenekler: List<Birim>) {
    SICAKLIK(listOf(Birim("°C", 1.0), Birim("°F", 1.8, 32.0))),
    NEM(listOf(Birim("kg/kg", 1.0), Birim("g/kg", 1000.0))),
    DEBI(listOf(Birim("l/s", 1.0), Birim("m³/h", 3.6), Birim("m³/s", 0.001))),
    ENTALPI(listOf(Birim("kJ/kg", 1.0), Birim("kcal/kg", 1 / 4.1868))),
    BASINC(listOf(Birim("Pa", 1.0), Birim("kPa", 0.001), Birim("mbar", 0.01))),
    GUC(listOf(Birim("kW", 1.0), Birim("kcal/h", 859.845)))
}

data class BirimSecimi(val secim: Map<Buyukluk, Int> = emptyMap()) {
    fun birim(b: Buyukluk) = b.secenekler[secim[b] ?: 0]
    fun ozellikBirimi(o: Ozellik) = when (o) {
        Ozellik.KURU, Ozellik.YAS -> birim(Buyukluk.SICAKLIK)
        Ozellik.MUTLAK -> birim(Buyukluk.NEM)
        Ozellik.ENTALPI -> birim(Buyukluk.ENTALPI)
        Ozellik.BAGIL -> Birim("%", 1.0)
        Ozellik.HACIM -> Birim("m³/kg", 1.0)
        Ozellik.YOGUNLUK -> Birim("kg/m³", 1.0)
    }
}

package com.psikrometri.app.psy

/**
 * SHF doğrusu (Daikin exe 0x9b4384 / 0x9b3d30): P1'den Cooling capacity adımları. Kapasite c için
 * t = t1 − SHF·c/(cpm(W1r)·1,2·Q), W = W1r − (1−SHF)·c/(2500·Q·ρ_kuru(t1)) — t–W'de düz doğru.
 * c, 1 kW'dan başlayıp 1e-4'e inen adımlarla RH > 100 olana dek artırılır; son hesaplanan nokta döner
 * (RH biraz > 100 olabilir, Daikin W'yi doymaya kırpar). W <= 0 olursa adım geri alınır, nokta güncellenmez.
 * kontrol = true (Bypass factor): RH düşerse ya da W <= 0 olursa hata. Dönüş: nokta ve son kapasite (kW).
 */
internal fun shfAra(a: Durum, shf: Double, qm: Double, c0: Double, kontrol: Boolean): Pair<Durum, Double> {
    val w1 = a.Wr; val cp = cpm(w1) * 1.2; val rho = kuruHavaYogunlugu(a.t, a.p)
    var c = c0; var adim = 1.0; var t = a.t; var w = a.W
    while (adim >= 1e-4) {
        c += adim
        val w2 = w1 - (c - shf * c) / (2500 * qm * rho)
        if (w2 <= 0 && !kontrol) { c -= adim; adim /= 10; continue }
        t = a.t - shf * c / (cp * qm); w = w2
        val rh = Durum(t, w, a.p).rh
        if (kontrol && (rh < a.rh || w2 <= 0)) throw CozumHatasi("SHF doğrusu %100 nem eğrisini kesmiyor.")
        if (rh > 1) { c -= adim; adim /= 10 }
    }
    return Durum(t, minOf(w, doymaW(t, a.p)), a.p) to c
}

/** SHF doğrusunun doyma eğrisiyle (yoksa W≈0 ile) kesişimi; Daikin SHF işlemi Q = 0,1 m³/s ile arar. */
fun shfKesisim(a: Durum, shf: Double): Durum = shfAra(a, shf, 0.1, 0.0, false).first

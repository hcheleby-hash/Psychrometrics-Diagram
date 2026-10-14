package com.psikrometri.app.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.psikrometri.app.model.Proje
import com.psikrometri.app.model.dil
import com.psikrometri.app.psy.Durum

class AnaVM(app: Application, private val ss: SavedStateHandle) : AndroidViewModel(app) {
    // süreç ölümünden sonra (ve SAF geri dönüşü yeni VM'e düşerse) proje kaybolmasın
    private var _proje by mutableStateOf(ss.get<String>("proje")?.let { runCatching { Proje.jsonOku(it) }.getOrNull() } ?: Proje())
    var proje: Proje
        get() = _proje
        set(v) { _proje = v; ss["proje"] = v.json() }
    var secili by mutableStateOf<String?>(null)
    var onizleme by mutableStateOf<Durum?>(null)
    var ayarlar by mutableStateOf(ayarlariOku(app).also { dil = it.dil })
        private set
    // hata metinleri hesap anında çevrilir: dil değişince yeniden hesapla
    val hesap by derivedStateOf { ayarlar.dil; proje.hesapla() }
    val egriler by derivedStateOf { egrileriUret(proje.basinc, ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, ayarlar.egriler) }
    fun ayarDegistir(a: Ayarlar) { dil = a.dil; ayarlar = a; ayarlariYaz(getApplication(), a) }
}

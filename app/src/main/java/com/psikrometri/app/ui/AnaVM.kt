package com.psikrometri.app.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.psikrometri.app.model.Proje
import com.psikrometri.app.psy.Durum

class AnaVM(app: Application) : AndroidViewModel(app) {
    var proje by mutableStateOf(Proje())
    var secili by mutableStateOf<String?>(null)
    var onizleme by mutableStateOf<Durum?>(null)
    var ayarlar by mutableStateOf(ayarlariOku(app))
        private set
    val hesap by derivedStateOf { proje.hesapla() }
    val egriler by derivedStateOf { egrileriUret(proje.basinc, ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, ayarlar.egriler) }
    fun ayarDegistir(a: Ayarlar) { ayarlar = a; ayarlariYaz(getApplication(), a) }
}

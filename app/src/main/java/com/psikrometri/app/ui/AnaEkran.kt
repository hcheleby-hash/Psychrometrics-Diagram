package com.psikrometri.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.psikrometri.app.psy.Durum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnaEkran(vm: AnaVM = viewModel()) {
    var sekme by rememberSaveable { mutableIntStateOf(0) }
    var noktaTaslak by remember { mutableStateOf<Durum?>(null) }
    var rakimAcik by remember { mutableStateOf(false) }
    var ayarAcik by remember { mutableStateOf(false) }
    val dosya = dosyaIslemleri(vm)  // Task 12
    val yatay = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    MaterialTheme {
        Scaffold(topBar = {
            if (!yatay) TopAppBar(title = { Text("Psikrometri") }, actions = {
                AssistChip({ rakimAcik = true }, label = { Text("${vm.proje.rakim.yaz(0)} m · ${(vm.proje.basinc / 1000).yaz(2)} kPa") })
                var menu by remember { mutableStateOf(false) }
                IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Yeni") }, { vm.proje = com.psikrometri.app.model.Proje(); vm.secili = null; menu = false })
                    DropdownMenuItem({ Text("Aç") }, { dosya.ac(); menu = false })
                    DropdownMenuItem({ Text("Kaydet") }, { dosya.kaydet(); menu = false })
                    DropdownMenuItem({ Text("Rapor (PDF)") }, { dosya.rapor(); menu = false })
                    DropdownMenuItem({ Text("Ayarlar") }, { ayarAcik = true; menu = false })
                }
            })
        }) { ic ->
            if (yatay) Diyagram(vm, Modifier.fillMaxSize().padding(ic)) { noktaTaslak = it; sekme = 0 }
            else Column(Modifier.padding(ic).fillMaxSize()) {
                Diyagram(vm, Modifier.fillMaxWidth().weight(0.45f)) { noktaTaslak = it; sekme = 0 }
                TabRow(sekme) { listOf("Noktalar", "İşlemler", "Veri").forEachIndexed { i, s -> Tab(sekme == i, { sekme = i }, text = { Text(s) }) } }
                Box(Modifier.weight(0.55f).fillMaxWidth()) {
                    when (sekme) {
                        0 -> NoktalarSekmesi(vm, noktaTaslak) { noktaTaslak = null }
                        1 -> IslemlerSekmesi(vm)
                        else -> VeriSekmesi(vm)
                    }
                }
            }
        }
        if (rakimAcik) RakimDiyalog(vm) { rakimAcik = false }
        if (ayarAcik) AyarDiyalog(vm) { ayarAcik = false }
    }
}

@Composable
private fun RakimDiyalog(vm: AnaVM, kapat: () -> Unit) {
    var rakim by remember { mutableStateOf(vm.proje.rakim.yaz(0)) }
    var basinc by remember { mutableStateOf((vm.proje.basinc / 1000).yaz(3)) }
    AlertDialog(kapat, confirmButton = { TextButton({ rakim.sayi()?.let { vm.proje = vm.proje.copy(rakim = it) }; kapat() }) { Text("Tamam") } },
        title = { Text("Rakım / basınç") }, text = { Column {
            SayiAlani(rakim, { rakim = it; it.sayi()?.let { z -> basinc = (com.psikrometri.app.psy.basincRakimdan(z) / 1000).yaz(3) } }, "Rakım", "m")
            SayiAlani(basinc, { basinc = it; it.sayi()?.let { p -> rakim = com.psikrometri.app.psy.rakimBasinctan(p * 1000).yaz(0) } }, "Basınç", "kPa")
        } })
}

@Composable fun AyarDiyalog(vm: AnaVM, kapat: () -> Unit) { kapat() }
class DosyaIslemleri(val ac: () -> Unit, val kaydet: () -> Unit, val rapor: () -> Unit)
@Composable fun dosyaIslemleri(vm: AnaVM) = DosyaIslemleri({}, {}, {})
@Composable fun NoktalarSekmesi(vm: AnaVM, taslak: Durum?, taslakKullanildi: () -> Unit) { Text("Noktalar") }
@Composable fun IslemlerSekmesi(vm: AnaVM) { Text("İşlemler") }
@Composable fun VeriSekmesi(vm: AnaVM) { Text("Veri") }

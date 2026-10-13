package com.psikrometri.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
    var yeniSor by remember { mutableStateOf(false) }
    fun yeni() { vm.proje = com.psikrometri.app.model.Proje(); vm.secili = null; vm.onizleme = null }
    val dosya = dosyaIslemleri(vm)
    val yatay = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    MaterialTheme {
        val eylemler: @Composable RowScope.() -> Unit = {
            AssistChip({ rakimAcik = true }, label = { Text("${vm.proje.rakim.yaz(0)} m · ${(vm.proje.basinc / 1000).yaz(2)} kPa") })
            var menu by remember { mutableStateOf(false) }
            IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Yeni") }, { if (vm.proje.noktalar.isEmpty() && vm.proje.islemler.isEmpty()) yeni() else yeniSor = true; menu = false })
                DropdownMenuItem({ Text("Aç") }, { dosya.ac(); menu = false })
                DropdownMenuItem({ Text("Kaydet") }, { dosya.kaydet(); menu = false })
                DropdownMenuItem({ Text("Rapor (PDF)") }, { dosya.rapor(); menu = false })
                DropdownMenuItem({ Text("Ayarlar") }, { ayarAcik = true; menu = false })
            }
        }
        val sekmeler: @Composable ColumnScope.() -> Unit = {
            TabRow(sekme) { listOf("Noktalar", "İşlemler", "Veri").forEachIndexed { i, s -> Tab(sekme == i, { sekme = i }, text = { Text(s) }) } }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (sekme) {
                    0 -> NoktalarSekmesi(vm, noktaTaslak) { noktaTaslak = null }
                    1 -> IslemlerSekmesi(vm)
                    else -> VeriSekmesi(vm)
                }
            }
        }
        Scaffold(topBar = {
            if (!yatay) TopAppBar(title = { Text("Psikrometri") }, actions = eylemler)
        }) { ic ->
            if (yatay) Row(Modifier.padding(ic).fillMaxSize()) {
                Diyagram(vm, Modifier.weight(0.55f).fillMaxHeight()) { noktaTaslak = it; sekme = 0 }
                Column(Modifier.weight(0.45f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically, content = eylemler)
                    sekmeler()
                }
            }
            else Column(Modifier.padding(ic).fillMaxSize()) {
                Diyagram(vm, Modifier.fillMaxWidth().weight(0.45f)) { noktaTaslak = it; sekme = 0 }
                Column(Modifier.weight(0.55f).fillMaxWidth(), content = sekmeler)
            }
        }
        if (rakimAcik) RakimDiyalog(vm) { rakimAcik = false }
        if (ayarAcik) AyarDiyalog(vm) { ayarAcik = false }
        if (yeniSor) AlertDialog({ yeniSor = false }, confirmButton = { TextButton({ yeni(); yeniSor = false }) { Text("Sil ve yeni") } },
            dismissButton = { TextButton({ yeniSor = false }) { Text("Vazgeç") } }, title = { Text("Yeni proje") },
            text = { Text("Mevcut noktalar ve işlemler silinecek. Kaydetmediyseniz kaybolur.") })
    }
}

@Composable
private fun RakimDiyalog(vm: AnaVM, kapat: () -> Unit) {
    var rakim by remember { mutableStateOf(vm.proje.rakim.yaz(0)) }
    var basinc by remember { mutableStateOf((vm.proje.basinc / 1000).yaz(3)) }
    val z = rakim.sayi()?.takeIf { it in -500.0..10000.0 }
    AlertDialog(kapat, confirmButton = { TextButton({ vm.proje = vm.proje.copy(rakim = z!!); kapat() }, enabled = z != null) { Text("Tamam") } },
        title = { Text("Rakım / basınç") }, text = { Column {
            SayiAlani(rakim, { rakim = it; it.sayi()?.let { z -> basinc = (com.psikrometri.app.psy.basincRakimdan(z) / 1000).yaz(3) } }, "Rakım", "m",
                hata = if (z == null) "−500 … 10000 m" else null)
            SayiAlani(basinc, { basinc = it; it.sayi()?.let { p -> rakim = com.psikrometri.app.psy.rakimBasinctan(p * 1000).yaz(0) } }, "Basınç", "kPa")
        } })
}


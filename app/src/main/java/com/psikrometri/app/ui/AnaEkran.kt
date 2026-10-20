package com.psikrometri.app.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.psikrometri.app.model.RAKIM_ARALIK
import com.psikrometri.app.model.c
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
    // yatay kullanım ayarlardan açılır; kapalıyken (varsayılan) ekran dikeyde kilitli
    val etkinlik = LocalContext.current as android.app.Activity
    LaunchedEffect(vm.ayarlar.yatay) { etkinlik.requestedOrientation = if (vm.ayarlar.yatay) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    MaterialTheme { key(vm.ayarlar.dil) {
        val eylemler: @Composable RowScope.() -> Unit = {
            AssistChip({ rakimAcik = true }, label = { Text("${vm.proje.rakim.yaz(0)} m · ${(vm.proje.basinc / 1000).yaz(2)} kPa") })
            var menu by remember { mutableStateOf(false) }
            IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Menü".c()) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Yeni".c()) }, { if (vm.proje.noktalar.isEmpty() && vm.proje.islemler.isEmpty()) yeni() else yeniSor = true; menu = false })
                DropdownMenuItem({ Text("Aç".c()) }, { dosya.ac(); menu = false })
                DropdownMenuItem({ Text("Kaydet".c()) }, { dosya.kaydet(); menu = false })
                DropdownMenuItem({ Text("Rapor (PDF)".c()) }, { dosya.rapor(); menu = false })
                DropdownMenuItem({ Text("Ayarlar".c()) }, { ayarAcik = true; menu = false })
            }
        }
        // Daikin gibi açılır bölümler: başlığa basınca açılır, yeniden basınca kapanır; aynı anda yalnız biri açık
        val sekmeler: @Composable ColumnScope.() -> Unit = {
            listOf("Noktalar", "İşlemler", "Veri").forEachIndexed { i, s ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    .clickable { sekme = if (sekme == i) -1 else i }) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (sekme == i) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                        Text("  " + s.c(), style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (sekme == i) Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (i) {
                        0 -> NoktalarSekmesi(vm, noktaTaslak) { noktaTaslak = null }
                        1 -> IslemlerSekmesi(vm)
                        else -> VeriSekmesi(vm)
                    }
                }
            }
        }
        Scaffold(topBar = {
            if (!yatay) TopAppBar(title = { Text("Psikrometri".c()) }, actions = eylemler)
        }) { ic ->
            // kenardan kenara düzen (MainActivity): klavye pencereyi küçültmez, ekran aynı kalır, klavye üstüne biner
            if (yatay) Row(Modifier.padding(ic).fillMaxSize()) {
                Column(Modifier.weight(0.45f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically, content = eylemler)
                    sekmeler()
                }
                Diyagram(vm, Modifier.weight(0.55f).fillMaxHeight()) { noktaTaslak = it; sekme = 0 }
            }
            else Column(Modifier.padding(ic).fillMaxSize()) {
                // bölümler açılıp kapansa da grafik alanı sabit; ikisini çizgi ayırır
                Column(Modifier.weight(0.55f).fillMaxWidth(), content = sekmeler)
                HorizontalDivider(thickness = 2.dp)
                Diyagram(vm, Modifier.fillMaxWidth().weight(0.45f)) { noktaTaslak = it; sekme = 0 }
            }
        }
        if (rakimAcik) RakimDiyalog(vm) { rakimAcik = false }
        if (ayarAcik) AyarDiyalog(vm) { ayarAcik = false }
        if (yeniSor) AlertDialog({ yeniSor = false }, confirmButton = { TextButton({ yeni(); yeniSor = false }) { Text("Sil ve yeni".c()) } },
            dismissButton = { TextButton({ yeniSor = false }) { Text("Vazgeç".c()) } }, title = { Text("Yeni proje".c()) },
            text = { Text("Mevcut noktalar ve işlemler silinecek. Kaydetmediyseniz kaybolur.".c()) })
    } }
}

@Composable
private fun RakimDiyalog(vm: AnaVM, kapat: () -> Unit) {
    var rakim by remember { mutableStateOf(vm.proje.rakim.yaz(0)) }
    var basinc by remember { mutableStateOf((vm.proje.basinc / 1000).yaz(3)) }
    val z = rakim.sayi()?.takeIf { it in RAKIM_ARALIK }
    AlertDialog(kapat, confirmButton = { TextButton({ vm.proje = vm.proje.copy(rakim = z!!); kapat() }, enabled = z != null) { Text("Tamam".c()) } },
        title = { Text("Rakım / basınç".c()) }, text = { Column {
            SayiAlani(rakim, { rakim = it; it.sayi()?.let { z -> basinc = (com.psikrometri.app.psy.basincRakimdan(z) / 1000).yaz(3) } }, "Rakım".c(), "m",
                hata = if (z == null) "−500 … 10000 m" else null)
            SayiAlani(basinc, { basinc = it; it.sayi()?.let { p -> rakim = com.psikrometri.app.psy.rakimBasinctan(p * 1000).yaz(0) } }, "Basınç".c(), "kPa")
        } })
}


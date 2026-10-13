package com.psikrometri.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.model.Proje
import com.psikrometri.app.rapor.pdfYaz
import java.io.File

class DosyaIslemleri(val ac: () -> Unit, val kaydet: () -> Unit, val rapor: () -> Unit)

@Composable
fun dosyaIslemleri(vm: AnaVM): DosyaIslemleri {
    val ctx = LocalContext.current
    val kaydet = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(vm.proje.json().toByteArray()) } }
            .onSuccess { Toast.makeText(ctx, "Kaydedildi", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "Kaydedilemedi: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    val ac = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { Proje.jsonOku(ctx.contentResolver.openInputStream(uri)!!.bufferedReader().readText()) }
            .onSuccess { vm.proje = it; vm.secili = null; vm.onizleme = null }
            .onFailure { Toast.makeText(ctx, "Dosya okunamadı: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    return DosyaIslemleri(
        ac = { ac.launch(arrayOf("*/*")) },
        kaydet = { kaydet.launch("proje.psy") },
        rapor = {
            val f = File(ctx.cacheDir, "rapor/psikrometri.pdf").apply { parentFile!!.mkdirs() }
            f.outputStream().use { pdfYaz(it, vm.proje, vm.hesap, vm.egriler, vm.ayarlar) }
            val uri = FileProvider.getUriForFile(ctx, "com.psikrometri.app.dosya", f)
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Raporu paylaş"))
        })
}

@Composable
fun AyarDiyalog(vm: AnaVM, kapat: () -> Unit) {
    var a by remember { mutableStateOf(vm.ayarlar) }
    var tMin by remember { mutableStateOf(a.tMin.yaz(0)) }; var tMax by remember { mutableStateOf(a.tMax.yaz(0)) }; var wMax by remember { mutableStateOf(a.wMax.yaz(3)) }
    AlertDialog(kapat, confirmButton = { TextButton({
        vm.ayarDegistir(a.copy(tMin = tMin.sayi() ?: a.tMin, tMax = tMax.sayi() ?: a.tMax, wMax = wMax.sayi() ?: a.wMax)); kapat() }) { Text("Kaydet") } },
        dismissButton = { TextButton(kapat) { Text("Vazgeç") } }, title = { Text("Ayarlar") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (b in Buyukluk.entries) SecimKutusu(b.secenekler.indices.toList(), a.birimler.secim[b] ?: 0, { b.secenekler[it].etiket },
                { i -> a = a.copy(birimler = BirimSecimi(a.birimler.secim + (b to i))) }, Modifier.fillMaxWidth(), b.name.lowercase().replaceFirstChar { it.uppercase() })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SayiAlani(tMin, { tMin = it }, "t min", "°C", Modifier.weight(1f)); SayiAlani(tMax, { tMax = it }, "t max", "°C", Modifier.weight(1f))
            }
            SayiAlani(wMax, { wMax = it }, "W max", "kg/kg", Modifier.fillMaxWidth())
            for (e in EgriTuru.entries) Row { Checkbox(e in a.egriler, { c -> a = a.copy(egriler = if (c) a.egriler + e else a.egriler - e) }); Text(e.etiket, Modifier.padding(top = 12.dp)) }
        } })
}

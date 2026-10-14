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
import com.psikrometri.app.model.Dil
import com.psikrometri.app.model.c
import com.psikrometri.app.rapor.pdfYaz
import java.io.File

class DosyaIslemleri(val ac: () -> Unit, val kaydet: () -> Unit, val rapor: () -> Unit)

@Composable
fun dosyaIslemleri(vm: AnaVM): DosyaIslemleri {
    val ctx = LocalContext.current
    val kaydet = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(vm.proje.json().toByteArray()) } }
            .onSuccess { Toast.makeText(ctx, "Kaydedildi".c(), Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "Kaydedilemedi".c(), Toast.LENGTH_LONG).show() }
    }
    val ac = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { Proje.jsonOku(ctx.contentResolver.openInputStream(uri)!!.use { it.bufferedReader().readText() }) }
            .onSuccess { vm.proje = it; vm.secili = null; vm.onizleme = null }
            .onFailure { Toast.makeText(ctx, "Dosya okunamadı".c(), Toast.LENGTH_LONG).show() }
    }
    return DosyaIslemleri(
        ac = { ac.launch(arrayOf("*/*")) },
        kaydet = { kaydet.launch("proje.psy") },
        rapor = {
            runCatching {
            val f = File(ctx.cacheDir, "rapor/psikrometri.pdf").apply { parentFile!!.mkdirs() }
            f.outputStream().use { pdfYaz(it, vm.proje, vm.hesap, vm.egriler, vm.ayarlar) }
            val uri = FileProvider.getUriForFile(ctx, "com.psikrometri.app.dosya", f)
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Raporu paylaş".c()))
            }.onFailure { Toast.makeText(ctx, "Rapor oluşturulamadı".c(), Toast.LENGTH_LONG).show() }
        })
}

@Composable
fun AyarDiyalog(vm: AnaVM, kapat: () -> Unit) {
    var a by remember { mutableStateOf(vm.ayarlar) }
    val tb = a.birimler.birim(Buyukluk.SICAKLIK); val nb = a.birimler.birim(Buyukluk.NEM)
    var tMin by remember { mutableStateOf(tb.goster(a.tMin).yaz(0)) }; var tMax by remember { mutableStateOf(tb.goster(a.tMax).yaz(0)) }; var wMax by remember { mutableStateOf(nb.goster(a.wMax).yaz(3)) }
    val t1 = tMin.sayi(); val t2 = tMax.sayi(); val w = wMax.sayi()
    val yeni = if (t1 != null && t2 != null && w != null) a.copy(tMin = tb.siye(t1), tMax = tb.siye(t2), wMax = nb.siye(w)).takeIf { it.gecerli } else null
    AlertDialog(kapat, confirmButton = { TextButton({ vm.ayarDegistir(yeni!!); kapat() }, enabled = yeni != null) { Text("Kaydet".c()) } },
        dismissButton = { TextButton(kapat) { Text("Vazgeç".c()) } }, title = { Text("Ayarlar".c()) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SecimKutusu(Dil.entries, a.dil, { it.ad }, { a = a.copy(dil = it) }, Modifier.fillMaxWidth(), "Dil".c())
            for (b in Buyukluk.entries) SecimKutusu(b.secenekler.indices.toList(), a.birimler.secim[b] ?: 0, { b.secenekler[it].etiket },
                { i ->
                    val eski = a.birimler.birim(b); val yeni = b.secenekler[i]
                    fun donustur(t: String, d: Int) = t.sayi()?.let { yeni.goster(eski.siye(it)).yaz(d) } ?: t
                    if (b == Buyukluk.SICAKLIK) { tMin = donustur(tMin, 0); tMax = donustur(tMax, 0) }
                    if (b == Buyukluk.NEM) wMax = donustur(wMax, 3)
                    a = a.copy(birimler = BirimSecimi(a.birimler.secim + (b to i)))
                }, Modifier.fillMaxWidth(), b.etiket)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SayiAlani(tMin, { tMin = it }, "t min", tb.etiket, Modifier.weight(1f)); SayiAlani(tMax, { tMax = it }, "t max", tb.etiket, Modifier.weight(1f))
            }
            SayiAlani(wMax, { wMax = it }, "W max", nb.etiket, Modifier.fillMaxWidth())
            if (yeni == null) Text("Geçerli aralık".c() + ": ${tb.goster(-60.0).kisa()} ≤ t min < t max ≤ ${tb.goster(100.0).kisa()} ${tb.etiket}, 0 < W max ≤ ${nb.goster(0.1).kisa()} ${nb.etiket}",
                color = MaterialTheme.colorScheme.error)
            for (e in EgriTuru.entries) Row { Checkbox(e in a.egriler, { c -> a = a.copy(egriler = if (c) a.egriler + e else a.egriler - e) }); Text(e.etiket, Modifier.padding(top = 12.dp)) }
        } })
}

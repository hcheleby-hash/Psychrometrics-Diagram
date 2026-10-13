package com.psikrometri.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psikrometri.app.model.Buyukluk

@Composable
fun VeriSekmesi(vm: AnaVM) {
    val ad = vm.secili
    val h = ad?.let { vm.hesap.hava[it] }
    val b = vm.ayarlar.birimler
    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        if (h == null) { Text("Diyagramda ya da Noktalar sekmesinde bir nokta seçin."); return@Column }
        val d = h.d
        val tb = b.birim(Buyukluk.SICAKLIK); val nb = b.birim(Buyukluk.NEM); val hb = b.birim(Buyukluk.ENTALPI)
        val satirlar = listOf(
            "Kuru termometre" to "${tb.goster(d.t).yaz(1)} ${tb.etiket}", "Yaş termometre" to "${tb.goster(d.twb).yaz(1)} ${tb.etiket}",
            "Çiy noktası" to "${tb.goster(d.td).yaz(1)} ${tb.etiket}", "Bağıl nem" to "%${(d.rh * 100).yaz(1)}",
            "Mutlak nem" to nb.yazB(d.Wr, b.nemOndalik()),
            "Entalpi" to "${b.birim(Buyukluk.ENTALPI).goster(d.h).yaz(1)} ${b.birim(Buyukluk.ENTALPI).etiket}",
            "Yoğunluk" to "${d.rho.yaz(3)} kg/m³", "Özgül hacim" to "${d.v.yaz(3)} m³/kg",
            "Basınç" to "${b.birim(Buyukluk.BASINC).goster(d.p).yaz(1)} ${b.birim(Buyukluk.BASINC).etiket}",
            "Debi" to "${b.birim(Buyukluk.DEBI).goster(h.debi).yaz(1)} ${b.birim(Buyukluk.DEBI).etiket}")
        Text(ad!!, style = MaterialTheme.typography.titleMedium)
        for ((k, v) in satirlar) Row(Modifier.fillMaxWidth()) { Text(k, Modifier.weight(1f)); Text(v) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        vm.proje.islemler.forEachIndexed { i, isl ->
            if (ad in isl.girdiler || ad in isl.ciktilar) {
                val r = vm.hesap.sonuclar[i].getOrNull() ?: return@forEachIndexed
                val bas = vm.hesap.hava[isl.girdiler[0]]?.d
                val son = r.ciktilar.firstOrNull()?.d ?: isl.girdiler.getOrNull(1)?.let { vm.hesap.hava[it]?.d }
                Text("${isl.tur.etiket} (${(isl.girdiler + isl.ciktilar).joinToString(", ")})", style = MaterialTheme.typography.labelLarge)
                if (bas != null && son != null) Text("Δt ${tb.farkYaz(son.t - bas.t, 1)} · ΔW ${nb.farkYaz(son.Wr - bas.Wr, b.nemOndalik())} · Δh ${hb.farkYaz(son.h - bas.h, 1)}")
                Text(r.kapasite.metin(b))
            }
        }
    }
}

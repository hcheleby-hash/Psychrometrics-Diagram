package com.psikrometri.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.model.Islem
import com.psikrometri.app.model.RENKLER
import com.psikrometri.app.psy.*

fun Kapasite.metin(b: BirimSecimi): String {
    val g = b.birim(Buyukluk.GUC); val gd = if (g.carpan > 1) 0 else 2
    return listOfNotNull(
        toplam?.let { "Toplam ${g.yazB(it, gd)}" }, duyulur?.let { "Duyulur ${g.yazB(it, gd)}" }, gizli?.let { "Gizli ${g.yazB(it, gd)}" },
        shf?.let { "SHF %${it.yaz(0)}" }, isitici?.let { "Isıtıcı ${g.yazB(it, gd)}" }, nemKgH?.let { "Nem ${it.yaz(2)} kg/h" },
        cikisT?.let { "Üfleme ${b.birim(Buyukluk.SICAKLIK).yazB(it, 1)}" }, bypass?.let { "BF %${it.yaz(1)}" }).joinToString(" · ")
}

@Composable
fun IslemlerSekmesi(vm: AnaVM) {
    var secIndex by remember { mutableStateOf<Int?>(null) }
    val duzenlenen = secIndex?.let { vm.proje.islemler.getOrNull(it) }
    var tur by remember(duzenlenen) { mutableStateOf(duzenlenen?.tur ?: IslemTuru.KARISIM) }
    val noktaAdlari = vm.hesap.hava.keys.toList()
    val ayniTur = duzenlenen != null && tur == duzenlenen.tur
    var girdiler by remember(duzenlenen, tur, noktaAdlari) { mutableStateOf(duzenlenen?.takeIf { ayniTur }?.girdiler ?: List(tur.girdiSayisi) { noktaAdlari.getOrElse(it) { "" } }) }
    val b = vm.ayarlar.birimler; val gb = b.birim(Buyukluk.GUC)
    val varsayilan = mapOf(Parametre.KW to 5.0, Parametre.SHF to 75.0, Parametre.KGH to 5.0, Parametre.BF to 10.0, Parametre.BUHAR to 1.0, Parametre.ARA to 1.0)
    // kapasite parametresi seçili güç biriminde girilir, SI (kW) saklanır
    var param by remember(duzenlenen, tur, gb) { mutableStateOf((duzenlenen?.takeIf { ayniTur }?.param ?: tur.parametreler.map { varsayilan.getValue(it) })
        .mapIndexed { k, v -> (if (tur.parametreler[k] == Parametre.KW) gb.goster(v) else v).kisa() }) }
    var renk by remember(duzenlenen) { mutableStateOf(duzenlenen?.renk ?: RENKLER[0]) }

    fun islem(): Islem? {
        val p = param.mapIndexed { k, s -> s.sayi()?.let { if (tur.parametreler[k] == Parametre.KW) gb.siye(it) else it } ?: return null }
        if (girdiler.any { it !in noktaAdlari }) return null
        // çıktı adları mevcut nokta adlarıyla çakışmasın
        val kullanilan = vm.proje.noktalar.map { it.ad }.toSet()
        var n = vm.proje.yeniPPAdi().removePrefix("PP").toInt()
        val cikti = duzenlenen?.takeIf { ayniTur }?.ciktilar ?: List(tur.ciktiSayisi) { while ("PP$n" in kullanilan) n++; "PP${n++}" }
        return Islem(tur, girdiler, cikti, p, renk)
    }
    val onizleme = islem()?.let { i -> runCatching { islemHesapla(i.tur, i.girdiler.map { vm.hesap.hava.getValue(it) }, i.param) } }

    Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SecimKutusu(IslemTuru.entries, tur, { it.etiket }, { tur = it }, Modifier.fillMaxWidth(), "İşlem")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            girdiler.forEachIndexed { i, g ->
                SecimKutusu(noktaAdlari, g, { it }, { yeni -> girdiler = girdiler.toMutableList().also { l -> l[i] = yeni } }, Modifier.weight(1f), if (i == 0) "Başlangıç" else "İkinci nokta")
            }
        }
        tur.parametreler.forEachIndexed { i, pr ->
            when (pr) {
                Parametre.BUHAR -> Row { listOf("1" to "Buharlı", "0" to "Sulu").forEach { (v, e) ->
                    Row(Modifier.clickable { param = param.toMutableList().also { it[i] = v } }) { RadioButton(param[i] == v, { param = param.toMutableList().also { it[i] = v } }); Text(e, Modifier.padding(top = 12.dp)) } } }
                Parametre.ARA -> Row(Modifier.clickable { param = param.toMutableList().also { it[i] = if (param[i] == "1") "0" else "1" } }) {
                    Checkbox(param[i] == "1", { c -> param = param.toMutableList().also { it[i] = if (c) "1" else "0" } }); Text("Ara çizgileri göster", Modifier.padding(top = 12.dp)) }
                else -> SayiAlani(param[i], { v -> param = param.toMutableList().also { it[i] = v } }, pr.etiket, if (pr == Parametre.KW) gb.etiket else pr.birim, Modifier.fillMaxWidth())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RENKLER.forEach { r -> Box(Modifier.size(28.dp).clip(CircleShape).background(Color(r)).border(if (r == renk) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape).clickable { renk = r }) }
        }
        onizleme?.takeIf { o -> o.isFailure || o.getOrNull()?.kapasite?.metin(b)?.isNotEmpty() == true }?.let { o -> Text(o.fold({ it.kapasite.metin(b) }, { it.message ?: "" }), color = if (o.isFailure) MaterialTheme.colorScheme.error else Color.Unspecified) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ islem()?.let { vm.proje = vm.proje.copy(islemler = vm.proje.islemler + it) } }, enabled = duzenlenen == null && islem() != null) { Text("Ekle") }
            Button({ islem()?.let { yeni -> vm.proje = vm.proje.copy(islemler = vm.proje.islemler.mapIndexed { k, i -> if (k == secIndex) yeni else i }) } }, enabled = duzenlenen != null && islem() != null) { Text("Güncelle") }
            OutlinedButton({ vm.proje = vm.proje.copy(islemler = vm.proje.islemler.filterIndexed { k, _ -> k != secIndex }); secIndex = null }, enabled = duzenlenen != null) { Text("Sil") }
        }
        HorizontalDivider()
        vm.proje.islemler.forEachIndexed { k, i ->
            val r = vm.hesap.sonuclar.getOrNull(k)
            Card(Modifier.fillMaxWidth().clickable { secIndex = if (secIndex == k) null else k },
                colors = CardDefaults.cardColors(containerColor = if (secIndex == k) Color(0xFFBBDEFB) else Color(0xFFF5F5F5))) {
                Column(Modifier.padding(8.dp)) {
                    Row { Box(Modifier.size(14.dp).background(Color(i.renk))); Text("  ${i.tur.etiket}: ${(i.girdiler + i.ciktilar).joinToString(", ")}", color = Color.Black) }
                    r?.let { Text(it.fold({ s -> s.kapasite.metin(b) }, { e -> "⚠ ${e.message}" }), color = if (it.isFailure) MaterialTheme.colorScheme.error else Color.Black) }
                }
            }
        }
    }
}

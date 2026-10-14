package com.psikrometri.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.model.Nokta
import com.psikrometri.app.psy.*

@Composable
fun NoktalarSekmesi(vm: AnaVM, taslak: Durum?, taslakKullanildi: () -> Unit) {
    val b = vm.ayarlar.birimler
    val duzenlenen = vm.proje.noktalar.firstOrNull { it.ad == vm.secili }
    var ad by remember(duzenlenen, vm.proje.noktalar.size) { mutableStateOf(duzenlenen?.ad ?: "P${vm.proje.noktalar.size + 1}") }
    var o1 by remember(duzenlenen, vm.proje.noktalar.size, b) { mutableStateOf(duzenlenen?.o1 ?: Ozellik.KURU) }
    var o2 by remember(duzenlenen, vm.proje.noktalar.size, b) { mutableStateOf(duzenlenen?.o2 ?: Ozellik.BAGIL) }
    // birim değişince alanlar yeni birimde yeniden yazılır (eski metin yeni birimle yorumlanmasın)
    // varsayılan: 25 °C %50; özellik değişince alan mevcut noktanın o özelliğiyle dolar (nokta yerinde kalır)
    val ref = Durum(25.0, nemOrani(0.5 * pws(25.0), vm.proje.basinc), vm.proje.basinc)
    fun yazO(o: Ozellik, d: Durum) = b.ozellikBirimi(o).goster(d.deger(o)).yaz(when (o) { Ozellik.MUTLAK -> b.nemOndalik(); Ozellik.HACIM, Ozellik.YOGUNLUK -> 3; else -> 1 })
    var v1 by remember(duzenlenen, vm.proje.noktalar.size, b) { mutableStateOf(duzenlenen?.let { b.ozellikBirimi(it.o1).goster(it.v1).kisa() } ?: yazO(o1, ref)) }
    var v2 by remember(duzenlenen, vm.proje.noktalar.size, b) { mutableStateOf(duzenlenen?.let { b.ozellikBirimi(it.o2).goster(it.v2).kisa() } ?: yazO(o2, ref)) }
    var debi by remember(duzenlenen, vm.proje.noktalar.size, b) { mutableStateOf(b.birim(Buyukluk.DEBI).goster(duzenlenen?.debi ?: 150.0).yaz(1)) }
    LaunchedEffect(taslak) { taslak?.let { o1 = Ozellik.KURU; o2 = Ozellik.MUTLAK; v1 = b.ozellikBirimi(Ozellik.KURU).goster(it.t).yaz(1); v2 = b.ozellikBirimi(Ozellik.MUTLAK).goster(it.Wr).yaz(b.nemOndalik()); taslakKullanildi() } }

    val si1 = v1.sayi()?.let { b.ozellikBirimi(o1).siye(it) }; val si2 = v2.sayi()?.let { b.ozellikBirimi(o2).siye(it) }
    val debiSi = debi.sayi()?.let { b.birim(Buyukluk.DEBI).siye(it) }
    val sonuc = if (si1 != null && si2 != null) runCatching { coz(o1, si1, o2, si2, vm.proje.basinc) } else null
    val adCakisma = ad.trim().let { a -> (a != duzenlenen?.ad && vm.proje.noktalar.any { it.ad == a }) || vm.proje.islemler.any { a in it.ciktilar } }
    val hata = sonuc?.exceptionOrNull()?.message ?: if (debiSi != null && debiSi <= 0) "Debi 0'dan büyük olmalı" else if (adCakisma) "Bu ad başka bir nokta ya da işlem çıktısında kullanılıyor" else null
    SideEffect { vm.onizleme = sonuc?.getOrNull() }
    DisposableEffect(Unit) { onDispose { vm.onizleme = null } }
    val gecerli = sonuc?.isSuccess == true && debiSi != null && debiSi > 0 && ad.isNotBlank() && !adCakisma
    fun nokta() = Nokta(ad.trim(), o1, si1!!, o2, si2!!, debiSi!!, duzenlenen?.renk ?: com.psikrometri.app.model.MAVI)

    Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(ad, { ad = it }, label = { Text("Nokta adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        for ((o, setO, v, setV) in listOf(Quad(o1, { x: Ozellik -> v1 = yazO(x, sonuc?.getOrNull() ?: ref); o1 = x }, v1, { x: String -> v1 = x }), Quad(o2, { x: Ozellik -> v2 = yazO(x, sonuc?.getOrNull() ?: ref); o2 = x }, v2, { x: String -> v2 = x })))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SecimKutusu(Ozellik.entries, o, { it.etiket }, setO, Modifier.weight(1.2f))
                SayiAlani(v, setV, "Değer", b.ozellikBirimi(o).etiket, Modifier.weight(1f))
            }
        SayiAlani(debi, { debi = it }, "Debi", b.birim(Buyukluk.DEBI).etiket, Modifier.fillMaxWidth())
        hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ vm.proje = vm.proje.copy(noktalar = vm.proje.noktalar + nokta()); vm.secili = null }, enabled = gecerli && duzenlenen == null) { Text("Ekle") }
            Button({
                val eski = duzenlenen!!.ad; val yeni = nokta()
                // yeniden adlandırmada işlem girdileri de güncellenir
                vm.proje = vm.proje.copy(noktalar = vm.proje.noktalar.map { if (it.ad == eski) yeni else it },
                    islemler = vm.proje.islemler.map { i -> i.copy(girdiler = i.girdiler.map { if (it == eski) yeni.ad else it }) })
                vm.secili = yeni.ad
            }, enabled = gecerli && duzenlenen != null) { Text("Güncelle") }
            OutlinedButton({ vm.proje = vm.proje.copy(noktalar = vm.proje.noktalar.filter { it.ad != duzenlenen!!.ad }); vm.secili = null }, enabled = duzenlenen != null) { Text("Sil") }
        }
        HorizontalDivider()
        for (n in vm.proje.noktalar) {
            Card(Modifier.fillMaxWidth().clickable { vm.secili = if (vm.secili == n.ad) null else n.ad },
                colors = CardDefaults.cardColors(containerColor = if (vm.secili == n.ad) Color(0xFFBBDEFB) else Color(0xFFF5F5F5))) {
                Text("${n.ad}  ${n.o1.etiket} ${b.ozellikBirimi(n.o1).goster(n.v1).yaz(if (n.o1 == Ozellik.MUTLAK) b.nemOndalik() else 1)} ${b.ozellikBirimi(n.o1).etiket} · " +
                    "${n.o2.etiket} ${b.ozellikBirimi(n.o2).goster(n.v2).yaz(if (n.o2 == Ozellik.MUTLAK) b.nemOndalik() else 1)} ${b.ozellikBirimi(n.o2).etiket}" +
                    (vm.hesap.noktaHatalari[n.ad]?.let { "  ⚠ $it" } ?: ""), Modifier.padding(8.dp), color = Color.Black)
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

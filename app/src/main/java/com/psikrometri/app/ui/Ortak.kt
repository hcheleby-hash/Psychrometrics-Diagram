package com.psikrometri.app.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.psikrometri.app.model.Birim
import com.psikrometri.app.model.ondalikAyrac

fun String.sayi(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
fun Double.yaz(ondalik: Int): String = if (isNaN()) "–" else "%.${ondalik}f".format(java.util.Locale.ROOT, this).replace('.', ondalikAyrac)
/** Giriş alanına geri yazmak için: 75.0 -> "75", 0.0 -> "0", 0.0123 -> "0,0123". */
fun Birim.yazB(si: Double, ondalik: Int) = "${goster(si).yaz(ondalik)} $etiket"
/** Fark gösterimi: ofset uygulanmaz (Δ°F = 1,8·Δ°C). */
fun Birim.farkYaz(dsi: Double, ondalik: Int) = "${(dsi * carpan).yaz(ondalik)} $etiket"
fun Double.kisa(): String = if (this == Math.rint(this)) toLong().toString() else "%.4f".format(java.util.Locale.ROOT, this).trimEnd('0').replace('.', ondalikAyrac)

@Composable
fun SayiAlani(deger: String, onDeger: (String) -> Unit, etiket: String, birim: String, modifier: Modifier = Modifier, hata: String? = null) {
    OutlinedTextField(deger, onDeger, modifier, label = { Text(etiket) }, suffix = { Text(birim) }, singleLine = true,
        isError = hata != null || (deger.isNotBlank() && deger.sayi() == null),
        supportingText = hata?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SecimKutusu(secenekler: List<T>, secili: T, etiket: (T) -> String, onSec: (T) -> Unit, modifier: Modifier = Modifier, baslik: String = "") {
    var acik by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(acik, { acik = it }, modifier) {
        OutlinedTextField(etiket(secili), {}, readOnly = true, label = { Text(baslik) }, singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(acik) }, modifier = Modifier.menuAnchor())
        ExposedDropdownMenu(acik, { acik = false }) {
            secenekler.forEach { s -> DropdownMenuItem(text = { Text(etiket(s)) }, onClick = { onSec(s); acik = false }) }
        }
    }
}

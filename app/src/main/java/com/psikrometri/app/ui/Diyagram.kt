package com.psikrometri.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.psikrometri.app.psy.Durum
import com.psikrometri.app.psy.doymaW

@Composable
fun Diyagram(vm: AnaVM, modifier: Modifier, noktaEkle: (Durum) -> Unit) {
    var olcek by remember { mutableFloatStateOf(1f) }
    var kayma by remember { mutableStateOf(Offset.Zero) }
    var balon by remember { mutableStateOf<Pair<Offset, Durum>?>(null) }
    var boyut by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    fun eksen() = Eksen(vm.ayarlar.tMin, vm.ayarlar.tMax, vm.ayarlar.wMax, 24f, 8f, boyut.width - 106f, boyut.height - 44f)
    fun ekrandanVeriye(o: Offset) = (o - kayma) / olcek
    val yog = LocalDensity.current.density
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().clipToBounds()
            .onSizeChanged { boyut = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) { detectTransformGestures { merkez, kay, zoom, _ ->
                val yeni = (olcek * zoom).coerceIn(1f, 8f)
                val k = merkez - (merkez - kayma) * (yeni / olcek) + kay
                kayma = Offset(k.x.coerceIn(boyut.width * (1 - yeni), 0f), k.y.coerceIn(boyut.height * (1 - yeni), 0f)); olcek = yeni } }
            .pointerInput(Unit) { detectTapGestures(
                onDoubleTap = { olcek = 1f; kayma = Offset.Zero; balon = null },
                onTap = { o -> balon = null
                    val e = eksen(); val v = ekrandanVeriye(o)
                    vm.secili = vm.hesap.hava.minByOrNull { (_, h) -> (Offset(e.x(h.d.t), e.y(h.d.W)) - v).getDistance() }
                        ?.takeIf { (_, h) -> (Offset(e.x(h.d.t), e.y(h.d.W)) - v).getDistance() < 40 / olcek }?.key },
                onLongPress = { o ->
                    val e = eksen(); val v = ekrandanVeriye(o); val t = e.t(v.x); val w = e.w(v.y)
                    if (w >= 0 && w <= doymaW(t, vm.proje.basinc)) balon = o to Durum(t, w, vm.proje.basinc) }) }
        ) {
            withTransform({ translate(kayma.x, kayma.y); scale(olcek, olcek, Offset.Zero) }) {
                drawContext.canvas.nativeCanvas.let { cizDiyagram(it, eksen(), vm.proje.basinc, vm.egriler, vm.hesap, vm.proje, vm.secili, vm.onizleme, vm.ayarlar.birimler) }
            }
        }
        balon?.let { (o, d) ->
            Card(Modifier.offset { IntOffset(o.x.coerceIn(0f, (boyut.width - 220 * yog).coerceAtLeast(0f)).toInt(), o.y.coerceIn(0f, (boyut.height - 130 * yog).coerceAtLeast(0f)).toInt()) }.widthIn(max = 220.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text("t ${d.t.yaz(1)} °C · W ${d.Wr.yaz(4)}"); Text("RH %${(d.rh * 100).yaz(1)} · h ${d.h.yaz(1)}")
                    TextButton({ noktaEkle(d); balon = null }) { Text("Nokta ekle") }
                }
            }
        }
    }
}

# Psikrometri Uygulaması — Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Daikin Psychrometric Diagram Viewer 4.1.0'ın Android karşılığı: 7 özellikten ikisiyle nokta, 15 işlem, diyagram, rakım, kaydet/aç, PDF rapor, birimler — sonuçlar Daikin ile aynı.

**Architecture:** Saf Kotlin hesap çekirdeği (`psy/`) Daikin'den alınan referans tablosuyla JVM testlerinde doğrulanır. Tek `Proje` durumu (noktalar + işlemler + rakım) her değişiklikte baştan hesaplanır; Compose UI ve PDF aynı `android.graphics.Canvas` çizim fonksiyonunu kullanır.

**Tech Stack:** Kotlin 2.0.20, AGP 8.6.1, Gradle 8.9, Jetpack Compose (BOM 2024.09.02) + Material3, `org.json`, `android.graphics.pdf.PdfDocument`, SharedPreferences, JUnit 4.

## Global Constraints

- Proje kökü: `C:\Users\User\Desktop\Claude Projeler\Psikrometri`; paket `com.psikrometri.app`; minSdk 24, target/compileSdk 35, JVM 17.
- Ek kütüphane yok: JSON `org.json` (test için `testImplementation("org.json:json:20240303")`), PDF `PdfDocument`, ayarlar SharedPreferences (spec'teki DataStore yerine — bağımlılık eklememek için; spec güncellendi).
- Daikin adı/logosu uygulamada geçmez. Uygulama adı "Psikrometri".
- Formül sabitleri `docs/superpowers/specs/2026-10-13-psikrometri-design.md` tablosundan aynen; işlem davranışları `docs/daikin-bulgular.md`.
- Test JVM'i: `-Duser.language=en -Duser.country=US` (tr-TR tuzağı).
- Referans toleransı: Daikin'in gösterdiği son hanenin yarısı (t 0,05; W 0,00005; RH 0,05; h 0,05; ρ,v 0,0005; kW ve kg/h 0,005).
- Gradle çıktısında "BUILD SUCCESSFUL" yazısı tr-TR'de bozulur; başarı ölçütü **exit code 0**.
- Daikin'i süren adımlarda kullanıcı bilgisayarı kullanmaz; adımdan önce kullanıcıya haber verilir.
- Bir işlemin Daikin davranışı referansla çözülemezse varsayım yapılmaz, kullanıcıya Daikin sayılarıyla sorulur.

## Dosya yapısı

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew(.bat), gradle/wrapper/*   (NeyinVar'dan)
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/xml/dosya_yollari.xml                 FileProvider (PDF paylaşımı)
app/src/main/java/com/psikrometri/app/
  MainActivity.kt                                       setContent { AnaEkran() }
  psy/Psikro.kt          Durum + durum fonksiyonları + kök bulucu
  psy/Cozucu.kt          Ozellik (7) + coz(): 21 ikili
  psy/Islemler.kt        IslemTuru (15) + islemHesapla()
  psy/ShfDogrusu.kt      SHF işlemi / Bypass factor doğrusu (Task 6)
  model/Proje.kt         Nokta, Islem, Proje, hesapla(), JSON
  model/Birimler.kt      Birim, BirimSecimi
  ui/Ayarlar.kt          Ayarlar (SharedPreferences)
  ui/DiyagramCizer.kt    Eksen + eğriler + android.graphics.Canvas çizimi
  ui/Diyagram.kt         Compose sarmalayıcı: zoom/pan/dokunma/balon
  ui/AnaVM.kt            ViewModel: proje, seçim, önizleme, ayarlar
  ui/AnaEkran.kt         düzen + üst çubuk + menü + kaydet/aç/rapor
  ui/NoktalarSekmesi.kt
  ui/IslemlerSekmesi.kt
  ui/VeriSekmesi.kt
  ui/Ortak.kt            SayiAlani, SecimKutusu, biçimlendirme
  rapor/PdfRapor.kt
app/src/test/java/com/psikrometri/app/
  psy/PsikroTest.kt, psy/CozucuTest.kt, psy/ReferansTest.kt, model/ProjeTest.kt, model/BirimlerTest.kt, ui/EksenTest.kt
app/src/test/resources/daikin_referans.json            Task 3 üretir
tools/daikin/referans_senaryo.py, referans_al.ps1, referans_json.py   Task 3
```

---

### Task 1: Proje iskeleti

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/dosya_yollari.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/com/psikrometri/app/MainActivity.kt`
- Copy: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `local.properties` ← `..\NeyinVar\`

**Interfaces:**
- Produces: derlenen boş uygulama; `./gradlew :app:testDebugUnitTest` ve `:app:assembleDebug` çalışır.

- [ ] **Step 1: Wrapper ve SDK yolunu kopyala**

```bash
cd "C:/Users/User/Desktop/Claude Projeler/Psikrometri"
cp ../NeyinVar/gradlew ../NeyinVar/gradlew.bat ../NeyinVar/local.properties .
mkdir -p gradle/wrapper && cp ../NeyinVar/gradle/wrapper/* gradle/wrapper/
```

- [ ] **Step 2: Gradle dosyalarını yaz**

`settings.gradle.kts`:
```kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Psikrometri"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
```

`gradle.properties`:
```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.psikrometri.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.psikrometri.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.all { it.jvmArgs("-Duser.language=en", "-Duser.country=US") } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
```

- [ ] **Step 3: Manifest, kaynaklar, MainActivity**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="@string/app_name" android:icon="@android:drawable/ic_menu_compass"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <provider android:name="androidx.core.content.FileProvider"
            android:authorities="com.psikrometri.app.dosya" android:exported="false" android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/dosya_yollari" />
        </provider>
    </application>
</manifest>
```

`app/src/main/res/xml/dosya_yollari.xml`:
```xml
<paths><cache-path name="rapor" path="rapor/" /></paths>
```

`app/src/main/res/values/strings.xml`:
```xml
<resources><string name="app_name">Psikrometri</string></resources>
```

`app/src/main/java/com/psikrometri/app/MainActivity.kt` (Task 12'de `AnaEkran()` ile değişir):
```kotlin
package com.psikrometri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Psikrometri") }
    }
}
```

- [ ] **Step 4: Derle**

Run: `./gradlew :app:assembleDebug`
Expected: exit code 0.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "build: Android proje iskeleti"
```

---

### Task 2: Durum hesapları (`psy/Psikro.kt`)

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/psy/Psikro.kt`
- Test: `app/src/test/java/com/psikrometri/app/psy/PsikroTest.kt`

**Interfaces:**
- Produces:
  - `const val P_DENIZ = 101325.0`, `EPS`, `RDA`
  - `fun pws(t: Double): Double` [Pa]; `fun nemOrani(pw: Double, p: Double): Double`; `fun doymaW(t: Double, p: Double): Double`
  - `fun entalpi(t: Double, W: Double): Double` [kJ/kg]; `fun tEntalpidan(h: Double, W: Double): Double`; `fun cpm(W: Double): Double = 1.006 + 1.84 * W`
  - `fun yasW(t: Double, tw: Double, p: Double): Double`; `fun yasTermometre(t: Double, W: Double, p: Double): Double`; `fun ciyNoktasi(W: Double, p: Double): Double`
  - `fun ozgulHacim(t: Double, W: Double, p: Double): Double`; `fun yogunluk(t: Double, W: Double, p: Double): Double`
  - `fun basincRakimdan(z: Double): Double`; `fun rakimBasinctan(p: Double): Double`
  - `fun kokBul(a: Double, b: Double, adim: Double, f: (Double) -> Double): Double?` — a'dan b'ye (a>b de olabilir) adım adım tarar, ilk işaret değişiminde ikiye böler; yoksa null.
  - `data class Durum(val t: Double, val W: Double, val p: Double = P_DENIZ)` — `pw, rh (0..1), h, twb, td, v, rho` özellikleri.

- [ ] **Step 1: Failing test**

```kotlin
package com.psikrometri.app.psy

import org.junit.Assert.assertEquals
import org.junit.Test

class PsikroTest {
    private fun yakin(beklenen: Double, gercek: Double, ondalik: Int) =
        assertEquals(beklenen, gercek, 0.5 * Math.pow(10.0, -ondalik.toDouble()) + 1e-9)

    @Test fun doymaBasinciAshraeTablosu() {
        assertEquals(2339.0, pws(20.0), 2.0)
        assertEquals(259.9, pws(-10.0), 0.5)
    }

    @Test fun bagilNemDaikinPppOnbellegiyle15Hane() {  // tools/daikin/veri/test1.ppp
        assertEquals(44.9441962635556, Durum(27.0, 0.010).rh * 100, 1e-8)
        assertEquals(75.4471482056509, Durum(15.0, 0.008).rh * 100, 1e-8)
        assertEquals(42.3978002755472, Durum(35.0, 0.015).rh * 100, 1e-8)
    }

    @Test fun raporDegerleri() {  // rapor1.txt / rapor3.txt: (t, W) -> twb, td, h, rho, v
        val satirlar = listOf(
            doubleArrayOf(27.0, 0.010, 18.6, 14.1, 52.7, 1.169, 0.864),
            doubleArrayOf(15.0, 0.008, 12.5, 10.7, 35.3, 1.219, 0.827),
            doubleArrayOf(35.0, 0.015, 24.5, 20.3, 73.7, 1.135, 0.894),
            doubleArrayOf(5.0, 0.003, 1.8, -2.7, 12.6, 1.267, Double.NaN),
            doubleArrayOf(22.0, 0.006, 13.4, 6.5, 37.4, 1.192, Double.NaN),
        )
        for (r in satirlar) {
            val d = Durum(r[0], r[1])
            yakin(r[2], d.twb, 1); yakin(r[3], d.td, 1); yakin(r[4], d.h, 1); yakin(r[5], d.rho, 3)
            if (!r[6].isNaN()) yakin(r[6], d.v, 3)
        }
    }

    @Test fun doymusHavadaUcSicaklikEsit() {
        val d = Durum(20.0, doymaW(20.0, P_DENIZ))
        assertEquals(20.0, d.twb, 1e-3); assertEquals(20.0, d.td, 0.05)
    }

    @Test fun rakimBasincGidisDonus() {
        assertEquals(P_DENIZ, basincRakimdan(0.0), 1e-6)
        assertEquals(1500.0, rakimBasinctan(basincRakimdan(1500.0)), 1e-6)
    }

    @Test fun kokBulAzalanYonde() {
        assertEquals(2.0, kokBul(10.0, -10.0, 0.5) { it - 2.0 }!!, 1e-9)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.psy.PsikroTest"`
Expected: FAIL — unresolved reference `pws`, `Durum`.

- [ ] **Step 3: Implement**

```kotlin
package com.psikrometri.app.psy

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

// Sabitler Daikin Psychrometrics 4.1.0 exe'sinden (ASHRAE Fundamentals Bl. 1, buhar cp = 1.84).
const val P_DENIZ = 101325.0
const val EPS = 0.6219779385
const val RDA = 287.0551882

fun pws(t: Double): Double {
    val T = t + 273.15
    return if (t >= 0) exp(-5800.2206 / T + 1.3914993 - 0.048640239 * T + 4.1764768e-5 * T * T
        - 1.4452093e-8 * T * T * T + 6.5459673 * ln(T))
    else exp(-5674.5359 / T + 6.3925247 - 9.677843e-3 * T + 6.2215701e-7 * T * T
        + 2.0747825e-9 * T * T * T - 9.484024e-13 * T * T * T * T + 4.1635019 * ln(T))
}

fun nemOrani(pw: Double, p: Double) = EPS * pw / (p - pw)
fun buharBasinci(W: Double, p: Double) = W * p / (EPS + W)
fun doymaW(t: Double, p: Double) = nemOrani(pws(t), p)
fun cpm(W: Double) = 1.006 + 1.84 * W
fun entalpi(t: Double, W: Double) = 1.006 * t + W * (2501 + 1.84 * t)
fun tEntalpidan(h: Double, W: Double) = (h - 2501 * W) / cpm(W)
fun ozgulHacim(t: Double, W: Double, p: Double) = RDA * (t + 273.15) * (1 + 1.607774067 * W) / p
fun yogunluk(t: Double, W: Double, p: Double) = (1 + W) / ozgulHacim(t, W, p)

/** Yaş termometre tw'deki havanın nem oranı (ASHRAE eş. 35/37, cp buhar 1.84). */
fun yasW(t: Double, tw: Double, p: Double): Double {
    val ws = doymaW(tw, p)
    return if (tw >= 0) ((2501 - 2.346 * tw) * ws - 1.006 * (t - tw)) / (2501 + 1.84 * t - 4.186 * tw)
    else ((2834.4 - 0.26 * tw) * ws - 1.006 * (t - tw)) / (2834.4 + 1.84 * t - 2.1 * tw)
}

fun yasTermometre(t: Double, W: Double, p: Double): Double =
    kokBul(t, -100.0, 0.5) { tw -> yasW(t, tw, p) - W } ?: Double.NaN

/** ASHRAE eş. 39/40 (Peppers). */
fun ciyNoktasi(W: Double, p: Double): Double {
    if (W <= 0) return Double.NaN
    val pw = buharBasinci(W, p) / 1000
    val a = ln(pw)
    val td = 6.54 + 14.526 * a + 0.7389 * a * a + 0.09486 * a * a * a + 0.4569 * pw.pow(0.1984)
    return if (td >= 0) td else 6.09 + 12.608 * a + 0.4959 * a * a
}

// ponytail: standart atmosfer; Daikin'in rakım formülü Task 3 referansıyla doğrulanır.
fun basincRakimdan(z: Double) = P_DENIZ * (1 - 2.25577e-5 * z).pow(5.2559)
fun rakimBasinctan(p: Double) = (1 - (p / P_DENIZ).pow(1 / 5.2559)) / 2.25577e-5

fun kokBul(a: Double, b: Double, adim: Double, f: (Double) -> Double): Double? {
    val yon = if (b >= a) 1.0 else -1.0
    var x = a
    var fx = f(x)
    while ((b - x) * yon > 0) {
        val y = if (abs(b - x) < adim) b else x + yon * adim
        val fy = f(y)
        if (fx == 0.0) return x
        if (!fx.isNaN() && !fy.isNaN() && fx * fy <= 0) {
            var lo = x; var hi = y; var flo = fx
            repeat(80) {
                val m = (lo + hi) / 2; val fm = f(m)
                if (flo * fm > 0) { lo = m; flo = fm } else hi = m
            }
            return (lo + hi) / 2
        }
        x = y; fx = fy
    }
    return null
}

data class Durum(val t: Double, val W: Double, val p: Double = P_DENIZ) {
    val pw get() = buharBasinci(W, p)
    val rh get() = pw / pws(t)
    val h get() = entalpi(t, W)
    val twb get() = yasTermometre(t, W, p)
    val td get() = ciyNoktasi(W, p)
    val v get() = ozgulHacim(t, W, p)
    val rho get() = yogunluk(t, W, p)
}
```

- [ ] **Step 4: Run, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.psy.PsikroTest"`
Expected: PASS (6 test).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(psy): Daikin formülleriyle durum hesapları"
```

---

### Task 3: Daikin referans tablosu

Kullanıcıya haber ver: "Daikin'i ~20 dk süreceğim, fare/klavyeye dokunma."

**Files:**
- Create: `tools/daikin/referans_senaryo.py`, `tools/daikin/referans_al.ps1`, `tools/daikin/referans_json.py`
- Create (üretilen): `tools/daikin/veri/ref_*.ppp`, `ref_*.rtf`, `ref_*.txt`, `app/src/test/resources/daikin_referans.json`
- Modify: `docs/daikin-bulgular.md` (rakım satırı, önbellek bulgusu)

**Interfaces:**
- Consumes: `tools/daikin/ppp_yaz.py` (`nokta`, `yaz`), `tools/daikin/dk.ps1` (`Ac`, `Rapor`, `DialogKapat`), `tools/daikin/rtf_metin.py` (`rtf_metin`).
- Produces: `daikin_referans.json`:
```json
{"senaryolar": [{
  "ad": "ref_durum", "basinc": 101325.0,
  "noktalar": [{"ad": "P1", "o1": 0, "v1": 27, "o2": 1, "v2": 0.01, "debi": 150}],
  "islemler": [{"kod": 9, "girdi": ["P1"], "cikti": ["PP1", "PP2"], "param": [0, 75, 1]}],
  "beklenen_noktalar": {"P1": {"t": 27.0, "twb": 18.6, "td": 14.1, "rh": 44.9, "W": 0.01, "h": 52.7, "rho": 1.169, "v": 0.864, "p": 101325.0, "debi": 150.0}},
  "beklenen_islemler": {"PP1": {"Total": 4.10, "Sensible": 3.08, "Latent": 1.03}},
  "hatalar": ["The required cooling action ..."]
}]}
```
  `beklenen_islemler` anahtarı işlemin ilk çıktı adıdır (çıktısı olmayanlar — Connect, Show point, Condensate — için `"<kod>:<girdi1>:<girdi2>"`).

- [ ] **Step 1: Önbellek ve rakım formatını öğren**

Daikin'de (dk.ps1 ile) Altitude panelini aç (`RealClick 400 35`), rakımı 1500 yap (alanın koordinatını ekran görüntüsünden bul, `Yaz x y '1500'`), projeyi `tools/daikin/veri/rakim.ppp` olarak kaydet. Dosyayı oku:
```bash
iconv -f UTF-16 -t UTF-8 tools/daikin/veri/rakim.ppp
```
Rakım satırının biçimini `docs/daikin-bulgular.md` → "Dosya formatları"na ekle. Ayrıca önbellek alanlarını (8. ve 9. alan) bilerek yanlış (`0 0`) yazdığın tek noktalı bir `.ppp` aç, raporda değerlerin doğru çıktığını kontrol et; sonucu bulgulara yaz. Önbellek kullanılıyorsa `ppp_yaz.nokta` zaten doğru değer yazıyor, ek iş yok.

- [ ] **Step 2: Senaryo üretici**

`tools/daikin/referans_senaryo.py`:
```python
"""Referans senaryolarının .ppp dosyalarını üretir. Kullanım: py -3 referans_senaryo.py <çıktı klasörü>"""
import itertools, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from ppp_yaz import nokta, yaz, d, f

R = '2 16711680'
P = 101325.0

def ozellikler(t, W):
    pw = W * P / (d.EPS + W)
    v = 287.0551882 * (t + 273.15) * (1 + 1.607774067 * W) / P
    return [t, W, pw / d.pws(t) * 100, d.entalpi(t, W), d.yas_termometre(t, pw / d.pws(t), P), v, (1 + W) / v]

YUVARLA = [1, 4, 1, 1, 1, 3, 3]  # Daikin giriş hassasiyeti varsayımı; Daikin farklı yuvarlarsa raporda görünür

def nokta_ikili(ad, o1, o2, t, W, debi=150):
    oz = ozellikler(t, W)
    v1, v2 = round(oz[o1], YUVARLA[o1]), round(oz[o2], YUVARLA[o2])
    pw = W * P / (d.EPS + W)
    return f'1 {ad} {o1} {f(v1)} {o2} {f(v2)} {debi} {f(t)} {f(pw / d.pws(t) * 100)}'

def durum_senaryosu():
    s, n = [], 0
    for (t, W) in [(27, .010), (5, .003), (-10, .0013), (40, .020)]:
        for o1, o2 in itertools.combinations(range(7), 2):
            n += 1; s.append(nokta_ikili(f'D{n:02d}', o1, o2, t, W))
    return s

def islem_senaryosu():
    s = [nokta('A', 27, .010), nokta('B', 5, .003, debi=300), nokta('C', 35, .015, debi=100),
         nokta('D', -5, .002, debi=200), nokta('E', 13, .008), nokta('F', 16, .0105, debi=100),
         nokta('G', 22, .006), nokta('H', 20, .007, debi=200), nokta('K', 20, .005), nokta('L', 30, .006),
         nokta('M', 20, .012), nokta('N', 30, .012), nokta('S1', 12, rh=100), nokta('S2', 8, rh=100),
         nokta('S3', 20, rh=100), nokta('S4', 10, rh=100), nokta('Q', 30, .008)]
    i = 0
    def pp(k=1):
        nonlocal i
        out = []
        for _ in range(k):
            i += 1; out.append(f'X{i}')
        return out
    s += [f'{R} 2 A B {pp()[0]}', f'{R} 2 C D {pp()[0]}',                                   # Mix air
          f'{R} 3 A E {pp()[0]}', f'{R} 3 C F {pp()[0]}',                                   # Cooling
          f'{R} 4 B G {pp()[0]} 1 1', f'{R} 4 B G {pp()[0]} 0 1',                           # Heating buhar/su
          f'{R} 4 D H {pp()[0]} 1 1', f'{R} 4 D H {pp()[0]} 0 1']
    for p_, kw in (('A', 5), ('D', 10)):                                                    # Heating capacity
        x = pp()[0]; s.append(f'{R} 6 {p_} {x} {x} {kw}')
    for kod, cift in ((7, (('A', 5), ('K', 2))), (8, (('A', 1), ('L', 2))), (11, (('A', 5), ('M', 3)))):
        for p_, kg in cift:                                                                 # Steam / Water / Dehum
            x = pp()[0]; s.append(f'{R} {kod} {p_} {x} {x} {kg}')
    for p_, kw, shf in (('A', 2, 75), ('C', 4, 60), ('N', 3, 90)):                          # Cooling capacity
        a, b = pp(2); s.append(f'{R} 5 {p_} {a} {b} {kw} {shf} 1')
    for p_, shf in (('A', 75), ('A', 90), ('C', 60), ('C', 75), ('C', 90)):                 # Cool to saturation
        a, b = pp(2); s.append(f'{R} 9 {p_} {a} {b} 0 {shf} 1')
    for p_, shf, bf in (('A', 75, 10), ('C', 60, 20)):                                      # Discharge
        a, b = pp(2); s.append(f'{R} 13 {p_} {a} {b} {shf} {bf}')
    s += [f'{R} 10 S1 S2', f'{R} 10 S3 S4']                                                 # Condensate
    for p_, kw, shf in (('A', 2, 75), ('C', 4, 60)):                                        # Bypass factor
        a, b = pp(2); s.append(f'{R} 12 {p_} {a} {b} {kw} {shf}')
    return s

def shf_senaryosu():
    s = [nokta('A', 27, .010), nokta('C', 35, .015), nokta('Q', 30, .008), nokta('Z', 24, .012)]
    n = 0
    for p_ in 'ACQZ':
        for shf in (50, 60, 70, 75, 80, 85, 90, 95):
            n += 1; s.append(f'{R} 14 {p_} H{n} H{n} {shf}')
    return s

if __name__ == '__main__':
    klasor = sys.argv[1]
    yaz(os.path.join(klasor, 'ref_durum.ppp'), durum_senaryosu())
    yaz(os.path.join(klasor, 'ref_islem.ppp'), islem_senaryosu())
    yaz(os.path.join(klasor, 'ref_shf.ppp'), shf_senaryosu())
```

`tools/daikin_psikro.py` içinde `yas_termometre(t, rh, p)` var (rh 0..1); yukarıdaki çağrı ona uygundur.

Run: `py -3 -I tools/daikin/referans_senaryo.py tools/daikin/veri`
Expected: `ref_durum.ppp`, `ref_islem.ppp`, `ref_shf.ppp` oluşur.

- [ ] **Step 3: Daikin'de aç + rapor al**

`tools/daikin/referans_al.ps1`:
```powershell
# Kullanım: powershell -ExecutionPolicy Bypass -File referans_al.ps1 <veri klasörü (Windows yolu)>
param([string]$klasor)
. "$PSScriptRoot\dk.ps1"
foreach ($ppp in Get-ChildItem "$klasor\ref_*.ppp") {
  $ad = $ppp.BaseName
  RealClick 50 179; Start-Sleep -m 1500; DialogKapat | Out-Null; Start-Sleep -m 600
  [System.Windows.Forms.SendKeys]::SendWait($ppp.FullName); Start-Sleep -m 400
  [System.Windows.Forms.SendKeys]::SendWait('{ENTER}'); Start-Sleep -m 3000
  $hatalar = @(); for ($i = 0; $i -lt 30; $i++) { $m = DialogKapat; if (-not $m) { break }; $hatalar += $m; Start-Sleep -m 500 }
  $hatalar | Set-Content -Encoding UTF8 "$klasor\$ad.hatalar.txt"
  Rapor "$klasor\$ad.rtf"
}
```

Run (dosya yolunda boşluk var, tırnakla):
```bash
powershell -NoProfile -ExecutionPolicy Bypass -File tools/daikin/referans_al.ps1 "$(cygpath -w "$PWD/tools/daikin/veri")"
for r in tools/daikin/veri/ref_*.rtf; do py -3 -I tools/daikin/rtf_metin.py "$r" > "${r%.rtf}.txt"; done
```
Expected: üç `ref_*.rtf` + `.txt` + `.hatalar.txt`. `ref_islem.txt` "X. Actions" altında başarılı işlemleri listeler. Rapor yoksa ekran görüntüsü al (`Shot`), takılan kutuyu kapat, tekrar et.

- [ ] **Step 4: JSON üretici**

`tools/daikin/referans_json.py`:
```python
"""ref_*.ppp + ref_*.txt -> app/src/test/resources/daikin_referans.json. Kullanım: py -3 referans_json.py <veri> <json>"""
import glob, json, os, re, sys

def sayi(s): return float(s.replace(',', '.'))

def ppp_oku(yol):
    noktalar, islemler = [], []
    for satir in open(yol, encoding='utf-16').read().split('\n'):
        a = satir.split()
        if not a: continue
        if a[0] == '1':
            noktalar.append({'ad': a[1], 'o1': int(a[2]), 'v1': sayi(a[3]), 'o2': int(a[4]), 'v2': sayi(a[5]), 'debi': sayi(a[6])})
        elif a[0] == '2':
            kod, geri = int(a[2]), a[3:]
            girdi_n = {0: 1, 1: 2, 2: 2, 3: 2, 4: 2, 10: 2}.get(kod, 1)
            cikti_n = {0: 0, 1: 0, 10: 0, 2: 1, 3: 1, 4: 1}.get(kod, 2)
            girdi, cikti, param = geri[:girdi_n], geri[girdi_n:girdi_n + cikti_n], [sayi(x) for x in geri[girdi_n + cikti_n:]]
            islemler.append({'kod': kod, 'girdi': girdi, 'cikti': list(dict.fromkeys(cikti)), 'param': param})
    return noktalar, islemler

ALAN = {'Dry bulb temperature': 't', 'Wet bulb temperature': 'twb', 'Dew point': 'td', 'Relative humidity': 'rh',
        'Absolute humidity': 'W', 'Enthalpy': 'h', 'Density': 'rho', 'Specific volume': 'v', 'Pressure': 'p', 'Airflow': 'debi'}

def rapor_oku(yol):
    metin = open(yol, encoding='utf-8', errors='replace').read()
    nokta_kismi, _, islem_kismi = metin.partition('X. Actions')
    noktalar, adlar = {}, []
    for satir in nokta_kismi.split('\n'):
        hucre = [h.strip() for h in satir.split('|')]
        if len(hucre) < 2: continue
        if hucre[0] in ALAN:
            for k in range(0, len(hucre) - 1, 2):
                if hucre[k] in ALAN and k // 2 < len(adlar):
                    m = re.match(r'-?[\d,]+', hucre[k + 1])
                    if m: noktalar.setdefault(adlar[k // 2], {})[ALAN[hucre[k]]] = sayi(m.group())
        elif all(re.fullmatch(r'[A-Za-z]+\d+', h) or h == '' for h in hucre):
            adlar = [h for h in hucre if h]
    islemler = {}
    for satir in islem_kismi.split('\n'):
        m = re.match(r'\s*([A-Za-z ]+): ([^(|]+)', satir)
        if not m: continue
        isimler = [x.strip() for x in m.group(2).split(',') if x.strip()]
        degerler = {k.strip(): sayi(v) for k, v in re.findall(r'([A-Za-z][A-Za-z ]*?)\s*=\s*(-?[\d,]+)', satir)}
        islemler[(m.group(1).strip(), tuple(isimler))] = degerler
    return noktalar, islemler

AD = {0: 'Show point', 1: 'Connect', 2: 'Mix air', 3: 'Cooling', 4: 'Heating', 5: 'Cooling capacity', 6: 'Heating capacity',
      7: 'Steam humidification', 8: 'Water humidification', 9: 'Cool to saturation', 10: 'Condensate', 11: 'Dehumidification',
      12: 'Bypass factor', 13: 'Discharge', 14: 'Sensible heat factor'}

def senaryo(ppp):
    taban = ppp[:-4]
    noktalar, islemler = ppp_oku(ppp)
    bek_n, bek_i = rapor_oku(taban + '.txt')
    hatalar_yol = taban + '.hatalar.txt'
    hatalar = [h for h in open(hatalar_yol, encoding='utf-8-sig').read().split('\n') if h.strip()] if os.path.exists(hatalar_yol) else []
    beklenen_islemler = {}
    for isl in islemler:
        anahtar_rapor = (AD[isl['kod']], tuple(isl['girdi'] + isl['cikti']))
        if anahtar_rapor in bek_i:
            ad = isl['cikti'][0] if isl['cikti'] else f"{isl['kod']}:" + ':'.join(isl['girdi'])
            beklenen_islemler[ad] = bek_i[anahtar_rapor]
    basinc = next(iter(bek_n.values()))['p']  # raporun "Pressure" satırı; rakımlı senaryoda da doğru basınç
    return {'ad': os.path.basename(taban), 'basinc': basinc, 'noktalar': noktalar, 'islemler': islemler,
            'beklenen_noktalar': bek_n, 'beklenen_islemler': beklenen_islemler, 'hatalar': hatalar}

if __name__ == '__main__':
    veri, cikti = sys.argv[1], sys.argv[2]
    s = [senaryo(p) for p in sorted(glob.glob(os.path.join(veri, '*.ppp'))) if os.path.exists(p[:-4] + '.txt')]
    json.dump({'senaryolar': s}, open(cikti, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(len(s), 'senaryo,', sum(len(x['beklenen_noktalar']) for x in s), 'nokta,', sum(len(x['beklenen_islemler']) for x in s), 'işlem')
```

Run:
```bash
mkdir -p app/src/test/resources
py -3 -I tools/daikin/referans_json.py tools/daikin/veri app/src/test/resources/daikin_referans.json
```
Önce eski raporları eşleşen adlarla kopyala: `cp tools/daikin/veri/rapor1.txt tools/daikin/veri/test1.txt` (aynısı 3 ve 4 için). Ardından Step 1'de öğrenilen rakım satırını kullanarak `ref_rakim.ppp` üret (`ref_islem.ppp`'nin nokta satırları + ilk 8 işlem satırı + 1500 m rakım satırı) ve Step 3'ü onun için tekrarla.
Expected: 7 senaryo (test1, test3, test4, ref_durum, ref_islem, ref_shf, ref_rakim); nokta ≥ 150, işlem ≥ 40. `ref_rakim` senaryosunun `basinc` değerini `PsikroTest`'e ekle:
```kotlin
    @Test fun rakimDaikin() = assertEquals(/* ref_rakim basinc */, basincRakimdan(1500.0), 0.05)
```
Tutmazsa Daikin'in rakım formülünü bul (ör. `101325·exp(−z/8434.5)`, `101325·(1−0.0065z/288.15)^5.255`) ve `basincRakimdan`/`rakimBasinctan`'ı ona göre değiştir.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "test: Daikin referans tablosu (durum, işlem, SHF, rakım)"
```

---

### Task 4: İki özellikten durum çözücü (`psy/Cozucu.kt`)

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/psy/Cozucu.kt`
- Test: `app/src/test/java/com/psikrometri/app/psy/CozucuTest.kt`, `app/src/test/java/com/psikrometri/app/psy/ReferansTest.kt` (durum kısmı)

**Interfaces:**
- Consumes: Task 2 (`Durum`, `kokBul`, `doymaW`, `yasW`, `tEntalpidan`, `ozgulHacim`, `RDA`, `pws`, `nemOrani`)
- Produces:
  - `enum class Ozellik(val daikinKod: Int, val etiket: String)` — `KURU, MUTLAK, BAGIL, ENTALPI, YAS, HACIM, YOGUNLUK` (kod 0..6)
  - `fun Durum.deger(o: Ozellik): Double` (BAGIL yüzde olarak)
  - `class CozumHatasi(mesaj: String) : Exception(mesaj)`
  - `fun coz(o1: Ozellik, v1: Double, o2: Ozellik, v2: Double, p: Double): Durum` (hata → `CozumHatasi`)
  - test yardımcısı `object Referans { fun yukle(): JSONObject; fun yakin(...) }` (ReferansTest.kt içinde, Task 5–6 de kullanır)

- [ ] **Step 1: Failing test**

`CozucuTest.kt`:
```kotlin
package com.psikrometri.app.psy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CozucuTest {
    @Test fun yirmiBirIkiliGidisDonus() {
        for ((t, W) in listOf(27.0 to 0.010, 5.0 to 0.003, -10.0 to 0.0013, 40.0 to 0.020)) {
            val d = Durum(t, W)
            for (i in 0 until 7) for (j in i + 1 until 7) {
                val o1 = Ozellik.entries[i]; val o2 = Ozellik.entries[j]
                val c = coz(o1, d.deger(o1), o2, d.deger(o2), P_DENIZ)
                assertEquals("$o1+$o2 t", t, c.t, 1e-4)
                assertEquals("$o1+$o2 W", W, c.W, 1e-7)
            }
        }
    }

    @Test fun ayniOzellikHata() {
        assertThrows(CozumHatasi::class.java) { coz(Ozellik.KURU, 20.0, Ozellik.KURU, 25.0, P_DENIZ) }
    }

    @Test fun doymaUstuHata() {
        assertThrows(CozumHatasi::class.java) { coz(Ozellik.KURU, 20.0, Ozellik.BAGIL, 120.0, P_DENIZ) }
        assertThrows(CozumHatasi::class.java) { coz(Ozellik.KURU, 20.0, Ozellik.MUTLAK, 0.05, P_DENIZ) }
    }
}
```

`ReferansTest.kt`:
```kotlin
package com.psikrometri.app.psy

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

object Referans {
    fun yukle(): JSONObject = JSONObject(Referans::class.java.getResource("/daikin_referans.json")!!.readText())
    private val ondalik = mapOf("t" to 1, "twb" to 1, "td" to 1, "rh" to 1, "W" to 4, "h" to 1, "rho" to 3, "v" to 3, "p" to 1)
    fun noktaKarsilastir(etiket: String, beklenen: JSONObject, d: Durum) {
        for (k in beklenen.keys()) {
            val n = ondalik[k] ?: continue
            val g = when (k) { "t" -> d.t; "twb" -> d.twb; "td" -> d.td; "rh" -> d.rh * 100; "W" -> d.W
                "h" -> d.h; "rho" -> d.rho; "v" -> d.v; else -> d.p }
            assertEquals("$etiket.$k", beklenen.getDouble(k), g, 0.5 * Math.pow(10.0, -n.toDouble()) + 1e-9)
        }
    }
}

class ReferansTest {
    @Test fun durumSenaryosu() {
        val s = Referans.yukle().getJSONArray("senaryolar")
        for (i in 0 until s.length()) {
            val sn = s.getJSONObject(i)
            if (sn.getString("ad") != "ref_durum") continue
            val p = sn.getDouble("basinc")
            val bek = sn.getJSONObject("beklenen_noktalar")
            val nk = sn.getJSONArray("noktalar")
            for (j in 0 until nk.length()) {
                val n = nk.getJSONObject(j)
                val d = coz(Ozellik.entries[n.getInt("o1")], n.getDouble("v1"), Ozellik.entries[n.getInt("o2")], n.getDouble("v2"), p)
                Referans.noktaKarsilastir(n.getString("ad"), bek.getJSONObject(n.getString("ad")), d)
            }
        }
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.psy.CozucuTest" --tests "com.psikrometri.app.psy.ReferansTest"`
Expected: FAIL — unresolved `Ozellik`, `coz`.

- [ ] **Step 3: Implement**

```kotlin
package com.psikrometri.app.psy

enum class Ozellik(val daikinKod: Int, val etiket: String) {
    KURU(0, "Kuru termometre"), MUTLAK(1, "Mutlak nem"), BAGIL(2, "Bağıl nem"), ENTALPI(3, "Entalpi"),
    YAS(4, "Yaş termometre"), HACIM(5, "Özgül hacim"), YOGUNLUK(6, "Yoğunluk")
}

class CozumHatasi(mesaj: String) : Exception(mesaj)

fun Durum.deger(o: Ozellik) = when (o) {
    Ozellik.KURU -> t; Ozellik.MUTLAK -> W; Ozellik.BAGIL -> rh * 100; Ozellik.ENTALPI -> h
    Ozellik.YAS -> twb; Ozellik.HACIM -> v; Ozellik.YOGUNLUK -> rho
}

/** t bilinirken o=v sağlayan W (hepsi kapalı form). */
private fun wBul(t: Double, o: Ozellik, v: Double, p: Double): Double {
    val T = t + 273.15
    return when (o) {
        Ozellik.MUTLAK -> v
        Ozellik.BAGIL -> nemOrani(v / 100 * pws(t), p)
        Ozellik.ENTALPI -> (v - 1.006 * t) / (2501 + 1.84 * t)
        Ozellik.YAS -> yasW(t, v, p)
        Ozellik.HACIM -> (v * p / (RDA * T) - 1) / 1.607774067
        Ozellik.YOGUNLUK -> (p - v * RDA * T) / (1.607774067 * v * RDA * T - p)
        Ozellik.KURU -> error("KURU wBul'a gelmez")
    }
}

private fun gecerli(t: Double, W: Double, p: Double) = W >= 0 && W <= doymaW(t, p) * (1 + 1e-9)

fun coz(o1: Ozellik, v1: Double, o2: Ozellik, v2: Double, p: Double): Durum {
    if (o1 == o2) throw CozumHatasi("Aynı özellik iki kez seçilemez")
    // KURU, sonra MUTLAK öne alınır: biri varsa diğeri kapalı formdan ya da tek boyutlu aramayla bulunur.
    val ters = o2 == Ozellik.KURU || (o2 == Ozellik.MUTLAK && o1 != Ozellik.KURU)
    val a = if (ters) o2 else o1; val va = if (ters) v2 else v1
    val b = if (ters) o1 else o2; val vb = if (ters) v1 else v2
    val d = when (a) {
        Ozellik.KURU -> Durum(va, wBul(va, b, vb, p), p)
        Ozellik.MUTLAK -> {
            val t = kokBul(-60.0, 100.0, 0.25) { t -> Durum(t, va, p).deger(b) - vb }
                ?: throw CozumHatasi("Bu değerlerle nokta bulunamadı")
            Durum(t, va, p)
        }
        else -> {
            val t = kokBul(-60.0, 100.0, 0.25) { t ->
                val W = wBul(t, a, va, p)
                if (!gecerli(t, W, p)) Double.NaN else Durum(t, W, p).deger(b) - vb
            } ?: throw CozumHatasi("Bu iki özellik birlikte noktayı belirlemiyor")
            Durum(t, wBul(t, a, va, p), p)
        }
    }
    if (d.t < -60 || d.t > 100) throw CozumHatasi("Sıcaklık -60…100 °C dışında")
    if (!gecerli(d.t, d.W, p)) throw CozumHatasi(if (d.W < 0) "Nem oranı negatif" else "Nokta doyma eğrisinin üstünde (RH > %100)")
    return d
}
```

- [ ] **Step 4: Run, verify PASS**

Run: aynı komut. Expected: PASS. `durumSenaryosu` bir ikilide kalırsa: Daikin'in o ikilide farklı kök seçtiğine (ör. YAS+ENTALPI neredeyse paralel) bak; raporda o noktanın değerlerini Python'la (`tools/daikin_psikro.py`) kontrol et ve kök aralığını/sırasını Daikin'inkine göre düzelt. Fark açıklanamazsa kullanıcıya sor.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(psy): 7 özellikten 21 ikili durum çözücü"
```

---

### Task 5: İşlemler (`psy/Islemler.kt`) — SHF ve Bypass hariç 13 işlem

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/psy/Islemler.kt`
- Modify: `app/src/test/java/com/psikrometri/app/psy/ReferansTest.kt` (işlem testi eklenir)

**Interfaces:**
- Consumes: Task 2, Task 4.
- Produces:
  - `data class Hava(val d: Durum, val debi: Double)`
  - `data class Kapasite(val toplam: Double? = null, val duyulur: Double? = null, val gizli: Double? = null, val isitici: Double? = null, val nemKgH: Double? = null, val cikisT: Double? = null, val bypass: Double? = null, val shf: Double? = null)`
  - `data class Cizgi(val a: Durum, val b: Durum, val kesikli: Boolean)`
  - `data class IslemSonucu(val ciktilar: List<Hava>, val cizgiler: List<Cizgi>, val kapasite: Kapasite)`
  - `enum class IslemTuru(val daikinKod: Int, val etiket: String, val girdiSayisi: Int, val ciktiSayisi: Int, val parametreler: List<Parametre>)`
  - `enum class Parametre(val etiket: String, val birim: String) { KW("Kapasite", "kW"), SHF("SHF", "%"), KGH("Miktar", "kg/h"), BF("Bypass faktörü", "%"), BUHAR("Buharlı (1) / sulu (0)", ""), ARA("Ara çizgiler", "") }`
  - `fun islemHesapla(tur: IslemTuru, girdi: List<Hava>, param: List<Double>): IslemSonucu` (hata → `CozumHatasi`); `param` sırası `tur.parametreler` sırasıdır.
  - `fun kutle(debi: Double) = 1.2 * debi / 1000`
  - `fun adpKapasite(a: Durum, shf: Double): Durum` (Cool to saturation / Discharge ADP)

- [ ] **Step 1: Failing test** — `ReferansTest`'e ekle:

```kotlin
    private val parametreSirasi: Map<Int, (List<Double>) -> List<Double>> = mapOf(
        4 to { p -> listOf(p[0], p.getOrElse(1) { 1.0 }) },        // buhar, ara
        5 to { p -> listOf(p[0], p[1], p.getOrElse(2) { 1.0 }) },  // kW, SHF, ara
        9 to { p -> listOf(p[1], p.getOrElse(2) { 1.0 }) },        // (0), SHF, ara -> SHF, ara
    )

    /** ppp sırasıyla tüm işlemleri koşturur; Daikin'in hata verdiği işlemler atlanır. */
    fun calistir(sn: JSONObject, atlaKodlar: Set<Int> = emptySet()): Pair<Map<String, Hava>, Map<String, IslemSonucu>> {
        val p = sn.getDouble("basinc")
        val hava = LinkedHashMap<String, Hava>(); val sonuc = HashMap<String, IslemSonucu>()
        val nk = sn.getJSONArray("noktalar")
        for (j in 0 until nk.length()) { val n = nk.getJSONObject(j)
            hava[n.getString("ad")] = Hava(coz(Ozellik.entries[n.getInt("o1")], n.getDouble("v1"), Ozellik.entries[n.getInt("o2")], n.getDouble("v2"), p), n.getDouble("debi")) }
        val isl = sn.getJSONArray("islemler")
        for (j in 0 until isl.length()) {
            val o = isl.getJSONObject(j); val kod = o.getInt("kod")
            if (kod in atlaKodlar) continue
            val tur = IslemTuru.entries.first { it.daikinKod == kod }
            val girdi = (0 until o.getJSONArray("girdi").length()).map { hava[o.getJSONArray("girdi").getString(it)] ?: return@map null }
            if (girdi.any { it == null }) continue
            val ham = (0 until o.getJSONArray("param").length()).map { o.getJSONArray("param").getDouble(it) }
            val param = parametreSirasi[kod]?.invoke(ham) ?: ham
            val r = try { islemHesapla(tur, girdi.map { it!! }, param) } catch (e: CozumHatasi) { continue }
            val cikti = (0 until o.getJSONArray("cikti").length()).map { o.getJSONArray("cikti").getString(it) }
            cikti.forEachIndexed { k, ad -> hava[ad] = r.ciktilar[k] }
            sonuc[cikti.firstOrNull() ?: "$kod:" + girdi.indices.joinToString(":") { o.getJSONArray("girdi").getString(it) }] = r
        }
        return hava to sonuc
    }

    private fun kapasiteKarsilastir(etiket: String, b: JSONObject, k: Kapasite) {
        val es = mapOf("Total" to k.toplam, "Sensible" to k.duyulur, "Latent" to k.gizli, "Heating" to k.isitici,
            "Dehumidification" to k.nemKgH, "Steam humidification" to k.nemKgH, "Water humidification" to k.nemKgH,
            "Discharge" to k.cikisT, "Bypass factor" to k.bypass, "SHF" to k.shf)
        for (key in b.keys()) {
            val g = es[key] ?: error("$etiket: $key hesaplanmadı")
            val tol = if (key == "Discharge" || key == "Bypass factor" || key == "SHF") 0.05 else 0.005
            assertEquals("$etiket.$key", b.getDouble(key), g, tol + 1e-9)
        }
    }

    @Test fun islemSenaryolari() {
        val s = Referans.yukle().getJSONArray("senaryolar")
        for (i in 0 until s.length()) {
            val sn = s.getJSONObject(i)
            if (sn.getString("ad") == "ref_durum" || sn.getString("ad") == "ref_shf") continue
            val atla = setOf(12, 14)  // Task 6'da emptySet() olur
            val (hava, sonuc) = calistir(sn, atla)
            val isl = sn.getJSONArray("islemler")
            val atlanan = (0 until isl.length()).map { isl.getJSONObject(it) }.filter { it.getInt("kod") in atla }
                .flatMap { o -> (0 until o.getJSONArray("cikti").length()).map { o.getJSONArray("cikti").getString(it) } }.toSet()
            val bek = sn.getJSONObject("beklenen_noktalar")
            for (ad in bek.keys()) {
                if (ad in atlanan) continue
                val h = hava[ad] ?: throw AssertionError("${sn.getString("ad")}.$ad: Daikin hesapladı, biz hesaplayamadık")
                Referans.noktaKarsilastir("${sn.getString("ad")}.$ad", bek.getJSONObject(ad), h.d)
            }
            val bi = sn.getJSONObject("beklenen_islemler")
            for (ad in bi.keys()) {
                if (ad in atlanan) continue
                val r = sonuc[ad] ?: throw AssertionError("${sn.getString("ad")}.$ad: işlem sonucu yok")
                kapasiteKarsilastir("${sn.getString("ad")}.$ad", bi.getJSONObject(ad), r.kapasite)
            }
        }
    }
```
(Gerekli importlar: `org.json.JSONObject`.)

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.psy.ReferansTest"`
Expected: FAIL — unresolved `Hava`, `islemHesapla`.

- [ ] **Step 3: Implement** — `docs/daikin-bulgular.md` "İşlem davranışı" tablosu:

```kotlin
package com.psikrometri.app.psy

data class Hava(val d: Durum, val debi: Double)
data class Kapasite(val toplam: Double? = null, val duyulur: Double? = null, val gizli: Double? = null, val isitici: Double? = null,
                    val nemKgH: Double? = null, val cikisT: Double? = null, val bypass: Double? = null, val shf: Double? = null)
data class Cizgi(val a: Durum, val b: Durum, val kesikli: Boolean)
data class IslemSonucu(val ciktilar: List<Hava>, val cizgiler: List<Cizgi>, val kapasite: Kapasite)

enum class Parametre(val etiket: String, val birim: String) {
    KW("Kapasite", "kW"), SHF("SHF", "%"), KGH("Miktar", "kg/h"), BF("Bypass faktörü", "%"), BUHAR("Buharlı", ""), ARA("Ara çizgiler", "")
}

enum class IslemTuru(val daikinKod: Int, val etiket: String, val girdiSayisi: Int, val ciktiSayisi: Int, val parametreler: List<Parametre>) {
    NOKTA_GOSTER(0, "Noktayı göster", 1, 0, emptyList()),
    BAGLA(1, "Bağla", 2, 0, emptyList()),
    KARISIM(2, "Hava karışımı", 2, 1, emptyList()),
    SOGUTMA(3, "Soğutma", 2, 1, listOf(Parametre.ARA)),
    ISITMA(4, "Isıtma", 2, 1, listOf(Parametre.BUHAR, Parametre.ARA)),
    SOGUTMA_KAPASITE(5, "Soğutma kapasitesi", 1, 2, listOf(Parametre.KW, Parametre.SHF, Parametre.ARA)),
    ISITMA_KAPASITE(6, "Isıtma kapasitesi", 1, 1, listOf(Parametre.KW)),
    BUHAR_NEM(7, "Buharlı nemlendirme", 1, 1, listOf(Parametre.KGH)),
    SU_NEM(8, "Sulu nemlendirme", 1, 1, listOf(Parametre.KGH)),
    DOYMAYA_SOGUTMA(9, "Doymaya kadar soğutma", 1, 2, listOf(Parametre.SHF, Parametre.ARA)),
    YOGUSMA(10, "Yoğuşma", 2, 0, emptyList()),
    NEM_ALMA(11, "Nem alma", 1, 1, listOf(Parametre.KGH)),
    BYPASS(12, "Bypass faktörü", 1, 2, listOf(Parametre.KW, Parametre.SHF)),
    CIKIS(13, "Üfleme (discharge)", 1, 2, listOf(Parametre.SHF, Parametre.BF)),
    SHF(14, "Duyulur ısı faktörü", 1, 1, listOf(Parametre.SHF)),
}

fun kutle(debi: Double) = 1.2 * debi / 1000

private fun sogutmaKapasitesi(m: Double, a: Durum, b: Durum) = Kapasite(
    toplam = m * (a.h - b.h), duyulur = m * cpm(b.W) * (a.t - b.t),
    gizli = m * (a.h - b.h) - m * cpm(b.W) * (a.t - b.t), shf = cpm(b.W) * (a.t - b.t) / (a.h - b.h) * 100)

/** Doyma eğrisinde cpm(Wx)·(t1−tx)/(h1−hx) = SHF; t1'e en yakın kök. */
fun adpKapasite(a: Durum, shf: Double): Durum {
    val t = kokBul(a.t - 1e-6, -60.0, 0.05) { tx ->
        val w = doymaW(tx, a.p); val dh = a.h - entalpi(tx, w)
        if (dh <= 0) Double.NaN else cpm(w) * (a.t - tx) / dh - shf
    } ?: throw CozumHatasi("İstenen SHF ile doğru %100 nem eğrisini kesmiyor. SHF'yi düşürün ya da SHF işlemini kullanın.")
    return Durum(t, doymaW(t, a.p), a.p)
}

private fun doymaKontrol(d: Durum, mesaj: String) { if (d.W > doymaW(d.t, d.p) * (1 + 1e-9) || d.W < 0) throw CozumHatasi(mesaj) }

fun islemHesapla(tur: IslemTuru, girdi: List<Hava>, param: List<Double>): IslemSonucu {
    val a = girdi[0].d; val p = a.p; val m = kutle(girdi[0].debi); val q = girdi[0].debi
    fun hava(d: Durum, debi: Double = q) = Hava(d, debi)
    fun ara(vararg d: Durum) = d.toList().zipWithNext { x, y -> Cizgi(x, y, kesikli = true) }
    val araAcik = { i: Int -> param.getOrElse(i) { 1.0 } != 0.0 }
    return when (tur) {
        IslemTuru.NOKTA_GOSTER -> IslemSonucu(emptyList(), emptyList(), Kapasite())
        IslemTuru.BAGLA -> IslemSonucu(emptyList(), listOf(Cizgi(a, girdi[1].d, false)), Kapasite())
        IslemTuru.KARISIM -> {
            val b = girdi[1].d
            val m1 = a.rho * girdi[0].debi; val m2 = b.rho * girdi[1].debi
            val W = (m1 * a.W + m2 * b.W) / (m1 + m2); val h = (m1 * a.h + m2 * b.h) / (m1 + m2)
            val c = Durum(tEntalpidan(h, W), W, p)
            IslemSonucu(listOf(hava(c, girdi[0].debi + girdi[1].debi)), listOf(Cizgi(a, c, false), Cizgi(b, c, false)), Kapasite())
        }
        IslemTuru.SOGUTMA -> {
            val b = girdi[1].d; val pp = Durum(a.t, b.W, p)
            IslemSonucu(listOf(hava(pp)), listOf(Cizgi(a, b, false)) + if (araAcik(0)) ara(a, pp, b) else emptyList(), sogutmaKapasitesi(m, a, b))
        }
        IslemTuru.ISITMA -> {
            val b = girdi[1].d; val buhar = param.getOrElse(0) { 1.0 } != 0.0
            val pp = if (buhar) Durum(b.t, a.W, p) else Durum(tEntalpidan(b.h, a.W), a.W, p)
            val nem = m * (b.W - a.W) * 3600
            IslemSonucu(listOf(hava(pp)), listOf(Cizgi(a, pp, false), Cizgi(pp, b, false)),
                Kapasite(toplam = m * (b.h - a.h), duyulur = m * cpm(a.W) * (b.t - a.t), gizli = m * (b.h - a.h) - m * cpm(a.W) * (b.t - a.t),
                    isitici = m * (pp.h - a.h), nemKgH = nem))
        }
        IslemTuru.ISITMA_KAPASITE -> {
            val c = Durum(a.t + param[0] / (m * cpm(a.W)), a.W, p)
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(isitici = param[0]))
        }
        IslemTuru.BUHAR_NEM -> {
            val kg = param[0] / 3600; val c = Durum(a.t, a.W + kg / m, p)
            doymaKontrol(c, "Buharlı nemlendirme sonucu %100 nem eğrisini aşıyor. Miktarı azaltın.")
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(gizli = kg * (2501 + 1.84 * a.t), nemKgH = param[0]))
        }
        IslemTuru.SU_NEM, IslemTuru.NEM_ALMA -> {
            val kg = param[0] / 3600; val W = if (tur == IslemTuru.SU_NEM) a.W + kg / m else a.W - kg / m
            val c = Durum(tEntalpidan(a.h, W), W, p)
            doymaKontrol(c, if (tur == IslemTuru.SU_NEM) "Sulu nemlendirme sonucu diyagram dışında. Miktarı azaltın ya da buharlı nemlendirme kullanın." else "Nem alma sonucu nem oranı negatif. Miktarı azaltın.")
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(gizli = kg * 2501, nemKgH = param[0]))
        }
        IslemTuru.SOGUTMA_KAPASITE -> {
            val kw = param[0]; val shf = param[1] / 100
            val W = a.W - (1 - shf) * kw / (m * (2501 + 1.84 * a.t))
            val c = Durum(a.t - shf * kw / (m * cpm(W)), W, p); val pp2 = Durum(a.t, W, p)
            doymaKontrol(c, "İstenen soğutma ve SHF %100 nem eğrisini aşıyor. Kapasiteyi azaltın, SHF'yi değiştirin ya da doymaya soğutma + yoğuşma kullanın.")
            IslemSonucu(listOf(hava(c), hava(pp2)), listOf(Cizgi(a, c, false)) + if (araAcik(2)) ara(a, pp2, c) else emptyList(), sogutmaKapasitesi(m, a, c))
        }
        IslemTuru.DOYMAYA_SOGUTMA -> {
            val adp = adpKapasite(a, param[0] / 100); val pp2 = Durum(a.t, adp.W, p)
            IslemSonucu(listOf(hava(adp), hava(pp2)), listOf(Cizgi(a, adp, false)) + if (araAcik(1)) ara(a, pp2, adp) else emptyList(), sogutmaKapasitesi(m, a, adp))
        }
        IslemTuru.CIKIS -> {
            val adp = adpKapasite(a, param[0] / 100); val bf = param[1] / 100
            val c = Durum(adp.t + bf * (a.t - adp.t), adp.W + bf * (a.W - adp.W), p)
            IslemSonucu(listOf(hava(c), hava(adp)), listOf(Cizgi(a, c, false), Cizgi(c, adp, true)),
                sogutmaKapasitesi(m, a, c).copy(cikisT = c.t, bypass = param[1]))
        }
        IslemTuru.YOGUSMA -> {
            val b = girdi[1].d
            if (a.rh < 0.999 || b.rh < 0.999) throw CozumHatasi("Yoğuşma işlemi için iki nokta da %100 nem eğrisinde olmalı.")
            IslemSonucu(emptyList(), listOf(Cizgi(a, b, false)), sogutmaKapasitesi(m, a, b).copy(nemKgH = m * (a.W - b.W) * 3600))
        }
        IslemTuru.SHF, IslemTuru.BYPASS -> throw CozumHatasi("Task 6")
    }
}
```

- [ ] **Step 4: Run, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.psy.ReferansTest"`
Expected: PASS. Kalan farklar için bilinen alternatifler (bulgular.md'ye sonucu yaz):
  - Sulu ısıtmada PP entalpi yerine yaş termometre sabit olabilir → `Durum(t, a.W)` ile `yasTermometre(t, a.W, p) == b.twb` kökü.
  - Yoğuşma kg/h ~%1 fark → yoğuşan suyun entalpisi (`m_w · 4.186 · t_b`) toplamdan düşülüyor olabilir; `toplam = m(ha−hb) − m(Wa−Wb)·4.186·tb` dene.
  - Discharge toplamı → `toplam = duyulur / SHF` dene.
  Bir alternatif tüm senaryolarda toleransa girmiyorsa kullanıcıya Daikin sayılarıyla sor.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(psy): 13 işlem, Daikin referansıyla doğrulandı"
```

---

### Task 6: SHF işlemi ve Bypass factor (`psy/ShfDogrusu.kt`)

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/psy/ShfDogrusu.kt`
- Modify: `app/src/main/java/com/psikrometri/app/psy/Islemler.kt` (SHF/BYPASS dalı), `ReferansTest.kt` (`atlaKodlar` kaldırılır), `tools/shf_uydur.py` (veri `ref_shf` + `ref_islem`'den okunur)

**Interfaces:**
- Consumes: Task 5.
- Produces: `fun shfKesisim(a: Durum, shf: Double): Durum` — SHF doğrusunun doyma eğrisiyle (yoksa W=0 ile) kesişimi; Bypass factor'ün ADP'si.

- [ ] **Step 1: Veriyle aday seç**

`tools/shf_uydur.py`'deki `SHF_ISLEM` listesini `app/src/test/resources/daikin_referans.json` → `ref_shf` senaryosundan (32 nokta) doldur:
```python
import json
ref = json.load(open('app/src/test/resources/daikin_referans.json', encoding='utf-8'))
sn = next(s for s in ref['senaryolar'] if s['ad'] == 'ref_shf')
bas = {n['ad']: (n['v1'], n['v2']) for n in sn['noktalar']}
SHF_ISLEM = [((*bas[i['girdi'][0]], i['param'][0] / 100), (sn['beklenen_noktalar'][i['cikti'][0]]['t'], sn['beklenen_noktalar'][i['cikti'][0]]['W']))
             for i in sn['islemler'] if i['cikti'][0] in sn['beklenen_noktalar']]
```
Run: `py -3 -I tools/shf_uydur.py`
Seçim kuralı: tüm 32 senaryoda `max|dt| ≤ 0.05` olan aday. Mevcut adaylar yetmezse şunları da ekle: doğrultu (h,W) düzleminde `Δh/ΔW = hfg/(1−SHF)` (hfg ∈ {2501, 2501+1.84t1, 2501−2.346t1, 2454}); doğrultu kuru hava ısısı `1.006` + buhar `1.84·W1` ile, hfg `2501+1.84·t_adp`. Hiçbiri tutmazsa: kullanıcıya 32 senaryonun tablosunu göster, dur.

- [ ] **Step 2: Failing test** — `ReferansTest.islemSenaryolari` içinde `val atla = setOf(12, 14)` → `val atla = emptySet<Int>()`; `ref_shf` senaryosunu da döngüye al (koşul `if (sn.getString("ad") == "ref_durum") continue` olsun).

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.psy.ReferansTest"`
Expected: FAIL — `CozumHatasi: Task 6`.

- [ ] **Step 3: Implement** — Step 1'de seçilen aday. Aşağıdaki kod mevcut en iyi aday (`dogru cp=1.006+1.84W1, hfg=2501−2.346·t1`); Step 1 farklı aday seçtiyse yalnız `egim` satırı değişir:

```kotlin
package com.psikrometri.app.psy

/** SHF işlemi ve Bypass factor'ün doğrusu: P1'den doymaya (ya da W=0'a) giden doğru. Tanım tools/shf_uydur.py ile seçildi. */
fun shfKesisim(a: Durum, shf: Double): Durum {
    val egim = cpm(a.W) * (1 - shf) / (shf * (2501 - 2.346 * a.t))  // dW/dt
    val w = { t: Double -> a.W - (a.t - t) * egim }
    val t = kokBul(a.t - 1e-6, -60.0, 0.05) { t -> if (w(t) <= 0) Double.NaN else w(t) - doymaW(t, a.p) }
    if (t != null) return Durum(t, doymaW(t, a.p), a.p)
    val t0 = a.t - a.W / egim
    return Durum(t0, 0.0, a.p)
}
```

`Islemler.kt` içindeki `IslemTuru.SHF, IslemTuru.BYPASS -> throw ...` satırını değiştir:
```kotlin
        IslemTuru.SHF -> {
            val c = shfKesisim(a, param[0] / 100)
            IslemSonucu(listOf(hava(c)), listOf(Cizgi(a, c, false)), Kapasite(shf = param[0]))
        }
        IslemTuru.BYPASS -> {
            val son = islemHesapla(IslemTuru.SOGUTMA_KAPASITE, girdi, listOf(param[0], param[1], 0.0)).ciktilar[0].d
            val adp = shfKesisim(a, param[1] / 100)
            val bf = (son.t - adp.t) / (a.t - adp.t) * 100
            IslemSonucu(listOf(hava(son), hava(adp)), listOf(Cizgi(a, son, false), Cizgi(son, adp, true)),
                sogutmaKapasitesi(m, a, son).copy(cikisT = son.t, bypass = bf))
        }
```

- [ ] **Step 4: Run, verify PASS**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (tüm testler).

- [ ] **Step 5: Commit + bulgular**

`docs/daikin-bulgular.md` → SHF ve Bypass satırlarını "✓ <seçilen tanım>" yap.
```bash
git add -A && git commit -m "feat(psy): SHF işlemi ve bypass faktörü"
```

---

### Task 7: Proje modeli, bağımlı hesap ve JSON (`model/Proje.kt`)

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/model/Proje.kt`
- Test: `app/src/test/java/com/psikrometri/app/model/ProjeTest.kt`

**Interfaces:**
- Consumes: Task 4–6.
- Produces:
  - `data class Nokta(val ad: String, val o1: Ozellik, val v1: Double, val o2: Ozellik, val v2: Double, val debi: Double, val renk: Int = MAVI)`
  - `data class Islem(val tur: IslemTuru, val girdiler: List<String>, val ciktilar: List<String>, val param: List<Double>, val renk: Int = MAVI)`
  - `data class Proje(val rakim: Double = 0.0, val noktalar: List<Nokta> = emptyList(), val islemler: List<Islem> = emptyList())` — `val basinc get() = basincRakimdan(rakim)`; `fun hesapla(): Hesap`; `fun yeniPPAdi(): String`; `fun json(): String`; `companion fun jsonOku(s: String): Proje`
  - `data class Hesap(val hava: Map<String, Hava>, val sonuclar: List<Result<IslemSonucu>>, val noktaHatalari: Map<String, String>)` — `sonuclar[i]` `islemler[i]`'ye karşılık gelir.
  - `const val MAVI = 0xFF1565C0.toInt()`; `val RENKLER: List<Int>` (8 renk)

- [ ] **Step 1: Failing test**

```kotlin
package com.psikrometri.app.model

import com.psikrometri.app.psy.IslemTuru
import com.psikrometri.app.psy.Ozellik
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjeTest {
    private val proje = Proje(
        noktalar = listOf(Nokta("P1", Ozellik.KURU, 27.0, Ozellik.MUTLAK, 0.010, 150.0), Nokta("P2", Ozellik.KURU, 15.0, Ozellik.MUTLAK, 0.008, 150.0)),
        islemler = listOf(
            Islem(IslemTuru.KARISIM, listOf("P1", "P2"), listOf("PP1"), emptyList()),
            Islem(IslemTuru.ISITMA_KAPASITE, listOf("PP1"), listOf("PP2"), listOf(5.0)),   // önceki işlemin çıktısını kullanır
            Islem(IslemTuru.SOGUTMA_KAPASITE, listOf("P1"), listOf("PP3", "PP4"), listOf(50.0, 75.0, 1.0)),  // hata verir
        ))

    @Test fun zincirliHesap() {
        val h = proje.hesapla()
        assertEquals(20.9, h.hava.getValue("PP1").d.t, 0.05)
        assertEquals(300.0, h.hava.getValue("PP1").debi, 1e-9)
        assertEquals(20.9 + 5 / (0.36 * 1.0166), h.hava.getValue("PP2").d.t, 0.1)  // m = 1,2·300/1000
        assertTrue(h.sonuclar[2].isFailure)
    }

    @Test fun noktaDegisinceBagliIslemDegisir() {
        val p2 = proje.copy(noktalar = proje.noktalar.map { if (it.ad == "P2") it.copy(v1 = 5.0) else it })
        assertTrue(p2.hesapla().hava.getValue("PP1").d.t < proje.hesapla().hava.getValue("PP1").d.t)
    }

    @Test fun jsonGidisDonus() {
        assertEquals(proje, Proje.jsonOku(proje.copy(rakim = 1500.0).json()).copy(rakim = 0.0))
    }

    @Test fun ppAdi() { assertEquals("PP5", proje.yeniPPAdi()) }
}
```

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.model.ProjeTest"`
Expected: FAIL — unresolved `Proje`.

- [ ] **Step 3: Implement**

```kotlin
package com.psikrometri.app.model

import com.psikrometri.app.psy.*
import org.json.JSONArray
import org.json.JSONObject

const val MAVI = 0xFF1565C0.toInt()
val RENKLER = listOf(MAVI, 0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFEF6C00.toInt(), 0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFF4E342E.toInt(), 0xFF212121.toInt())

data class Nokta(val ad: String, val o1: Ozellik, val v1: Double, val o2: Ozellik, val v2: Double, val debi: Double, val renk: Int = MAVI)
data class Islem(val tur: IslemTuru, val girdiler: List<String>, val ciktilar: List<String>, val param: List<Double>, val renk: Int = MAVI)
data class Hesap(val hava: Map<String, Hava>, val sonuclar: List<Result<IslemSonucu>>, val noktaHatalari: Map<String, String>)

data class Proje(val rakim: Double = 0.0, val noktalar: List<Nokta> = emptyList(), val islemler: List<Islem> = emptyList()) {
    val basinc get() = basincRakimdan(rakim)

    fun hesapla(): Hesap {
        val hava = LinkedHashMap<String, Hava>(); val hatalar = HashMap<String, String>()
        for (n in noktalar) try { hava[n.ad] = Hava(coz(n.o1, n.v1, n.o2, n.v2, basinc), n.debi) } catch (e: CozumHatasi) { hatalar[n.ad] = e.message!! }
        val sonuclar = islemler.map { i ->
            runCatching {
                val girdi = i.girdiler.map { hava[it] ?: throw CozumHatasi("$it noktası yok") }
                islemHesapla(i.tur, girdi, i.param).also { r -> i.ciktilar.forEachIndexed { k, ad -> hava[ad] = r.ciktilar[k] } }
            }
        }
        return Hesap(hava, sonuclar, hatalar)
    }

    fun yeniPPAdi(): String = "PP" + ((islemler.flatMap { it.ciktilar }.mapNotNull { it.removePrefix("PP").toIntOrNull() }.maxOrNull() ?: 0) + 1)

    fun json(): String = JSONObject().put("surum", 1).put("rakim", rakim)
        .put("noktalar", JSONArray(noktalar.map { JSONObject().put("ad", it.ad).put("o1", it.o1.name).put("v1", it.v1).put("o2", it.o2.name).put("v2", it.v2).put("debi", it.debi).put("renk", it.renk) }))
        .put("islemler", JSONArray(islemler.map { JSONObject().put("tur", it.tur.name).put("girdiler", JSONArray(it.girdiler)).put("ciktilar", JSONArray(it.ciktilar)).put("param", JSONArray(it.param)).put("renk", it.renk) }))
        .toString(1)

    companion object {
        fun jsonOku(s: String): Proje {
            val o = JSONObject(s)
            fun JSONArray.str() = (0 until length()).map { getString(it) }
            fun JSONArray.dbl() = (0 until length()).map { getDouble(it) }
            val n = o.getJSONArray("noktalar"); val i = o.getJSONArray("islemler")
            return Proje(o.getDouble("rakim"),
                (0 until n.length()).map { n.getJSONObject(it) }.map { Nokta(it.getString("ad"), Ozellik.valueOf(it.getString("o1")), it.getDouble("v1"), Ozellik.valueOf(it.getString("o2")), it.getDouble("v2"), it.getDouble("debi"), it.getInt("renk")) },
                (0 until i.length()).map { i.getJSONObject(it) }.map { Islem(IslemTuru.valueOf(it.getString("tur")), it.getJSONArray("girdiler").str(), it.getJSONArray("ciktilar").str(), it.getJSONArray("param").dbl(), it.getInt("renk")) })
        }
    }
}
```

- [ ] **Step 4: Run, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.model.ProjeTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(model): proje, zincirli hesap, JSON"
```

---

### Task 8: Birimler ve ayarlar

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/model/Birimler.kt`, `app/src/main/java/com/psikrometri/app/ui/Ayarlar.kt`
- Test: `app/src/test/java/com/psikrometri/app/model/BirimlerTest.kt`
- Modify: `docs/superpowers/specs/2026-10-13-psikrometri-design.md` ("Ayarlar (DataStore)" → "Ayarlar (SharedPreferences)")

**Interfaces:**
- Produces:
  - `data class Birim(val etiket: String, val carpan: Double, val ofset: Double = 0.0)` — `fun goster(si: Double) = si * carpan + ofset`; `fun siye(v: Double) = (v - ofset) / carpan`
  - `enum class Buyukluk(val secenekler: List<Birim>)` — `SICAKLIK, NEM, DEBI, ENTALPI, BASINC, GUC` (GUC: kW, kcal/h)
  - `data class BirimSecimi(val secim: Map<Buyukluk, Int>)` — `fun birim(b: Buyukluk): Birim`; `fun ozellikBirimi(o: Ozellik): Birim` (BAGIL→"%", HACIM→"m³/kg", YOGUNLUK→"kg/m³" sabit)
  - `data class Ayarlar(val birimler: BirimSecimi, val tMin: Double, val tMax: Double, val wMax: Double, val egriler: Set<EgriTuru>)` + `fun ayarlariOku(ctx: Context): Ayarlar`, `fun ayarlariYaz(ctx: Context, a: Ayarlar)`
  - `enum class EgriTuru(val etiket: String) { KURU("Kuru termometre"), MUTLAK("Mutlak nem"), BAGIL("Bağıl nem"), ENTALPI("Entalpi"), YAS("Yaş termometre"), HACIM("Özgül hacim"), YOGUNLUK("Yoğunluk") }` (Ayarlar.kt içinde)

- [ ] **Step 1: Failing test**

```kotlin
package com.psikrometri.app.model

import com.psikrometri.app.psy.Ozellik
import org.junit.Assert.assertEquals
import org.junit.Test

class BirimlerTest {
    @Test fun donusumler() {
        val f = Buyukluk.SICAKLIK.secenekler.first { it.etiket == "°F" }
        assertEquals(212.0, f.goster(100.0), 1e-9); assertEquals(100.0, f.siye(212.0), 1e-9)
        assertEquals(540.0, Buyukluk.DEBI.secenekler.first { it.etiket == "m³/h" }.goster(150.0), 1e-9)
        assertEquals(10.0, Buyukluk.NEM.secenekler.first { it.etiket == "g/kg" }.goster(0.010), 1e-12)
        assertEquals(101.325, Buyukluk.BASINC.secenekler.first { it.etiket == "kPa" }.goster(101325.0), 1e-9)
    }

    @Test fun ozellikBirimi() {
        val s = BirimSecimi(mapOf(Buyukluk.SICAKLIK to 1))
        assertEquals("°F", s.ozellikBirimi(Ozellik.YAS).etiket)
        assertEquals("%", s.ozellikBirimi(Ozellik.BAGIL).etiket)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.model.BirimlerTest"`
Expected: FAIL — unresolved `Buyukluk`.

- [ ] **Step 3: Implement**

`Birimler.kt`:
```kotlin
package com.psikrometri.app.model

import com.psikrometri.app.psy.Ozellik

data class Birim(val etiket: String, val carpan: Double, val ofset: Double = 0.0) {
    fun goster(si: Double) = si * carpan + ofset
    fun siye(v: Double) = (v - ofset) / carpan
}

enum class Buyukluk(val secenekler: List<Birim>) {
    SICAKLIK(listOf(Birim("°C", 1.0), Birim("°F", 1.8, 32.0))),
    NEM(listOf(Birim("kg/kg", 1.0), Birim("g/kg", 1000.0))),
    DEBI(listOf(Birim("l/s", 1.0), Birim("m³/h", 3.6), Birim("m³/s", 0.001))),
    ENTALPI(listOf(Birim("kJ/kg", 1.0), Birim("kcal/kg", 1 / 4.1868))),
    BASINC(listOf(Birim("Pa", 1.0), Birim("kPa", 0.001), Birim("mbar", 0.01))),
    GUC(listOf(Birim("kW", 1.0), Birim("kcal/h", 859.845)))
}

data class BirimSecimi(val secim: Map<Buyukluk, Int> = emptyMap()) {
    fun birim(b: Buyukluk) = b.secenekler[secim[b] ?: 0]
    fun ozellikBirimi(o: Ozellik) = when (o) {
        Ozellik.KURU, Ozellik.YAS -> birim(Buyukluk.SICAKLIK)
        Ozellik.MUTLAK -> birim(Buyukluk.NEM)
        Ozellik.ENTALPI -> birim(Buyukluk.ENTALPI)
        Ozellik.BAGIL -> Birim("%", 1.0)
        Ozellik.HACIM -> Birim("m³/kg", 1.0)
        Ozellik.YOGUNLUK -> Birim("kg/m³", 1.0)
    }
}
```

`ui/Ayarlar.kt`:
```kotlin
package com.psikrometri.app.ui

import android.content.Context
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Buyukluk

enum class EgriTuru(val etiket: String) { KURU("Kuru termometre"), MUTLAK("Mutlak nem"), BAGIL("Bağıl nem"), ENTALPI("Entalpi"), YAS("Yaş termometre"), HACIM("Özgül hacim"), YOGUNLUK("Yoğunluk") }

data class Ayarlar(val birimler: BirimSecimi = BirimSecimi(), val tMin: Double = -40.0, val tMax: Double = 65.0, val wMax: Double = 0.055,
                   val egriler: Set<EgriTuru> = EgriTuru.entries.toSet())

fun ayarlariOku(ctx: Context): Ayarlar {
    val sp = ctx.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)
    return Ayarlar(
        BirimSecimi(Buyukluk.entries.associateWith { sp.getInt("birim_${it.name}", 0) }),
        sp.getFloat("tMin", -40f).toDouble(), sp.getFloat("tMax", 65f).toDouble(), sp.getFloat("wMax", 0.055f).toDouble(),
        EgriTuru.entries.filter { sp.getBoolean("egri_${it.name}", true) }.toSet())
}

fun ayarlariYaz(ctx: Context, a: Ayarlar) {
    val e = ctx.getSharedPreferences("ayarlar", Context.MODE_PRIVATE).edit()
    Buyukluk.entries.forEach { e.putInt("birim_${it.name}", a.birimler.secim[it] ?: 0) }
    e.putFloat("tMin", a.tMin.toFloat()).putFloat("tMax", a.tMax.toFloat()).putFloat("wMax", a.wMax.toFloat())
    EgriTuru.entries.forEach { e.putBoolean("egri_${it.name}", it in a.egriler) }
    e.apply()
}
```

- [ ] **Step 4: Run, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.model.BirimlerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: birimler ve ayarlar"
```

---

### Task 9: Diyagram çizici (`ui/DiyagramCizer.kt`)

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/ui/DiyagramCizer.kt`
- Test: `app/src/test/java/com/psikrometri/app/ui/EksenTest.kt`

**Interfaces:**
- Consumes: Task 2, 5, 7, 8.
- Produces:
  - `data class Eksen(val tMin: Double, val tMax: Double, val wMax: Double, val sol: Float, val ust: Float, val gen: Float, val yuk: Float)` — `fun x(t)`, `fun y(W)`, `fun t(x)`, `fun w(y)`
  - `data class Egri(val tur: EgriTuru, val deger: Double, val noktalar: List<Pair<Double, Double>>)` — (t, W) çiftleri, doymaya ve eksene kırpılmış
  - `fun egrileriUret(p: Double, tMin: Double, tMax: Double, wMax: Double, turler: Set<EgriTuru>): List<Egri>`
  - `fun cizDiyagram(c: android.graphics.Canvas, e: Eksen, p: Double, egriler: List<Egri>, hesap: Hesap, proje: Proje, secili: String?, onizleme: Durum?, birim: BirimSecimi)`

- [ ] **Step 1: Failing test**

```kotlin
package com.psikrometri.app.ui

import com.psikrometri.app.psy.P_DENIZ
import com.psikrometri.app.psy.doymaW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EksenTest {
    private val e = Eksen(-40.0, 65.0, 0.055, 10f, 20f, 1050f, 550f)

    @Test fun gidisDonus() {
        assertEquals(10f, e.x(-40.0), 1e-4f); assertEquals(1060f, e.x(65.0), 1e-4f)
        assertEquals(570f, e.y(0.0), 1e-4f); assertEquals(20f, e.y(0.055), 1e-4f)
        assertEquals(27.0, e.t(e.x(27.0)), 1e-4); assertEquals(0.01, e.w(e.y(0.01)), 1e-7)
    }

    @Test fun egrilerDoymaUstuneTasmaz() {
        for (g in egrileriUret(P_DENIZ, -40.0, 65.0, 0.055, EgriTuru.entries.toSet()))
            for ((t, w) in g.noktalar) {
                assertTrue("${g.tur} ${g.deger} t=$t", t >= -40.0 - 1e-6 && t <= 65.0 + 1e-6)
                assertTrue("${g.tur} ${g.deger} w=$w", w >= -1e-9 && w <= minOf(0.055, doymaW(t, P_DENIZ)) + 1e-6)
            }
    }
}
```

- [ ] **Step 2: Run, verify FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.ui.EksenTest"`
Expected: FAIL — unresolved `Eksen`.

- [ ] **Step 3: Implement**

```kotlin
package com.psikrometri.app.ui

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import com.psikrometri.app.model.BirimSecimi
import com.psikrometri.app.model.Hesap
import com.psikrometri.app.model.Proje
import com.psikrometri.app.psy.*
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

data class Eksen(val tMin: Double, val tMax: Double, val wMax: Double, val sol: Float, val ust: Float, val gen: Float, val yuk: Float) {
    fun x(t: Double) = sol + ((t - tMin) / (tMax - tMin) * gen).toFloat()
    fun y(w: Double) = ust + yuk - (w / wMax * yuk).toFloat()
    fun t(x: Float) = tMin + (x - sol) / gen * (tMax - tMin)
    fun w(y: Float) = (ust + yuk - y) / yuk * wMax
}

data class Egri(val tur: EgriTuru, val deger: Double, val noktalar: List<Pair<Double, Double>>)

/** W'yi 0..min(wMax, doyma) aralığında örnekleyip t(W) ile çizilen eğri. */
private fun wEgrisi(tur: EgriTuru, deger: Double, p: Double, tMin: Double, tMax: Double, wMax: Double, tBul: (Double) -> Double?): Egri {
    val nk = ArrayList<Pair<Double, Double>>()
    var w = 0.0
    while (w <= wMax + 1e-12) {
        val t = tBul(w)
        if (t != null && t in tMin..tMax && w <= doymaW(t, p)) nk += t to w
        w += wMax / 200
    }
    return Egri(tur, deger, nk)
}

fun egrileriUret(p: Double, tMin: Double, tMax: Double, wMax: Double, turler: Set<EgriTuru>): List<Egri> {
    val r = ArrayList<Egri>()
    val adimT = 5.0
    if (EgriTuru.KURU in turler) {
        var t = ceil(tMin / adimT) * adimT
        while (t <= tMax) { r += Egri(EgriTuru.KURU, t, listOf(t to 0.0, t to minOf(wMax, doymaW(t, p)))); t += adimT }
    }
    if (EgriTuru.MUTLAK in turler) {
        var w = 0.005
        while (w <= wMax + 1e-12) {
            val td = kokBul(tMin, tMax, 1.0) { doymaW(it, p) - w }
            if (td != null) r += Egri(EgriTuru.MUTLAK, w, listOf(td to w, tMax to w))
            w += 0.005
        }
    }
    if (EgriTuru.BAGIL in turler) for (rh in 10..100 step 10) {
        val nk = ArrayList<Pair<Double, Double>>(); var t = tMin
        while (t <= tMax) { val w = nemOrani(rh / 100.0 * pws(t), p); if (w <= wMax) nk += t to w; t += 0.5 }
        r += Egri(EgriTuru.BAGIL, rh.toDouble(), nk)
    }
    if (EgriTuru.ENTALPI in turler) {
        var h = floor(entalpi(tMin, 0.0) / 10) * 10
        while (h <= entalpi(tMax, minOf(wMax, doymaW(tMax, p)))) { r += wEgrisi(EgriTuru.ENTALPI, h, p, tMin, tMax, wMax) { tEntalpidan(h, it) }; h += 10 }
    }
    if (EgriTuru.YAS in turler) {
        var tw = ceil(tMin / adimT) * adimT
        while (tw <= tMax) { val twc = tw; r += wEgrisi(EgriTuru.YAS, tw, p, tMin, tMax, wMax) { w -> kokBul(twc, tMax + 1, 1.0) { yasW(it, twc, p) - w } }; tw += adimT }
    }
    if (EgriTuru.HACIM in turler) {
        var v = 0.70
        while (v <= 1.10) { val vc = v; r += wEgrisi(EgriTuru.HACIM, v, p, tMin, tMax, wMax) { w -> vc * p / (RDA * (1 + 1.607774067 * w)) - 273.15 }; v += 0.02 }
    }
    if (EgriTuru.YOGUNLUK in turler) {
        var ro = 0.95
        while (ro <= 1.50) { val rc = ro; r += wEgrisi(EgriTuru.YOGUNLUK, ro, p, tMin, tMax, wMax) { w -> (1 + w) * p / (rc * RDA * (1 + 1.607774067 * w)) - 273.15 }; ro += 0.05 }
    }
    // Doyma eğrisi her zaman
    val doyma = ArrayList<Pair<Double, Double>>(); var t = tMin
    while (t <= tMax) { val w = doymaW(t, p); if (w <= wMax) doyma += t to w; t += 0.25 }
    r += Egri(EgriTuru.BAGIL, 100.0, doyma)
    return r
}

private val RENK_EGRI = mapOf(
    EgriTuru.KURU to 0xFFB0B0B0.toInt(), EgriTuru.MUTLAK to 0xFFB0B0B0.toInt(), EgriTuru.BAGIL to 0xFF29A3E0.toInt(),
    EgriTuru.ENTALPI to 0xFFE66A1F.toInt(), EgriTuru.YAS to 0xFF7FC8F0.toInt(), EgriTuru.HACIM to 0xFFF2C6A8.toInt(), EgriTuru.YOGUNLUK to 0xFFC8C8C8.toInt())

fun cizDiyagram(c: Canvas, e: Eksen, p: Double, egriler: List<Egri>, hesap: Hesap, proje: Proje, secili: String?, onizleme: Durum?, birim: BirimSecimi) {
    val ince = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f }
    val yazi = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f }
    c.save(); c.clipRect(e.sol, e.ust, e.sol + e.gen, e.ust + e.yuk)
    for (g in egriler) {
        if (g.noktalar.size < 2) continue
        ince.color = RENK_EGRI.getValue(g.tur); ince.strokeWidth = if (g.tur == EgriTuru.BAGIL && g.deger == 100.0) 2.5f else 1.2f
        val path = Path(); g.noktalar.forEachIndexed { i, (t, w) -> if (i == 0) path.moveTo(e.x(t), e.y(w)) else path.lineTo(e.x(t), e.y(w)) }
        c.drawPath(path, ince)
    }
    c.restore()
    // Eksen yazıları
    yazi.color = 0xFF333333.toInt()
    val tb = birim.birim(com.psikrometri.app.model.Buyukluk.SICAKLIK); val wb = birim.birim(com.psikrometri.app.model.Buyukluk.NEM)
    var t = ceil(e.tMin / 5) * 5
    while (t <= e.tMax) { c.drawText("%.0f".format(tb.goster(t)), e.x(t) - 12, e.ust + e.yuk + 24, yazi); t += 5 }
    var w = 0.0
    while (w <= e.wMax + 1e-12) { c.drawText(if (wb.carpan > 1) "%.0f".format(wb.goster(w)) else "%.3f".format(w), e.sol + e.gen + 6, e.y(w) + 8, yazi); w += 0.005 }
    // İşlem çizgileri
    val cizgi = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    proje.islemler.forEachIndexed { i, isl ->
        val r = hesap.sonuclar.getOrNull(i)?.getOrNull() ?: return@forEachIndexed
        cizgi.color = isl.renk
        for (cz in r.cizgiler) {
            cizgi.pathEffect = if (cz.kesikli) DashPathEffect(floatArrayOf(10f, 8f), 0f) else null
            val x1 = e.x(cz.a.t); val y1 = e.y(cz.a.W); val x2 = e.x(cz.b.t); val y2 = e.y(cz.b.W)
            c.drawLine(x1, y1, x2, y2, cizgi)
            if (!cz.kesikli) { // ok başı ortada
                val a = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()); val mx = (x1 + x2) / 2; val my = (y1 + y2) / 2
                cizgi.pathEffect = null
                for (s in listOf(2.6, -2.6)) c.drawLine(mx, my, (mx + 16 * cos(a + s)).toFloat(), (my + 16 * sin(a + s)).toFloat(), cizgi)
            }
        }
    }
    // Noktalar
    val dolgu = Paint(Paint.ANTI_ALIAS_FLAG)
    val renkler = proje.noktalar.associate { it.ad to it.renk } + proje.islemler.flatMap { i -> i.ciktilar.map { it to i.renk } }
    for ((ad, h) in hesap.hava) {
        dolgu.color = renkler[ad] ?: 0xFF1565C0.toInt(); yazi.color = dolgu.color
        val x = e.x(h.d.t); val y = e.y(h.d.W)
        c.drawCircle(x, y, if (ad == secili) 11f else 7f, dolgu)
        c.drawText(ad, x + 10, y - 10, yazi)
    }
    onizleme?.let { dolgu.color = 0x88C62828.toInt(); c.drawCircle(e.x(it.t), e.y(it.W), 9f, dolgu) }
}
```

- [ ] **Step 4: Run, verify PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.psikrometri.app.ui.EksenTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): diyagram eğrileri ve çizici"
```

---

### Task 10: ViewModel, ortak bileşenler, ana ekran ve Diyagram sarmalayıcı

**Files:**
- Create: `ui/AnaVM.kt`, `ui/Ortak.kt`, `ui/Diyagram.kt`, `ui/AnaEkran.kt`
- Modify: `MainActivity.kt` (`setContent { AnaEkran() }`)

**Interfaces:**
- Consumes: Task 7–9.
- Produces:
  - `class AnaVM(app: Application) : AndroidViewModel(app)` — `var proje by mutableStateOf(Proje())`, `val hesap by derivedStateOf { proje.hesapla() }`, `var secili: String?`, `var onizleme: Durum?`, `var ayarlar: Ayarlar`, `fun ayarDegistir(a: Ayarlar)`, `val egriler by derivedStateOf { egrileriUret(proje.basinc, ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, ayarlar.egriler) }`
  - `@Composable fun SayiAlani(deger: String, onDeger: (String) -> Unit, etiket: String, birim: String, modifier: Modifier = Modifier, hata: String? = null)` — virgül ve noktayı kabul eder
  - `fun String.sayi(): Double?` (virgül→nokta), `fun Double.yaz(ondalik: Int): String`, `fun Double.kisa(): String` (alana geri yazma)
  - `@Composable fun <T> SecimKutusu(secenekler: List<T>, secili: T, etiket: (T) -> String, onSec: (T) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun Diyagram(vm: AnaVM, modifier: Modifier)` — zoom/pan, çift dokunuş sıfırlar, dokunuş en yakın noktayı seçer, uzun basış balon açar ("Nokta ekle")
  - `@Composable fun AnaEkran(vm: AnaVM = viewModel())`

- [ ] **Step 1: AnaVM + Ortak**

`ui/AnaVM.kt`:
```kotlin
package com.psikrometri.app.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.psikrometri.app.model.Proje
import com.psikrometri.app.psy.Durum

class AnaVM(app: Application) : AndroidViewModel(app) {
    var proje by mutableStateOf(Proje())
    var secili by mutableStateOf<String?>(null)
    var onizleme by mutableStateOf<Durum?>(null)
    var ayarlar by mutableStateOf(ayarlariOku(app))
        private set
    val hesap by derivedStateOf { proje.hesapla() }
    val egriler by derivedStateOf { egrileriUret(proje.basinc, ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, ayarlar.egriler) }
    fun ayarDegistir(a: Ayarlar) { ayarlar = a; ayarlariYaz(getApplication(), a) }
}
```

`ui/Ortak.kt`:
```kotlin
package com.psikrometri.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

fun String.sayi(): Double? = trim().replace(',', '.').toDoubleOrNull()
fun Double.yaz(ondalik: Int): String = if (isNaN()) "–" else "%.${ondalik}f".format(this).replace('.', ',')
/** Giriş alanına geri yazmak için: 75.0 -> "75", 0.0 -> "0", 0.0123 -> "0,0123". */
fun Double.kisa(): String = if (this == Math.rint(this)) toLong().toString() else "%.4f".format(java.util.Locale.ROOT, this).trimEnd('0').replace('.', ',')

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
```

- [ ] **Step 2: Diyagram sarmalayıcı**

`ui/Diyagram.kt`:
```kotlin
package com.psikrometri.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    fun eksen() = Eksen(vm.ayarlar.tMin, vm.ayarlar.tMax, vm.ayarlar.wMax, 8f, 8f, boyut.width - 90f, boyut.height - 44f)
    fun ekrandanVeriye(o: Offset) = (o - kayma) / olcek
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()
            .onSizeChanged { boyut = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) { detectTransformGestures { merkez, kay, zoom, _ ->
                val yeni = (olcek * zoom).coerceIn(1f, 8f)
                kayma = merkez - (merkez - kayma) * (yeni / olcek) + kay; olcek = yeni } }
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
            Card(Modifier.offset { IntOffset(o.x.toInt(), o.y.toInt()) }.widthIn(max = 220.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text("t ${d.t.yaz(1)} °C · W ${d.Wr.yaz(4)}"); Text("RH %${(d.rh * 100).yaz(1)} · h ${d.h.yaz(1)}")
                    TextButton({ noktaEkle(d); balon = null }) { Text("Nokta ekle") }
                }
            }
        }
    }
}
```

- [ ] **Step 3: AnaEkran (sekmeler Task 11'de dolar)**

`ui/AnaEkran.kt`:
```kotlin
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
```

`AyarDiyalog` ve `dosyaIslemleri` Task 12'de; bu task derlensin diye geçici olarak `AnaEkran.kt` sonuna ekle:
```kotlin
@Composable fun AyarDiyalog(vm: AnaVM, kapat: () -> Unit) { kapat() }
class DosyaIslemleri(val ac: () -> Unit, val kaydet: () -> Unit, val rapor: () -> Unit)
@Composable fun dosyaIslemleri(vm: AnaVM) = DosyaIslemleri({}, {}, {})
@Composable fun NoktalarSekmesi(vm: AnaVM, taslak: Durum?, taslakKullanildi: () -> Unit) { Text("Noktalar") }
@Composable fun IslemlerSekmesi(vm: AnaVM) { Text("İşlemler") }
@Composable fun VeriSekmesi(vm: AnaVM) { Text("Veri") }
```

`MainActivity.kt`: `setContent { com.psikrometri.app.ui.AnaEkran() }`

- [ ] **Step 4: Derle ve emülatörde aç**

Run: `./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.psikrometri.app/.MainActivity`
Expected: exit 0; ekranda diyagram (RH, entalpi, yaş term. eğrileri, doyma eğrisi), sekmeler. `adb exec-out screencap -p > ekran.png` ile ekran görüntüsü al, Daikin ekranıyla göz kararı kıyasla (eğri yerleri). Uzun basınca balon, çift dokununca sıfırlama çalışır.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): ana ekran, diyagram etkileşimi, rakım"
```

---

### Task 11: Noktalar, İşlemler, Veri sekmeleri

**Files:**
- Create: `ui/NoktalarSekmesi.kt`, `ui/IslemlerSekmesi.kt`, `ui/VeriSekmesi.kt`
- Modify: `ui/AnaEkran.kt` (geçici `NoktalarSekmesi/IslemlerSekmesi/VeriSekmesi` tanımlarını sil)

**Interfaces:**
- Consumes: Task 4, 5, 7, 8, 10.
- Produces: üç sekme composable'ı (imzalar Task 10'daki geçicilerle aynı).

- [ ] **Step 1: NoktalarSekmesi**

```kotlin
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
    var ad by remember(duzenlenen) { mutableStateOf(duzenlenen?.ad ?: "P${vm.proje.noktalar.size + 1}") }
    var o1 by remember(duzenlenen) { mutableStateOf(duzenlenen?.o1 ?: Ozellik.KURU) }
    var o2 by remember(duzenlenen) { mutableStateOf(duzenlenen?.o2 ?: Ozellik.BAGIL) }
    var v1 by remember(duzenlenen) { mutableStateOf(duzenlenen?.let { b.ozellikBirimi(it.o1).goster(it.v1).kisa() } ?: "") }
    var v2 by remember(duzenlenen) { mutableStateOf(duzenlenen?.let { b.ozellikBirimi(it.o2).goster(it.v2).kisa() } ?: "") }
    var debi by remember(duzenlenen) { mutableStateOf(b.birim(Buyukluk.DEBI).goster(duzenlenen?.debi ?: 150.0).yaz(1)) }
    LaunchedEffect(taslak) { taslak?.let { o1 = Ozellik.KURU; o2 = Ozellik.MUTLAK; v1 = b.ozellikBirimi(Ozellik.KURU).goster(it.t).yaz(1); v2 = b.ozellikBirimi(Ozellik.MUTLAK).goster(it.W).yaz(4); taslakKullanildi() } }

    val si1 = v1.sayi()?.let { b.ozellikBirimi(o1).siye(it) }; val si2 = v2.sayi()?.let { b.ozellikBirimi(o2).siye(it) }
    val debiSi = debi.sayi()?.let { b.birim(Buyukluk.DEBI).siye(it) }
    val sonuc = if (si1 != null && si2 != null) runCatching { coz(o1, si1, o2, si2, vm.proje.basinc) } else null
    val hata = sonuc?.exceptionOrNull()?.message ?: if (debiSi != null && debiSi <= 0) "Debi 0'dan büyük olmalı" else null
    SideEffect { vm.onizleme = sonuc?.getOrNull() }
    DisposableEffect(Unit) { onDispose { vm.onizleme = null } }
    val gecerli = sonuc?.isSuccess == true && debiSi != null && debiSi > 0 && ad.isNotBlank() &&
        (duzenlenen != null || vm.proje.noktalar.none { it.ad == ad })
    fun nokta() = Nokta(ad.trim(), o1, si1!!, o2, si2!!, debiSi!!, duzenlenen?.renk ?: com.psikrometri.app.model.MAVI)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(ad, { ad = it }, label = { Text("Nokta adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        for ((o, setO, v, setV) in listOf(Quad(o1, { x: Ozellik -> o1 = x }, v1, { x: String -> v1 = x }), Quad(o2, { x: Ozellik -> o2 = x }, v2, { x: String -> v2 = x })))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SecimKutusu(Ozellik.entries, o, { it.etiket }, setO, Modifier.weight(1.2f))
                SayiAlani(v, setV, "Değer", b.ozellikBirimi(o).etiket, Modifier.weight(1f))
            }
        SayiAlani(debi, { debi = it }, "Debi", b.birim(Buyukluk.DEBI).etiket, Modifier.fillMaxWidth())
        hata?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ vm.proje = vm.proje.copy(noktalar = vm.proje.noktalar + nokta()); vm.secili = null }, enabled = gecerli && duzenlenen == null) { Text("Ekle") }
            Button({ vm.proje = vm.proje.copy(noktalar = vm.proje.noktalar.map { if (it.ad == duzenlenen!!.ad) nokta() else it }) }, enabled = gecerli && duzenlenen != null) { Text("Güncelle") }
            OutlinedButton({ vm.proje = vm.proje.copy(noktalar = vm.proje.noktalar.filter { it.ad != duzenlenen!!.ad }); vm.secili = null }, enabled = duzenlenen != null) { Text("Sil") }
        }
        HorizontalDivider()
        for (n in vm.proje.noktalar) {
            Card(Modifier.fillMaxWidth().clickable { vm.secili = if (vm.secili == n.ad) null else n.ad },
                colors = CardDefaults.cardColors(containerColor = if (vm.secili == n.ad) Color(0xFFBBDEFB) else Color(0xFFF5F5F5))) {
                Text("${n.ad}  ${n.o1.etiket} ${b.ozellikBirimi(n.o1).goster(n.v1).yaz(if (n.o1 == Ozellik.MUTLAK) 4 else 1)} ${b.ozellikBirimi(n.o1).etiket} · " +
                    "${n.o2.etiket} ${b.ozellikBirimi(n.o2).goster(n.v2).yaz(if (n.o2 == Ozellik.MUTLAK) 4 else 1)} ${b.ozellikBirimi(n.o2).etiket}" +
                    (vm.hesap.noktaHatalari[n.ad]?.let { "  ⚠ $it" } ?: ""), Modifier.padding(8.dp))
            }
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
```

- [ ] **Step 2: IslemlerSekmesi**

```kotlin
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
import com.psikrometri.app.model.Islem
import com.psikrometri.app.model.RENKLER
import com.psikrometri.app.psy.*

fun Kapasite.metin(): String = listOfNotNull(
    toplam?.let { "Toplam ${it.yaz(2)} kW" }, duyulur?.let { "Duyulur ${it.yaz(2)} kW" }, gizli?.let { "Gizli ${it.yaz(2)} kW" },
    shf?.let { "SHF %${it.yaz(0)}" }, isitici?.let { "Isıtıcı ${it.yaz(2)} kW" }, nemKgH?.let { "Nem ${it.yaz(2)} kg/h" },
    cikisT?.let { "Üfleme ${it.yaz(1)} °C" }, bypass?.let { "BF %${it.yaz(1)}" }).joinToString(" · ")

@Composable
fun IslemlerSekmesi(vm: AnaVM) {
    var secIndex by remember { mutableStateOf<Int?>(null) }
    val duzenlenen = secIndex?.let { vm.proje.islemler.getOrNull(it) }
    var tur by remember(duzenlenen) { mutableStateOf(duzenlenen?.tur ?: IslemTuru.KARISIM) }
    val noktaAdlari = vm.hesap.hava.keys.toList()
    var girdiler by remember(duzenlenen, tur) { mutableStateOf(duzenlenen?.girdiler ?: List(tur.girdiSayisi) { noktaAdlari.getOrElse(it) { "" } }) }
    val varsayilan = mapOf(Parametre.KW to "5", Parametre.SHF to "75", Parametre.KGH to "5", Parametre.BF to "10", Parametre.BUHAR to "1", Parametre.ARA to "1")
    var param by remember(duzenlenen, tur) { mutableStateOf(duzenlenen?.param?.map { it.kisa() } ?: tur.parametreler.map { varsayilan.getValue(it) }) }
    var renk by remember(duzenlenen) { mutableStateOf(duzenlenen?.renk ?: RENKLER[0]) }

    fun islem(): Islem? {
        val p = param.map { it.sayi() ?: return null }
        if (girdiler.any { it.isBlank() }) return null
        val ilk = vm.proje.yeniPPAdi().removePrefix("PP").toInt()
        val cikti = duzenlenen?.ciktilar ?: List(tur.ciktiSayisi) { "PP${ilk + it}" }
        return Islem(tur, girdiler, cikti, p, renk)
    }
    val onizleme = islem()?.let { i -> runCatching { islemHesapla(i.tur, i.girdiler.map { vm.hesap.hava.getValue(it) }, i.param) } }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                else -> SayiAlani(param[i], { v -> param = param.toMutableList().also { it[i] = v } }, pr.etiket, pr.birim, Modifier.fillMaxWidth())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RENKLER.forEach { r -> Box(Modifier.size(28.dp).clip(CircleShape).background(Color(r)).border(if (r == renk) 3.dp else 0.dp, Color.Black, CircleShape).clickable { renk = r }) }
        }
        onizleme?.let { o -> Text(o.fold({ it.kapasite.metin() }, { it.message ?: "" }), color = if (o.isFailure) MaterialTheme.colorScheme.error else Color.Unspecified) }
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
                    Row { Box(Modifier.size(14.dp).background(Color(i.renk))); Text("  ${i.tur.etiket}: ${(i.girdiler + i.ciktilar).joinToString(", ")}") }
                    r?.let { Text(it.fold({ s -> s.kapasite.metin() }, { e -> "⚠ ${e.message}" }), color = if (it.isFailure) MaterialTheme.colorScheme.error else Color.Unspecified) }
                }
            }
        }
    }
}
```

- [ ] **Step 3: VeriSekmesi**

```kotlin
package com.psikrometri.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.psikrometri.app.model.Buyukluk
import com.psikrometri.app.psy.Ozellik

@Composable
fun VeriSekmesi(vm: AnaVM) {
    val ad = vm.secili
    val h = ad?.let { vm.hesap.hava[it] }
    val b = vm.ayarlar.birimler
    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        if (h == null) { Text("Diyagramda ya da Noktalar sekmesinde bir nokta seçin."); return@Column }
        val d = h.d
        val tb = b.birim(Buyukluk.SICAKLIK)
        val satirlar = listOf(
            "Kuru termometre" to "${tb.goster(d.t).yaz(1)} ${tb.etiket}", "Yaş termometre" to "${tb.goster(d.twb).yaz(1)} ${tb.etiket}",
            "Çiy noktası" to "${tb.goster(d.td).yaz(1)} ${tb.etiket}", "Bağıl nem" to "%${(d.rh * 100).yaz(1)}",
            "Mutlak nem" to "${b.birim(Buyukluk.NEM).goster(d.Wr).yaz(4)} ${b.birim(Buyukluk.NEM).etiket}",
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
                if (bas != null && son != null) Text("Δt ${(son.t - bas.t).yaz(1)} · ΔW ${(son.Wr - bas.Wr).yaz(4)} · Δh ${(son.h - bas.h).yaz(1)}")
                Text(r.kapasite.metin())
            }
        }
    }
}
```

`AnaEkran.kt`'deki geçici `NoktalarSekmesi`, `IslemlerSekmesi`, `VeriSekmesi` satırlarını sil.

- [ ] **Step 4: Emülatörde dene**

Run: `./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.psikrometri.app/.MainActivity`
Kontrol (her biri ekran görüntüsüyle): P1 27 °C/0,0100 ve P2 15 °C/0,0080 ekle → Hava karışımı P1+P2 → PP1 20,9 °C; P1'i 30 °C yap → PP1 kayar; RH 120 gir → uyarı, Ekle pasif; Soğutma kapasitesi 50 kW → kırmızı hata; Veri sekmesinde P1: yaş 18,6 / çiy 14,1 / h 52,7.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): noktalar, işlemler ve veri sekmeleri"
```

---

### Task 12: Kaydet/Aç, PDF rapor, Ayarlar diyaloğu

**Files:**
- Create: `app/src/main/java/com/psikrometri/app/rapor/PdfRapor.kt`, `app/src/main/java/com/psikrometri/app/ui/Dosya.kt`
- Modify: `ui/AnaEkran.kt` (geçici `AyarDiyalog`, `DosyaIslemleri`, `dosyaIslemleri` silinir)

**Interfaces:**
- Consumes: Task 7–11.
- Produces: `fun pdfYaz(cikti: java.io.OutputStream, proje: Proje, hesap: Hesap, egriler: List<Egri>, ayarlar: Ayarlar)`; `class DosyaIslemleri(val ac: () -> Unit, val kaydet: () -> Unit, val rapor: () -> Unit)`; `@Composable fun dosyaIslemleri(vm: AnaVM): DosyaIslemleri`; `@Composable fun AyarDiyalog(vm: AnaVM, kapat: () -> Unit)`

- [ ] **Step 1: PDF**

```kotlin
package com.psikrometri.app.rapor

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.psikrometri.app.model.Hesap
import com.psikrometri.app.model.Proje
import com.psikrometri.app.ui.*
import java.io.OutputStream

fun pdfYaz(cikti: OutputStream, proje: Proje, hesap: Hesap, egriler: List<Egri>, ayarlar: Ayarlar) {
    val pdf = PdfDocument()
    // Sayfa 1: A4 yatay diyagram
    var sayfa = pdf.startPage(PdfDocument.PageInfo.Builder(842, 595, 1).create())
    sayfa.canvas.scale(0.5f, 0.5f)
    cizDiyagram(sayfa.canvas, Eksen(ayarlar.tMin, ayarlar.tMax, ayarlar.wMax, 40f, 40f, 1560f, 1060f), proje.basinc, egriler, hesap, proje, null, null, ayarlar.birimler)
    pdf.finishPage(sayfa)
    // Sonraki sayfalar: tablo
    val yazi = Paint().apply { textSize = 10f }
    val kalin = Paint().apply { textSize = 11f; isFakeBoldText = true }
    var no = 2; var y = 0f
    fun yeniSayfa() { sayfa = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, no++).create()); y = 40f }
    fun satir(s: String, p: Paint = yazi) { if (y > 800) { pdf.finishPage(sayfa); yeniSayfa() }; sayfa.canvas.drawText(s, 40f, y, p); y += 15f }
    yeniSayfa()
    satir("Psikrometri raporu — rakım ${proje.rakim.yaz(0)} m, basınç ${(proje.basinc / 1000).yaz(3)} kPa", kalin)
    satir("Nokta   t °C   twb °C   td °C   RH %   W kg/kg   h kJ/kg   ρ kg/m³   v m³/kg   debi l/s", kalin)
    for ((ad, hv) in hesap.hava) {
        val d = hv.d
        satir("%-6s %6s %7s %7s %6s %9s %8s %8s %8s %8s".format(ad, d.t.yaz(1), d.twb.yaz(1), d.td.yaz(1), (d.rh * 100).yaz(1), d.Wr.yaz(4), d.h.yaz(1), d.rho.yaz(3), d.v.yaz(3), hv.debi.yaz(1)))
    }
    y += 10f; satir("İşlemler", kalin)
    proje.islemler.forEachIndexed { i, isl ->
        satir("${isl.tur.etiket}: ${(isl.girdiler + isl.ciktilar).joinToString(", ")}")
        hesap.sonuclar[i].fold({ satir("    " + it.kapasite.metin()) }, { satir("    Hata: ${it.message}") })
    }
    pdf.finishPage(sayfa)
    pdf.writeTo(cikti); pdf.close()
}
```

- [ ] **Step 2: Dosya işlemleri + Ayarlar diyaloğu**

`ui/Dosya.kt`:
```kotlin
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
        ctx.contentResolver.openOutputStream(uri)!!.use { it.write(vm.proje.json().toByteArray()) }
        Toast.makeText(ctx, "Kaydedildi", Toast.LENGTH_SHORT).show()
    }
    val ac = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { Proje.jsonOku(ctx.contentResolver.openInputStream(uri)!!.bufferedReader().readText()) }
            .onSuccess { vm.proje = it; vm.secili = null }
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
```
(`import androidx.compose.ui.unit.dp` ekle.) `AnaEkran.kt`'deki geçici `AyarDiyalog`, `DosyaIslemleri`, `dosyaIslemleri` satırlarını sil.

- [ ] **Step 3: Emülatörde dene**

Run: `./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`
Kontrol: Task 11'deki projeyi Kaydet → Yeni → Aç → aynı noktalar/işlemler geri gelir; Rapor → paylaş penceresi açılır, PDF'i `adb shell ls /data/data/com.psikrometri.app/cache/rapor/` ile gör, `adb pull` ile çek ve aç (diyagram + tablo); Ayarlar → °F seç → eksen ve Veri sekmesi °F.

- [ ] **Step 4: Testler hâlâ yeşil**

Run: `./gradlew :app:testDebugUnitTest` → exit 0.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: kaydet/aç, PDF rapor, ayarlar"
```

---

### Task 13: Kabul — Daikin ile yan yana, APK

**Files:**
- Modify: `docs/daikin-bulgular.md` (son durum), hafıza `psikrometri.md`

- [ ] **Step 1: Yan yana kıyas**

`tools/daikin/veri/test1.ppp` projesini (P1–P3 + 11 işlem) uygulamada aynı adlarla elle kur (ya da `.ppp`→`.psy` için kısa bir Python dönüştürücü yaz: `Proje` JSON şemasına `o1/o2` adları `Ozellik.entries[kod].name`, tür `IslemTuru` kod eşlemesi). Uygulamanın ekran görüntüsü (`adb exec-out screencap -p`) ile Daikin ekran görüntüsünü (`tools/daikin/dk.ps1` → `Shot`) yan yana koy; nokta konumları ve işlem çizgileri aynı yerde olmalı. Farkları listele, kullanıcıya göster.

- [ ] **Step 2: Release APK**

Run: `./gradlew :app:assembleRelease && cp app/build/outputs/apk/release/app-release.apk "C:/Users/User/Desktop/Psikrometri.apk"`
Expected: exit 0, `Desktop\Psikrometri.apk` var.

- [ ] **Step 3: Commit + hafıza**

```bash
git add -A && git commit -m "release: 1.0"
```
Hafıza `psikrometri.md`: durum "1.0 APK masaüstünde", açık kalan fark varsa listesi.

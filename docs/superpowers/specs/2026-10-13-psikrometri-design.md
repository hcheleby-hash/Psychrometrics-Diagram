# Psikrometri — Android psikrometrik diyagram uygulaması

Tarih: 2026-10-13 · Durum: tasarım onaylandı, plan bekleniyor

## Amaç

Daikin Europe "Psychrometric Diagram Viewer 4.1.0" (Delphi, `Desktop\psychrometrics.exe`) masaüstü programının Android karşılığı. Kullanıcı iki özellik seçip değer girer, noktanın tüm özellikleri hesaplanır; noktalar diyagramda gösterilir, 15 işlem ile yeni noktalar türetilip çizgiyle bağlanır.

**Kabul kriteri:** Durum hesapları ve 15 işlemin sonuçları, Daikin'den alınan referans tablosuyla (`daikin_referans.json`) Daikin'in gösterdiği hassasiyetin yarısı toleransla aynı çıkar.

Daikin adı/logosu kullanılmaz.

## Kapsam

Tek sürümde hepsi:
- 7 özellikten herhangi ikisiyle nokta tanımı + debi
- 15 işlem (aşağıdaki tablo)
- Rakım / basınç
- Kaydet / Aç (`.psy`)
- PDF rapor + paylaş
- Birim seçimi

Platform: yalnız Android. Kotlin + Jetpack Compose, tek modül, paket `com.psikrometri.app`. Ek bağımlılık yok (JSON: `org.json`, PDF: `android.graphics.pdf.PdfDocument`, ayar: DataStore).

## Hesap çekirdeği (`psy/`, saf Kotlin)

Formüller exe'deki 80-bit sabitlerden çıkarıldı (ASHRAE Fundamentals Bölüm 1; buhar cp = **1.84**, ASHRAE'deki 1.86 değil). T = t + 273.15.

| Büyüklük | Formül |
|---|---|
| pws, t ≥ 0 [Pa] | ln pws = −5800.2206/T + 1.3914993 − 0.048640239·T + 4.1764768e-5·T² − 1.4452093e-8·T³ + 6.5459673·ln T |
| pws, t < 0 [Pa] | ln pws = −5674.5359/T + 6.3925247 − 9.677843e-3·T + 6.2215701e-7·T² + 2.0747825e-9·T³ − 9.484024e-13·T⁴ + 4.1635019·ln T |
| Nem oranı | W = 0.6219779385·pw / (p − pw) |
| Yaş termometre t\*, t\* ≥ 0 | W = [(2501 − 2.346·t\*)·Ws\* − 1.006·(t − t\*)] / (2501 + 1.84·t − 4.186·t\*) — t\* iteratif, tolerans 1e-4 |
| Yaş termometre, t\* < 0 | W = [(2834.4 − 0.26·t\*)·Ws\* − 1.006·(t − t\*)] / (2834.4 + 1.84·t − 2.1·t\*) |
| Çiy noktası | α = ln(pw[kPa]); td = 6.54 + 14.526α + 0.7389α² + 0.09486α³ + 0.4569·pw[kPa]^0.1984; td < 0 ise td = 6.09 + 12.608α + 0.4959α² |
| Entalpi [kJ/kg] | h = 1.006·t + W·(2501 + 1.84·t) |
| Özgül hacim [m³/kg] | v = 287.0551882·T·(1 + 1.607774067·W) / p |
| Yoğunluk [kg/m³] | ρ = (1 + W) / v |

Basınç–rakım ilişkisi ve yoğunluk tanımı referans verilerle doğrulanacak (exe'de sabit olarak bulunmadı).

- `Psikro.kt`: `Durum(t, W)` ve türetilen özellikler.
- `Cozucu.kt`: 7 özellik (kuru term., mutlak nem, bağıl nem, entalpi, yaş term., özgül hacim, yoğunluk) → 21 ikili. İkiliden biri t veya W ise tek boyutlu bisection; değilse dışta t, içte W iç içe bisection. Aynı özellik iki kez seçilirse hata.
- `Islemler.kt`: 15 işlem; girdi noktaları + parametre → yeni noktalar (PP1, PP2) + sonuçlar (kW, kg/h).

Python ön uygulaması: `tools/daikin_psikro.py`.

## İşlemler

Daikin arayüzünden çıkarılan girdi/parametre şeması. Anlamları referans verilerden çözülecek; çözülemeyen kullanıcıya sorulacak, varsayım yapılmayacak.

| İşlem | Girdi | Parametre | Çıktı |
|---|---|---|---|
| Show point | P1 | – | – |
| Connect | P1, P2 | – | çizgi |
| Mix air | P1, P2 | – | PP1 |
| Heating | P1 → P2 | nemlendirme: buhar / su | PP1 |
| Heating capacity | P1 | kW | PP1 |
| Cooling | P1 → P2 | ara çizgiler | PP1 |
| Cooling capacity | P1 | kW, SHF %, ara çizgiler | PP1, PP2 |
| Cool to saturation | P1 | SHF %, ara çizgiler | PP1, PP2 |
| Sensible heat factor | P1 | SHF % | PP1 |
| Dehumidification | P1 | su kg/h | PP1 |
| Steam humidification | P1 | buhar kg/h | PP1 |
| Water humidification | P1 | su kg/h | PP1 |
| Bypass factor | P1 | kW, SHF % | PP1, PP2 |
| Discharge | P1 | SHF %, bypass faktörü % | PP1, PP2 |
| Condensate | P1, P2 | – | yoğuşma miktarı |

Bir noktanın değişmesi ona bağlı işlemleri yeniden hesaplar (Daikin'in davranışı referans sırasında kontrol edilecek).

## Model (`model/`)

`Nokta(ad, durum, debi, renk)`, `Islem(tur, girdiler, parametreler, renk, araCizgi)`, `Proje(rakim, noktalar, islemler)`. Proje ↔ JSON dönüşümü burada. Tüm UI tek `Proje` durumundan beslenir.

## Ekran (`ui/`)

Bölünmüş ekran: üstte diyagram, altta sekmeler **Noktalar / İşlemler / Veri**. Yatayda diyagram tam ekran.

**Diyagram (Compose Canvas):**
- X: kuru term. −40…65 °C; Y (sağ): mutlak nem 0…0.055 kg/kg. Aralıklar Ayarlar'dan değişir.
- Eğriler: RH %10–100, entalpi, yaş term., özgül hacim, yoğunluk, kuru term./mutlak nem ızgarası. Her biri ayrı aç/kapa. Doyma eğrisinin üstü kırpılır.
- Noktalar renkli + ad; dokununca seçilir.
- İşlem çizgileri işlem renginde, yönlü ok; ara çizgiler kesikli.
- İki parmakla zoom/pan, çift dokunuş sıfırlar. Basılı tutunca o konumun değerleri balonda; balondan "Nokta ekle".

**Noktalar:** ad, 2 özellik combobox'ı + değer, debi, Ekle/Düzenle/Sil. Yazarken nokta diyagramda canlı kayar. Altta nokta kartları.

**İşlemler:** tür combobox'ı, türe göre değişen alanlar, renk, ara çizgi; Ekle/Düzenle/Sil; işlem listesi.

**Veri:** seçili noktanın tüm özellikleri + bağlı işlemlerin Δt, ΔW, Δh, kW, kg/h.

**Üst çubuk:** rakım (m) ↔ basınç (kPa), menü: Yeni, Aç, Kaydet, Rapor, Ayarlar.

**Hatalar:** RH > %100, W > doyma, aynı özellik iki kez, debi ≤ 0 → alan altında uyarı, Ekle pasif. Çözülemeyen işlem listede kırmızı + neden.

## Kaydet / Rapor / Ayarlar

- Kaydet/Aç: `.psy` (JSON), Android dosya seçici (SAF).
- Rapor: PDF — diyagram, nokta tablosu, işlem sonuçları; paylaş menüsü.
- Ayarlar (DataStore): °C/°F, kg/kg–g/kg, l/s–m³/h, kJ/kg–kcal/kg, kPa–Pa–mbar; eksen aralıkları; varsayılan eğri görünürlüğü.

## Doğrulama

- **Referans üretimi:** Daikin penceresi Win32 mesajları + gerçek tıklama/klavye ile sürülür (değer alanları ancak odak değişince işleniyor; `WM_SETTEXT` yetmiyor, tıkla + Ctrl+A + değer + Tab gerekli). Kullanıcı bu sırada bilgisayarı kullanmaz.
  - 21 özellik ikilisi × 3 girdi (Point Data paneli)
  - 15 işlem × 3–4 senaryo (sıfır altı/üstü, farklı SHF/kW/kg/h)
  - Rakım 0 m ve 1500 m
  - → `app/src/test/resources/daikin_referans.json`
- **Testler:** JUnit (JVM), her referans satırı; tolerans = Daikin gösterim hassasiyetinin yarısı. Test JVM'i `-Duser.language=en -Duser.country=US` (tr-TR tuzağı).
- **Elle kabul:** emülatörde nokta ekleme, her işlem türü, kaydet/aç, PDF; ekran görüntüsü Daikin ile yan yana.
- **Çıktı:** `Desktop\Psikrometri.apk`.

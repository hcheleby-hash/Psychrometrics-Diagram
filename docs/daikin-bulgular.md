# Daikin Psychrometric Diagram Viewer 4.1.0 — bulgular

Kaynak: `Desktop\psychrometrics.exe` (Delphi 32-bit). Ham veri: `tools/daikin/veri/` (`.ppp` girdiler, `.rtf` raporlar, `.txt` düz metin).

## Formüller (kesin)

Exe'deki 80-bit sabitler + `.ppp`'deki 15 haneli RH değerleri ile 1e-9 düzeyinde doğrulandı (p = 101325 Pa). Ayrıntı: `docs/superpowers/specs/2026-10-13-psikrometri-design.md`.

Yoğunluk: ρ = (1 + W) / v (P1 27 °C / 0,0100 → 1,169 ✓).

## Dosya formatları

**`.ppp`** — UTF-16 LE (BOM), CRLF, alanlar boşlukla, ondalık virgül.

- Nokta: `1 <ad> <özellik1> <değer1> <özellik2> <değer2> <debi l/s> <t önbellek> <RH% önbellek>`
  - Özellik indeksleri: 0 kuru term., 1 mutlak nem, 2 bağıl nem, 3 entalpi, 4 yaş term., 5 özgül hacim, 6 yoğunluk
- İşlem: `2 <renk BGR int> <tür> <girdiler...> <çıktılar...> <parametreler...>`
- Türetilen PP noktaları dosyaya yazılmaz, açılışta hesaplanır.
- **Rakım `.ppp`'ye yazılmaz.** 1500 m ile kaydedilen proje bayt bayt aynı (rakımsız) çıktı verdi; boş projeyi kaydetmek 2 baytlık (BOM) dosya üretir. Açılışta Altitude 0'a döner; rakım, dosya açıldıktan sonra Altitude alanına (≈335,69) `Yaz` ile girilir, rapor o rakımla alınır. Rakım değişince nokta RH'si korunur (W, h, ρ yeniden hesaplanır). 1500 m → 84555,9 Pa = `101325·(1−2,25577e-5·z)^5,2559` (Kotlin `basincRakimdan` ile birebir).
- **Önbellek alanları (8. t, 9. RH%) esastır:** özellik1/özellik2 değerleri yerine bunlar kullanılıyor. `0 0` yazılan nokta raporda 0 °C / 0 % çıktı. Yani `ppp_yaz.nokta` önbelleği doğru yazmalı; nokta tanımındaki (özellik, değer) çifti rapora etki etmiyor.

| Kod | İşlem | Satır şablonu |
|---|---|---|
| 0 | Show point | `0 P1` |
| 1 | Connect | `1 P1 P2` |
| 2 | Mix air | `2 P1 P2 PP` |
| 3 | Cooling | `3 P1 P2 PP` |
| 4 | Heating | `4 P1 P2 PP <1=buhar,0=su> <ara çizgi 0/1>` |
| 5 | Cooling capacity | `5 P1 PP1 PP2 <kW> <SHF%> <ara çizgi>` |
| 6 | Heating capacity | `6 P1 PP PP <kW>` |
| 7 | Steam humidification | `7 P1 PP PP <kg/h>` |
| 8 | Water humidification | `8 P1 PP PP <kg/h>` |
| 9 | Cool to saturation | `9 P1 PP1 PP2 0 <SHF%> <ara çizgi>` |
| 10 | Condensate | `10 P1 P2` |
| 11 | Dehumidification | `11 P1 PP PP <kg/h>` |
| 12 | Bypass factor | `12 P1 PP1 PP2 <kW> <SHF%>` |
| 13 | Discharge | `13 P1 PP1 PP2 <SHF%> <BF%>` |
| 14 | Sensible heat factor | `14 P1 PP PP <SHF%>` |

**Rapor** — `Report...` → `%TEMP%\Psychrometrics_<yyyymmddNN>.rtf`; tüm noktalar (t 0,1; W 0,0001; RH 0,1; h 0,1; ρ 0,001; v 0,001) + işlem kapasiteleri (0,01 kW / kg/h).

## İşlem davranışı (gözlem, `rapor1/3/4.txt`)

Tanımlar: m = 1,2 · Q/1000 kg/s (kapasitelerde standart yoğunluk); cpm(W) = 1,006 + 1,84·W.
Toplam = m·(h_a − h_b); duyulur = m·cpm(W_b)·(t_a − t_b); gizli = toplam − duyulur.

| İşlem | Davranış | Durum |
|---|---|---|
| Mix air | gerçek kütle (ρ·Q) ağırlıklı W ve h; debi = ΣQ | ✓ 20,9 °C / 0,0090 |
| Cooling P1→P2 | PP = (t1, W2) ara nokta | ✓ |
| Heating P1→P2 buhar | PP = (t2, W1): ısıt, sonra buhar | ✓ |
| Heating P1→P2 su | PP = (W1, h = h2): entalpiye kadar ısıt, sonra adyabatik | ✓ 29,5 °C |
| Heating capacity | W sabit, Δt = kW / (m·cpm(W1)) | ✓ 54,1 °C |
| Steam humidification | t sabit, ΔW = (kg/h/3600)/m; gizli = kg/s·(2501+1,84·t1) | ✓ |
| Water humidification | h sabit, ΔW = (kg/h/3600)/m; gizli = kg/s·2501 | ✓ 23,2 °C |
| Dehumidification | h sabit, W azalır; gizli = kg/s·2501 | ✓ 46,5 °C |
| Cooling capacity | duyulur = SHF·kW → Δt; gizli = (1−SHF)·kW → ΔW = gizli/(m·(2501+1,84·t1)); PP1 son, PP2 = (t1, W_son); doymayı aşarsa hata | ✓ 18,9 °C / 0,0089 |
| Cool to saturation | ADP: doyma eğrisinde cpm(W_x)·(t1 − t_x)/(h1 − h_x) = SHF, t1'e en yakın kök; PP1 = ADP, PP2 = (t1, W_ADP); kök yoksa hata | ✓ 5 senaryo ≤ 0,04 °C |
| Discharge | ADP (Cool to saturation ile aynı) ; PP1 = ADP + BF·(P1 − ADP) (t ve W); PP2 = ADP; toplam = duyulur/SHF | ✓ 12,0 °C |
| Condensate | iki nokta da RH=100 olmalı; kapasite + nem alma kg/h | kg/h'te ~%1 fark — referans testte çözülecek |
| Sensible heat factor | P1'den SHF doğrusu doymaya (ya da W=0'a); **Cool to saturation'dan farklı doğru** (75 %: 10,0 °C vs 10,3 °C) | ✗ en iyi aday ~0,1 °C sapıyor (`tools/shf_uydur.py`) |
| Bypass factor | son nokta Cooling capacity ile aynı; PP2 = SHF işlemindeki doyma kesişimi; BF = (t_son − t_ADP)/(t1 − t_ADP) | SHF doğrusuna bağlı |
| Connect / Show point | sadece çizim | ✓ |

## Daikin'i sürmek (`tools/daikin/dk.ps1`)

- `WM_SETTEXT` değer alanını görsel olarak değiştirir ama program okumaz → gerçek tık + `^a` + değer + `{TAB}` (`Yaz x y metin`).
- Düğme: `PostMessage BM_CLICK` (`PClick`); `SendMessage` mesaj kutusu açılınca takılır.
- Mesaj kutuları `TBaseMsg`; `DialogKapat` sadece onları kapatır (dosya penceresi `#32770`, dokunma).
- Adı olan projede "Save As..." doğrudan kaydeder. Açmak: `RealClick 50 179` + yol + Enter. Rapor: `Rapor <hedef>`.
- Koordinatlar 1920×1080 sol monitör, pencere tam ekran.
- Rapor/oturum tuzakları: `Ac` PowerShell takma adı (Add-Content) olduğundan fonksiyon `AcDosya` adıyla; Report… Windows'un "birlikte aç" kutusunu açar ve Word ilk rapor dosyasını kilitlerse sonraki raporlar "Cannot create file" verir (`Rapor` artık eski rtf'yi siler ve Word'ü kapatır). Daikin simge durumundaysa `RealClick` başka pencereye tıklar (dk.ps1 başlangıçta geri getirir).
- Başarısız işlemler (kök yok, doymayı aşan kapasite) raporda hiç listelenmez; mesaj kutusu her zaman çıkmaz (ref_islem'de Cooling capacity N sessizce düştü; ref_rakim'de 1500 m'de bir işlem TBaseMsg verdi).

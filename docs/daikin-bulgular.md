# Daikin Psychrometric Diagram Viewer 4.1.0 — bulgular

Kaynak: `Desktop\psychrometrics.exe` (Delphi 32-bit). Ham veri: `tools/daikin/veri/` (`.ppp` girdiler, `.rtf` raporlar, `.txt` düz metin).

## Formüller (kesin)

Exe'deki 80-bit sabitler + `.ppp`'deki 15 haneli RH değerleri ile 1e-9 düzeyinde doğrulandı (p = 101325 Pa). Ayrıntı: `docs/superpowers/specs/2026-10-13-psikrometri-design.md`.

Yoğunluk: ρ = (1 + W) / v (P1 27 °C / 0,0100 → 1,169 ✓).

## Dosya formatları

**`.ppp`** — UTF-16 LE (BOM), CRLF, alanlar boşlukla, ondalık virgül.

- Nokta: `1 <ad> <özellik1> <değer1> <özellik2> <değer2> <debi l/s> <t önbellek> <RH% önbellek>`
- ref_durum girdileri tam hassasiyetle yazıldı; Daikin önbellekten hesapladığı için rapor geçerli kaldı (yeniden koşturulmadı).
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
| Mix air | kuru hava kütlesi (Q/v) ağırlıklı, yuvarlanmamış W ve h (exe 0x8449dc); td gösterimde Wr'den | ✓ (Görev 6) |
| Cooling P1→P2 | PP = (t1, W2) ara nokta | ✓ |
| Heating P1→P2 buhar | PP = (t2, W1): ısıt, sonra buhar | ✓ |
| Heating P1→P2 su | PP = (W1, h = h2): entalpiye kadar ısıt, sonra adyabatik | ✓ 29,5 °C |
| Heating capacity | W sabit, Δt = kW / (m·cpm(W1)) | ✓ 54,1 °C |
| Steam humidification | t sabit, ΔW = (kg/h/3600)/m; gizli = kg/s·(2501+1,84·t1) | ✓ |
| Water humidification | h sabit, ΔW = (kg/h/3600)/m; gizli = kg/s·2501 | ✓ 23,2 °C |
| Dehumidification | h sabit, W azalır; gizli = kg/s·2501 | ✓ 46,5 °C |
| Cooling capacity | ✓ exe 0x9b3b34: t2 = t1 − SHF·kW/(cpm(W1r)·1,2·Q), W2 = W1r − (1−SHF)·kW/(2500·Q·ρ_kuru(t1)), ρ_kuru = p/(R_da·(t1+273,15)), Q m³/s; W2 ≤ 0 ya da RH2 > 100 hata; PP2 = (t1, round5(W2)) | ✓ (Görev 6) |
| Cool to saturation | ✓ exe 0x9b40a4: tx = td(W1r)'den 5 °C adımla aşağı; Wx = round5(Ws(tx)); T = 1,2·Q·\|hx − h1\|; oran = cpm(Wx)·1,2·Q·\|t1 − tx\|/T; oran < SHF ise geri dön ve adımı yarıla; \|SHF − oran\| ≤ 1e-4 (ya da adım < 1e-10) dur; tx ≤ −40 ya da Wx ≤ 0 hata. Kapasite: T (son hesaplanan), S = SHF·T, L = T − S | ✓ (Görev 6) |
| Discharge | ✓ ADP ve T_ADP Cool to saturation'dan; T = T_ADP − BF·T_ADP; PP1 = T kW'lık Cooling capacity sonu, PP2 = ADP; S = SHF·T; Discharge = t(PP1) | ✓ (Görev 6) |
| Condensate | iki nokta da RH=100 olmalı; kapasite + nem alma kg/h | ✓ W1, W2 önce 5 haneye yuvarlanır (Görev 5) |
| Sensible heat factor | ✓ exe 0x9b4384: Cooling capacity doğrusu (Q = 0,1 m³/s sabit), kapasite 0'dan 1 kW adımla artar; RH > 100 ya da W ≤ 0 olunca adım geri alınır ve /10; adım < 1e-4'te biter. RH > 100 adımında nokta yazılır (gösterimde W doymaya kırpılır), W ≤ 0 adımında yazılmaz. Cool to saturation'dan farklı: o h-tabanlı oranla ADP arar | ✓ ref_shf 32/32 (Görev 6) |
| Bypass factor | ✓ PP1 = Cooling capacity sonu; PP2 = exe 0x9b3d30: aynı doğruda gerçek Q ile kapasite kW'tan doymaya (RH düşerse ya da W ≤ 0 hata); BF = 100 − kW/T_doyma·100; Discharge = t(PP1) | ✓ (Görev 6) |
| Connect / Show point | sadece çizim | ✓ |

## Daikin'i sürmek (`tools/daikin/dk.ps1`)

- `WM_SETTEXT` değer alanını görsel olarak değiştirir ama program okumaz → gerçek tık + `^a` + değer + `{TAB}` (`Yaz x y metin`).
- Düğme: `PostMessage BM_CLICK` (`PClick`); `SendMessage` mesaj kutusu açılınca takılır.
- Mesaj kutuları `TBaseMsg`; `DialogKapat` sadece onları kapatır (dosya penceresi `#32770`, dokunma).
- Adı olan projede "Save As..." doğrudan kaydeder. Açmak: `RealClick 50 179` + yol + Enter. Rapor: `Rapor <hedef>`.
- Koordinatlar 1920×1080 sol monitör, pencere tam ekran.
- Rapor/oturum tuzakları: `Ac` PowerShell takma adı (Add-Content) olduğundan fonksiyon `AcDosya` adıyla; Report… Windows'un "birlikte aç" kutusunu açar ve Word ilk rapor dosyasını kilitlerse sonraki raporlar "Cannot create file" verir (`Rapor` artık eski rtf'yi siler ve Word'ü kapatır). Daikin simge durumundaysa `RealClick` başka pencereye tıklar (dk.ps1 başlangıçta geri getirir).
- Başarısız işlemler (kök yok, doymayı aşan kapasite) raporda hiç listelenmez; mesaj kutusu her zaman çıkmaz (ref_islem'de Cooling capacity N sessizce düştü; ref_rakim'de 1500 m'de bir işlem TBaseMsg verdi).
- ref_rakim'in tek `hatalar` girdisi ("crosses the 100%RH line") listelenen hiçbir işleme atfedilemez (8 işlemin hiçbiri SHF değil); tüketiciler üzerinde assert etmemeli.

## Task 5 referans doğrulaması (13 işlem, `psy/Islemler.kt`)

Referansla eşleşen tanımlar: Mix air kuru hava kütlesi (Q/v) ağırlıklı; Heating hedef daha soğuksa uçlar yer değiştirir (PP = (t_sıcak, W_soğuk), kapasiteler pozitif), kW b'nin debisiyle, kg/h ilk noktanın debisiyle, duyulur cpm(W_b); Cooling capacity: t cpm(W1) ile, W toplam enerjiden (yoğuşan su entalpisi 4.186·t1 düşülerek), kapasiteler istenen kW'ın kendisi; Discharge: duyulur cpm(W1)·(t1−t_c), toplam = duyulur/SHF, W aynı enerji dengesinden. Sulu ısıtma (PP entalpi sabit) ve Condensate (toplam = m·Δh) brief'teki halleriyle tutuyor; alternatif gerekmedi.
Kalan farklar (37 satır, çoğu 0,05 toleransın %0,1 ötesinde): `.superpowers/sdd/task-5-report.md`. ref_rakim'de Daikin altitude'u işlemlerden sonra değiştirdiği için türetilmiş noktalar (t, RH) korunarak yeniden hesaplanmış, kapasiteler deniz seviyesi değerinde kalmış; ref_islem'de X23–X40 Daikin'in varsayılan 25 °C/%50/100 l/s noktalarıdır (işlem çözülmemiş).
- ref_islem placeholder nedeni: Cooling capacity N (30 °C/0,012, 3 kW, SHF 90) çözülemedi, Daikin sessizce düşürdü; sonraki işlem çıktı adları (X21+) kayıp kaldı ve raporda X23+ varsayılan nokta (25 °C, %50, 100 l/s) çıktı. Satır 1,5 kW'a çekilince (çıktı adları aynı) X21–X40 gerçek değer aldı. Çözülemeyen işlem = sessiz düşme + sonraki adlar bozulur; senaryolarda tüm işlemler çözülebilir olmalı.
- Rakım değişince Daikin kullanıcı noktalarını yeniden hesaplar ama işlem çıktılarını (PP/X) HESAPLAMAZ (rapor eski kapasiteleri verir). Bir işlemi seçip Edit + Accept etmek tüm işlemleri yeniden hesaplatır; `referans_al.ps1` ref_rakim için bunu yapar (ör. Cooling A,E: 3,49 kW → 3,69 kW).

### Task 5 kabul edilen bulgular (analiz: `.superpowers/sdd/task-5-analiz.md`)

- m = 1,2·Q/1000 her yerde, 1500 m dahil (basınçtan bağımsız).
- 7 Steam / 8 Water humidification / 11 Dehumidification: W2 = round(W1 ± (kg/h/3600)/m, 5) (0,01 g/kg); sulu/nem almada t yuvarlanmamış W'den, gösterilen W yuvarlanmış.
- 10 Condensate: W1, W2 önce 5 haneye yuvarlanır, sonra T = m·Δh.
- 4 Heating: hedef daha soğuksa uçlar yer değiştirir (test1.PP7); kW b'nin debisiyle, kg/h ilk noktanınkiyle.
- Mix air: kuru hava kütlesi (Q/v) ağırlıklı.
- (Görev 6'da exe'den çözüldü, aşağıdaki bölüme bakın) Görev 5'te ertelenen: 2 (td ≤ 0,005 °C), 5, 9, 12, 13, 14 (SHF doğrusu ailesi; ADP kökü SHF'ye bağlı ±0,01 °C). Discharge kapasitesi: T = m(1−BF)(h1−h_ADP), S = SHF·T.

### Görev 6 — exe disassembly ile kesinleşen tanımlar

Kaynak: `Desktop\psychrometrics.exe` (capstone ile; VA = dosya ofseti + 0x400C00). İşlem dağıtıcısı 0x9b61e3 (`jmp [kod*4 + 0x9b61f3]`). Python kopyası: `.superpowers/sdd/ex.py` — 7 senaryonun 1581 karşılaştırmasında 0 sapma.

- **Gösterim/işlem kuralı (rapor 0x9b8ec0, panel 0x9b7f44):** nokta (t, RH) olarak saklanır; W = W(t, RH kırpılmış 0–100); Wr = round(W·1e5)/1e5. W, h, twb, td **Wr'den**; RH saklanan değer; v, ρ yuvarlanmamış W'den. İşlemler de çoğunlukla W1r kullanır. Bu, 1500 m'deki D.h (0,95) ve S2.h (28,1) sapmalarını ve karışım td'sini çözer. Görev 5'teki kod 7/8/10/11 yuvarlamaları bunun özel halleri.
- **Yaş termometre (0x8446e4):** formül dalı **kuru termometreye** göre: t > 0 su (2501/−2,346/4,186), t ≤ 0 buz (2834,4/−0,26/2,1). tw < 0, t > 0 noktalarda (W=0 SHF uçları) eski tw-dalı 0,5 °C sapıyordu.
- **Çiy noktası (0x844580):** W ≤ 0 ise W = 1e-6 (rapordaki −66,2 °C); Peppers dalı W < Ws(0 °C) ise buz formülü.
- **Durum çözücüleri:** 0x843660 vb. 10'dan 5'e bölünen adımlı tarama, tolerans 1e-6/1e-4; ref_durum'da fark görülmedi.
- **test4 artefaktı:** Daikin bir işlem hata verince (Cool to saturation 50/60 % kök yok) **hemen sonraki işlemin** çıktıları varsayılan nokta (25 °C, %50, 100 l/s) olur ve kapasitesi yazılmaz (S2, S3, S6). ReferansTest bunu taklit eder; uygulama taklit etmez.


## Son durum (1.0)

- 15 işlemin hepsi ve 1500 m senaryosu Daikin referansıyla gösterilen hassasiyette birebir (`ReferansTest`); `test1.ppp` uygulamanın proje yolundan (`jsonOku` → `hesapla`) 14 nokta + 11 kapasite 0 fark (`ProjeKabulTest`, dönüştürücü `tools/ppp_psy.py`).
- Bilinen farklar: Daikin başarısız işlemden sonraki çıktılara varsayılan nokta (25 °C / %50 / 100 l/s) yazar — uygulama yazmaz, hata gösterir (yalnız `ReferansTest` taklit eder). Daikin sıcaklığı ±80 °C'ye kırpar; uygulama kırpmaz (80 °C üstü ısıtmada fark olabilir).

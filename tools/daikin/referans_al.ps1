# Kullanım: powershell -ExecutionPolicy Bypass -File referans_al.ps1 <veri klasörü (Windows yolu)>
param([string]$klasor, [string]$filtre = 'ref_*')
. "$PSScriptRoot\dk.ps1"
foreach ($ppp in Get-ChildItem ("$klasor\" + $filtre + ".ppp")) {
  $ad = $ppp.BaseName
  RealClick 50 179; Start-Sleep -m 1500; DialogKapat | Out-Null; Start-Sleep -m 600
  [System.Windows.Forms.SendKeys]::SendWait('^a'); [System.Windows.Forms.SendKeys]::SendWait($ppp.FullName); Start-Sleep -m 400
  [System.Windows.Forms.SendKeys]::SendWait('{ENTER}'); Start-Sleep -m 3000
  $hatalar = @(); for ($i = 0; $i -lt 30; $i++) { $m = DialogKapat; if (-not $m) { break }; $hatalar += $m; Start-Sleep -m 500 }
  if ($ad -eq 'ref_rakim') { Yaz 335 69 '1500'; Start-Sleep -m 800; $hatalar += DialogKapat
    # rakım değişince işlem çıktıları yeniden hesaplanmaz; bir işlemi Edit + Accept ile hepsini tazeler
    RealClick 300 625; Start-Sleep -m 600; RealClick 491 502; Start-Sleep -m 600; RealClick 491 502; Start-Sleep -m 1000; $hatalar += DialogKapat }  # rakım dosyada saklanmaz
  $hatalar | Set-Content -Encoding UTF8 "$klasor\$ad.hatalar.txt"
  Rapor "$klasor\$ad.rtf"
}

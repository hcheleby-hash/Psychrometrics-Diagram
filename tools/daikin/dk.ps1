Add-Type -AssemblyName UIAutomationClient,UIAutomationTypes,System.Drawing,System.Windows.Forms
Add-Type @'
using System; using System.Text; using System.Runtime.InteropServices;
public class W {
  [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr SendMessage(IntPtr h, int m, IntPtr w, IntPtr l);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr SendMessage(IntPtr h, int m, IntPtr w, string l);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr SendMessage(IntPtr h, int m, IntPtr w, StringBuilder l);
  [DllImport("user32.dll")] public static extern IntPtr GetParent(IntPtr h);
  [DllImport("user32.dll")] public static extern int GetDlgCtrlID(IntPtr h);
}
'@
[W]::SetProcessDPIAware() | Out-Null
$p = Get-Process psychrometrics
$root = [System.Windows.Automation.AutomationElement]::RootElement
$cond = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ProcessIdProperty, $p.Id)
$win = $root.FindFirst([System.Windows.Automation.TreeScope]::Children, $cond)
function Ctl($cls, $x, $y) {
  $all = $win.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
  foreach ($e in $all) { $c=$e.Current; $r=$c.BoundingRectangle
    if ($c.ClassName -eq $cls -and -not $c.IsOffscreen -and [Math]::Abs($r.X-$x) -lt 6 -and [Math]::Abs($r.Y-$y) -lt 6) { return [IntPtr]$c.NativeWindowHandle } }
  throw "yok: $cls $x,$y"
}
function SetText($h, $t) { [W]::SendMessage($h, 0x000C, [IntPtr]::Zero, $t) | Out-Null }   # WM_SETTEXT -> EN_CHANGE
function Click($h) { [W]::SendMessage($h, 0x00F5, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null } # BM_CLICK
function CbItems($h) { $n=[int][W]::SendMessage($h,0x0146,[IntPtr]::Zero,[IntPtr]::Zero); 0..($n-1) | % { $sb=New-Object Text.StringBuilder 256; [W]::SendMessage($h,0x0148,[IntPtr]$_,$sb)|Out-Null; $sb.ToString() } }
function CbSel($h, $i) {
  [W]::SendMessage($h, 0x014E, [IntPtr]$i, [IntPtr]::Zero) | Out-Null   # CB_SETCURSEL
  $id = [W]::GetDlgCtrlID($h); $wp = [IntPtr](($id -band 0xFFFF) -bor (1 -shl 16))  # CBN_SELCHANGE
  [W]::SendMessage([W]::GetParent($h), 0x0111, $wp, $h) | Out-Null
}
function Shot($path, $x, $y, $w, $h) {
  $bmp = New-Object System.Drawing.Bitmap $w, $h; $g=[System.Drawing.Graphics]::FromImage($bmp)
  $g.CopyFromScreen($x, $y, 0, 0, $bmp.Size); $bmp.Save($path)
}
Add-Type @'
using System; using System.Runtime.InteropServices;
public class M {
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern void mouse_event(int f, int x, int y, int d, IntPtr e);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, int m, IntPtr w, IntPtr l);
}
'@
$main = [IntPtr]$win.Current.NativeWindowHandle
function RealClick($x, $y) { [M]::SetForegroundWindow($main) | Out-Null; [M]::SetCursorPos($x, $y) | Out-Null; Start-Sleep -m 80
  [M]::mouse_event(2,0,0,0,[IntPtr]::Zero); [M]::mouse_event(4,0,0,0,[IntPtr]::Zero); Start-Sleep -m 150 }
function Yaz($x, $y, $txt) { RealClick $x $y; [System.Windows.Forms.SendKeys]::SendWait("^a" + $txt + "{TAB}"); Start-Sleep -m 250 }
function PClick($h) { [M]::PostMessage($h, 0x00F5, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null; Start-Sleep -m 500 }  # BM_CLICK, kutu açılırsa takılmaz
Add-Type @'
using System; using System.Text; using System.Collections.Generic; using System.Runtime.InteropServices;
public class Q {
  public delegate bool CB(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(CB f, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr p, CB f, IntPtr l);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetClassName(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr SendMessageTimeout(IntPtr h, int m, IntPtr w, StringBuilder l, int f, int t, out IntPtr r);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  public static string Cls(IntPtr h){var b=new StringBuilder(256);GetClassName(h,b,256);return b.ToString();}
  public static string Txt(IntPtr h){var b=new StringBuilder(4096);IntPtr r;SendMessageTimeout(h,0x000D,(IntPtr)4096,b,2,500,out r);return b.ToString();}
  public static List<IntPtr> Dialogs(uint pid){var l=new List<IntPtr>();EnumWindows((h,x)=>{uint p;GetWindowThreadProcessId(h,out p);if(p==pid&&IsWindowVisible(h)&&Cls(h)!="TDlgDiagram"&&Cls(h)!="TApplication"&&Cls(h)!="IME"){l.Add(h);}return true;},IntPtr.Zero);return l;}
  public static List<IntPtr> Kids(IntPtr p){var l=new List<IntPtr>();EnumChildWindows(p,(h,x)=>{l.Add(h);return true;},IntPtr.Zero);return l;}
}
'@
# Açık mesaj kutusu varsa metnini döndürüp OK ile kapatır
function DialogKapat() {
  $out = @()
  foreach ($d in ([Q]::Dialogs([uint32]$p.Id) | ? { [Q]::Cls($_) -eq "TBaseMsg" })) {
    $t = ([Q]::Kids($d) | ? { [Q]::Cls($_) -eq 'TMemo' } | % { [Q]::Txt($_) }) -join ' '
    if (-not $t) { $t = [Q]::Txt($d) }
    $out += ([Q]::Cls($d) + ': ' + ($t -replace '\s+',' '))
    $ok = [Q]::Kids($d) | ? { [Q]::Cls($_) -eq 'TButton' } | Select-Object -First 1
    if ($ok) { [M]::PostMessage($ok, 0x00F5, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null } else { [M]::PostMessage($d, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null }
    Start-Sleep -m 400
  }
  $out
}
function KaydetFarkli($yol) {
  RealClick 50 203; Start-Sleep -m 1500
  [System.Windows.Forms.SendKeys]::SendWait($yol); Start-Sleep -m 300
  [System.Windows.Forms.SendKeys]::SendWait('{ENTER}'); Start-Sleep -m 1200
  # üzerine yazma onayı çıkarsa Evet
  foreach ($d in [Q]::Dialogs([uint32]$p.Id)) { [System.Windows.Forms.SendKeys]::SendWait('%y'); Start-Sleep -m 800 }
}
function AcDosya($yol) {  # "Ac" PowerShell takma adi (Add-Content) ile cakisir
  DialogKapat | Out-Null
  RealClick 50 179; Start-Sleep -m 1500
  $m = DialogKapat; if ($m) { "acarken: $m"; Start-Sleep -m 1200 }
  [System.Windows.Forms.SendKeys]::SendWait($yol); Start-Sleep -m 400
  [System.Windows.Forms.SendKeys]::SendWait('{ENTER}'); Start-Sleep -m 2000
  $m = DialogKapat; if ($m) { "acilinca: $m" }
}
function WordKapat() { Get-Process | ? { $_.Name -like '*word*' -and $_.MainWindowTitle -like '*Psychrometrics_*' } | Stop-Process -Force }
function Rapor($hedef) {
  WordKapat; Remove-Item "$env:TEMP\Psychrometrics_*.rtf" -Force -ErrorAction SilentlyContinue
  $once = Get-Date
  RealClick 50 227; Start-Sleep -m 3000
  [System.Windows.Forms.SendKeys]::SendWait('{ESC}'); Start-Sleep -m 800
  $r = Get-ChildItem "$env:TEMP\Psychrometrics_*.rtf" | ? { $_.LastWriteTime -ge $once.AddSeconds(-2) } | Sort-Object LastWriteTime | Select-Object -Last 1
  if ($r) { Copy-Item $r.FullName $hedef; "rapor: $hedef" } else { "rapor yok" }
  WordKapat; DialogKapat | Out-Null
}

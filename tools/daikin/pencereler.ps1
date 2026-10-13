Add-Type @'
using System; using System.Text; using System.Collections.Generic; using System.Runtime.InteropServices;
public class E {
  public delegate bool CB(IntPtr h, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumWindows(CB f, IntPtr l);
  [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr p, CB f, IntPtr l);
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetClassName(IntPtr h, StringBuilder s, int n);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  public static string D(IntPtr h){var a=new StringBuilder(256);var b=new StringBuilder(256);GetWindowText(h,a,256);GetClassName(h,b,256);return h+" ["+b+"] '"+a+"'";}
  public static List<string> Run(uint pid){var r=new List<string>();EnumWindows((h,l)=>{uint p;GetWindowThreadProcessId(h,out p);if(p==pid&&IsWindowVisible(h)){r.Add(D(h));EnumChildWindows(h,(c,x)=>{r.Add("   "+D(c));return true;},IntPtr.Zero);}return true;},IntPtr.Zero);return r;}
}
'@
$p = Get-Process psychrometrics
[E]::Run([uint32]$p.Id) | Select-String -NotMatch "TCheckBox|TPanel '|TScrollBar|TFrm_Unit" | Select-Object -First 25

"""Daikin .ppp proje dosyası yazar (UTF-16, CRLF). Kullanım: from ppp_yaz import nokta, yaz"""
import importlib.util, os
sp = importlib.util.spec_from_file_location('d', os.path.join(os.path.dirname(__file__), '..', 'daikin_psikro.py'))
d = importlib.util.module_from_spec(sp); sp.loader.exec_module(d)
f = lambda x: repr(float(x)).rstrip('0').rstrip('.').replace('.', ',') if float(x) != int(x) else str(int(x))
def nokta(ad, t, W=None, rh=None, debi=150):
    if rh is not None:
        return f'1 {ad} 0 {f(t)} 2 {f(rh)} {debi} {f(t)} {f(rh)}'
    pw = W * 101325 / (d.EPS + W)
    return f'1 {ad} 0 {f(t)} 1 {f(W)} {debi} {f(t)} {f(pw / d.pws(t) * 100)}'
def yaz(yol, satirlar):
    open(yol, 'w', encoding='utf-16', newline='\r\n').write('\n'.join(satirlar) + '\n')

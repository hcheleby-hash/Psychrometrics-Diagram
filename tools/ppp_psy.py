"""Daikin .ppp -> uygulama proje JSON'u (.psy). Kullanım: py -3 -I tools/ppp_psy.py <girdi.ppp> <cikti.psy>"""
import json, os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'daikin'))
from referans_json import ppp_oku

TUR = {0: 'NOKTA_GOSTER', 1: 'BAGLA', 2: 'KARISIM', 3: 'SOGUTMA', 4: 'ISITMA', 5: 'SOGUTMA_KAPASITE', 6: 'ISITMA_KAPASITE',
       7: 'BUHAR_NEM', 8: 'SU_NEM', 9: 'DOYMAYA_SOGUTMA', 10: 'YOGUSMA', 11: 'NEM_ALMA', 12: 'BYPASS', 13: 'CIKIS', 14: 'SHF'}

def renk(bgr):  # Daikin BGR int -> Android ARGB (işaretli int)
    r, g, b = bgr & 255, (bgr >> 8) & 255, (bgr >> 16) & 255
    return (0xFF000000 | r << 16 | g << 8 | b) - (1 << 32)

def param(kod, p):  # Daikin parametre sırası -> IslemTuru.parametreler sırası (ReferansTest.parametreSirasi ile aynı)
    if kod == 3: return [p[0] if p else 1.0]
    if kod == 4: return [p[0], p[1] if len(p) > 1 else 1.0]
    if kod == 5: return [p[0], p[1], p[2] if len(p) > 2 else 1.0]
    if kod == 9: return [p[1], p[2] if len(p) > 2 else 1.0]
    return p

def cevir(ppp):
    noktalar, islemler = ppp_oku(ppp)
    renkler = [int(s.split()[1]) for s in open(ppp, encoding='utf-16').read().split('\n') if s.split()[:1] == ['2']]
    return {'surum': 1, 'rakim': 0.0,
            'noktalar': [{'ad': n['ad'], 'o1': 'KURU', 'v1': n['tc'], 'o2': 'BAGIL', 'v2': n['rhc'], 'debi': n['debi'], 'renk': renk(0xC0 << 16 | 0x65 << 8 | 0x15)} for n in noktalar],
            'islemler': [{'tur': TUR[i['kod']], 'girdiler': i['girdi'], 'ciktilar': i['cikti'], 'param': param(i['kod'], i['param']), 'renk': renk(c)} for i, c in zip(islemler, renkler)]}

if __name__ == '__main__':
    json.dump(cevir(sys.argv[1]), open(sys.argv[2], 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

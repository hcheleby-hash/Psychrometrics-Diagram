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

YUVARLA = [12] * 7  # Daikin noktayı önbellek alanlarından (t, RH) hesaplar; girdiler tam hassasiyetle yazılır

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
    for p_, kw, shf in (('A', 2, 75), ('C', 4, 60), ('N', 1.5, 90)):                        # Cooling capacity
        a, b = pp(2); s.append(f'{R} 5 {p_} {a} {b} {f(kw)} {shf} 1')
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

def rakim_senaryosu():
    # rakım .ppp'ye yazılmaz; referans_al.ps1 açtıktan sonra Altitude = 1500 m girer
    s = islem_senaryosu()
    return [x for x in s if x.startswith('1 ')] + [x for x in s if x.startswith(R)][:8]

if __name__ == '__main__':
    klasor = sys.argv[1]
    yaz(os.path.join(klasor, 'ref_durum.ppp'), durum_senaryosu())
    yaz(os.path.join(klasor, 'ref_islem.ppp'), islem_senaryosu())
    yaz(os.path.join(klasor, 'ref_shf.ppp'), shf_senaryosu())
    yaz(os.path.join(klasor, 'ref_rakim.ppp'), rakim_senaryosu())

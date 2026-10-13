"""Daikin Psychrometrics 3.20 exe'sindeki sabitlerden çıkarılan formüller (ASHRAE Fundamentals Bölüm 1)."""
from math import log, exp

EPS = 0.6219779385      # Mw/Mda  (exe 0x442918)
RDA = 287.0551882       # J/kgK   (exe 0x442a50)


def pws(t):
    """Doyma buhar basıncı [Pa], Hyland-Wexler. t [°C]"""
    T = t + 273.15
    if t >= 0:  # su üzerinde
        return exp(-5800.2206 / T + 1.3914993 - 0.048640239 * T + 4.1764768e-5 * T**2
                   - 1.4452093e-8 * T**3 + 6.5459673 * log(T))
    return exp(-5674.5359 / T + 6.3925247 - 9.677843e-3 * T + 6.2215701e-7 * T**2  # buz üzerinde
               + 2.0747825e-9 * T**3 - 9.484024e-13 * T**4 + 4.1635019 * log(T))


def nem_orani(pw, p):
    return EPS * pw / (p - pw)


def W_yas(t, tw, p):
    """Yaş termometre tw'den nem oranı (ASHRAE eş. 35/37, buhar cp=1.84 ile)."""
    ws = nem_orani(pws(tw), p)
    if tw >= 0:
        return ((2501 - 2.346 * tw) * ws - 1.006 * (t - tw)) / (2501 + 1.84 * t - 4.186 * tw)
    return ((2834.4 - 0.26 * tw) * ws - 1.006 * (t - tw)) / (2834.4 + 1.84 * t - 2.1 * tw)


def yas_termometre(t, rh, p=101325.0):
    """Kuru termometre t [°C] + bağıl nem rh [0-1] -> yaş termometre [°C]. Bisection, tolerans 1e-10."""
    W = nem_orani(rh * pws(t), p)
    lo, hi = -100.0, t
    while hi - lo > 1e-10:
        m = (lo + hi) / 2
        lo, hi = (m, hi) if W_yas(t, m, p) < W else (lo, m)
    return (lo + hi) / 2


def ciy_noktasi(pw):
    """Çiy noktası [°C], pw [Pa]. ASHRAE eş. 39/40 (Peppers)."""
    a = log(pw / 1000)
    td = 6.54 + 14.526 * a + 0.7389 * a**2 + 0.09486 * a**3 + 0.4569 * (pw / 1000) ** 0.1984
    return td if td >= 0 else 6.09 + 12.608 * a + 0.4959 * a**2


def entalpi(t, W):
    return 1.006 * t + W * (2501 + 1.84 * t)  # kJ/kg kuru hava


def ozgul_hacim(t, W, p):
    return RDA * (t + 273.15) * (1 + 1.607774067 * W) / p  # m³/kg kuru hava


if __name__ == "__main__":
    p = 101325.0
    # ASHRAE tablosu: pws(20°C)=2339 Pa, pws(-10°C, buz)=259.9 Pa
    assert abs(pws(20) - 2339) < 2 and abs(pws(-10) - 259.9) < 0.5
    # Doymuş havada yaş termometre = kuru termometre = çiy noktası
    assert abs(yas_termometre(20, 1.0) - 20) < 1e-3 and abs(ciy_noktasi(pws(20)) - 20) < 0.05
    print(f"{'t':>5} {'RH':>4} {'twb':>7} {'td':>7} {'W g/kg':>7} {'h kJ/kg':>8} {'v m3/kg':>8}")
    for t, rh in [(25, .5), (35, .4), (20, .7), (0, .8), (-5, .6), (40, .2)]:
        pw = rh * pws(t); W = nem_orani(pw, p)
        print(f"{t:5.1f} {rh*100:4.0f} {yas_termometre(t, rh, p):7.2f} {ciy_noktasi(pw):7.2f} "
              f"{W*1000:7.3f} {entalpi(t, W):8.2f} {ozgul_hacim(t, W, p):8.4f}")

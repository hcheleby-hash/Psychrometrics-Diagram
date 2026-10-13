"""Daikin SHF doğrusu tanımı: aday formülleri rapor verisiyle karşılaştırır."""
import importlib.util
s = importlib.util.spec_from_file_location('d', 'daikin_psikro.py'); d = importlib.util.module_from_spec(s); s.loader.exec_module(d)
P = 101325; h = d.entalpi; Ws = lambda t: d.nem_orani(d.pws(t), P)
# (başlangıç t, W, SHF) -> gözlenen (t, W)  [W=0'a inenler: t, 0]
SHF_ISLEM = [((27, .010, .75), (10.0, .0076)), ((27, .010, .90), (13.0, .0094)), ((35, .015, .75), (17.4, .0125)),
             ((35, .015, .90), (19.5, .0143)), ((27, .010, .50), (3.1, 0)), ((35, .015, .50), (0.4, 0))]
COOL_SAT = [((27, .010, .75), (10.3, .0078)), ((27, .010, .90), (13.1, .0094)), ((35, .015, .60), (12.5, .0090)),
            ((35, .015, .75), (17.7, .0127)), ((35, .015, .90), (19.6, .0143))]
def coz(f, a, b):
    fa = f(a)
    for _ in range(80):
        m = (a + b) / 2; fm = f(m)
        if fa * fm > 0: a, fa = m, fm
        else: b = m
    return (a + b) / 2
def bitis(Wdogru, t1):
    """Doğru W(t) ile doyma kesişimi; yoksa W=0 noktası."""
    g = lambda t: Wdogru(t) - Ws(t)
    if g(-30) * g(t1 - 1e-6) < 0: t = coz(g, -30, t1 - 1e-6); return t, Ws(t)
    t = coz(Wdogru, -60, t1); return t, 0.0
def dogrusal(cp, hfg):  # W(t) = W1 - (t1-t)*cp(t1,W1)*(1-SHF)/(SHF*hfg(t1,W1))
    return lambda t1, W1, S: (lambda t: W1 - (t1 - t) * cp(t1, W1) * (1 - S) / (S * hfg(t1, W1)))
def hw(hfg):  # (h,W) düzleminde doğru: h = h1 - (W1-W)*hfg/(1-SHF)
    def kur(t1, W1, S):
        def W_t(t):  # h(t,W) = h1 - (W1-W)*k  ->  W'yi çöz
            k = hfg(t1, W1) / (1 - S); h1 = h(t1, W1)
            # 1.006t + W(2501+1.84t) = h1 - W1*k + W*k  -> W = (h1 - W1*k - 1.006t)/(2501+1.84t-k)
            return (h1 - W1 * k - 1.006 * t) / (2501 + 1.84 * t - k)
        return W_t
    return kur
ADAY = {
    'dogru cp=1.006+1.84W1 hfg=2501': dogrusal(lambda t, W: 1.006 + 1.84 * W, lambda t, W: 2501),
    'dogru cp=1.006 hfg=2501': dogrusal(lambda t, W: 1.006, lambda t, W: 2501),
    'dogru cp=1.006+1.84W1 hfg=2501+1.84t1': dogrusal(lambda t, W: 1.006 + 1.84 * W, lambda t, W: 2501 + 1.84 * t),
    'dogru cp=1.006 hfg=2501+1.84t1': dogrusal(lambda t, W: 1.006, lambda t, W: 2501 + 1.84 * t),
    'dogru cp=1.006+1.84W1 hfg=2501-2.346t1': dogrusal(lambda t, W: 1.006 + 1.84 * W, lambda t, W: 2501 - 2.346 * t),
    'hW cp hfg=2501': hw(lambda t, W: 2501),
    'hW hfg=2501+1.84t1': hw(lambda t, W: 2501 + 1.84 * t),
}
for ad, kur in ADAY.items():
    for etiket, veri in (('SHF', SHF_ISLEM), ('COOL', COOL_SAT)):
        hata = []
        for (t1, W1, S), (tg, Wg) in veri:
            t, W = bitis(kur(t1, W1, S), t1); hata.append(t - tg)
        print(f'{ad:42s} {etiket:4s} max|dt|={max(map(abs, hata)):.2f}  ' + ' '.join(f'{x:+.2f}' for x in hata))


print('--- t1den asagi yuruyerek ilk kesisim; dogru ve yerel-SHF egrisi')
def yuru(egim, t1, W1, dt=0.002):
    """egim(t,W) = dW/dt. t1'den aşağı yürür; doyma ya da W=0'da durur."""
    t, W = t1, W1
    while t > -60:
        tn, Wn = t - dt, W - dt * egim(t, W)
        if Wn <= 0: return t - dt * W / (W - Wn), 0.0
        if Wn >= Ws(tn): return tn, Wn
        t, W = tn, Wn
ADAY2 = {
  'dogru cp=1.006+1.84W1, hfg=2501':      lambda t1, W1, S: (lambda t, W: (1.006 + 1.84*W1)*(1-S)/(S*2501)),
  'dogru cp=1.006, hfg=2501':             lambda t1, W1, S: (lambda t, W: 1.006*(1-S)/(S*2501)),
  'dogru cp=1.006, hfg=2501+1.84t1':      lambda t1, W1, S: (lambda t, W: 1.006*(1-S)/(S*(2501+1.84*t1))),
  'dogru cp=1.006+1.84W1, hfg=2501-2.346t1': lambda t1, W1, S: (lambda t, W: (1.006+1.84*W1)*(1-S)/(S*(2501-2.346*t1))),
  'egri cp=1.006+1.84W, hfg=2501':        lambda t1, W1, S: (lambda t, W: (1.006 + 1.84*W)*(1-S)/(S*2501)),
  'egri cp=1.006+1.84W, hfg=2501+1.84t':  lambda t1, W1, S: (lambda t, W: (1.006 + 1.84*W)*(1-S)/(S*(2501+1.84*t))),
  'egri cp=1.006+1.84W, hfg=2501-2.346t': lambda t1, W1, S: (lambda t, W: (1.006 + 1.84*W)*(1-S)/(S*(2501-2.346*t))),
  'egri cp=1.006, hfg=2501-2.346t':       lambda t1, W1, S: (lambda t, W: 1.006*(1-S)/(S*(2501-2.346*t))),
}
for ad, kur in ADAY2.items():
    for etiket, veri in (('SHF', SHF_ISLEM), ('COOL', COOL_SAT)):
        hata = [yuru(kur(t1, W1, S), t1, W1)[0] - tg for (t1, W1, S), (tg, Wg) in veri]
        print(f'{ad:42s} {etiket:4s} max|dt|={max(map(abs, hata)):.2f}  ' + ' '.join(f'{x:+.2f}' for x in hata))

print('--- kapasite tanimi: cp*(t1-tx)/(h1-hx) = SHF, x doyma uzerinde (t1e en yakin kok)')
def kap(cpf):
    def bul(t1, W1, S):
        f = lambda tx: cpf(W1, Ws(tx)) * (t1 - tx) / (h(t1, W1) - h(tx, Ws(tx))) - S
        t = t1 - 0.01
        while t > -60:
            if h(t1, W1) - h(t, Ws(t)) > 0 and f(t) * f(t - 0.01) <= 0: return coz(f, t - 0.01, t)
            t -= 0.01
        return float('nan')
    return bul
for ad, cpf in {'cp=1.006+1.84W1': lambda W1, Wx: 1.006 + 1.84 * W1, 'cp=1.006+1.84Wx': lambda W1, Wx: 1.006 + 1.84 * Wx,
                'cp=1.006': lambda W1, Wx: 1.006, 'h tabanli: h(t1,Wx)-hx': None}.items():
    if cpf is None:
        bul = lambda t1, W1, S: coz(lambda tx: (h(t1, Ws(tx)) - h(tx, Ws(tx))) / (h(t1, W1) - h(tx, Ws(tx))) - S, -20, t1 - 0.5)
    else:
        bul = kap(cpf)
    for etiket, veri in (('SHF', SHF_ISLEM[:4]), ('COOL', COOL_SAT)):
        hata = [bul(t1, W1, S) - tg for (t1, W1, S), (tg, Wg) in veri]
        print(f'{ad:42s} {etiket:4s} max|dt|={max(map(abs, hata)):.2f}  ' + ' '.join(f'{x:+.2f}' for x in hata))

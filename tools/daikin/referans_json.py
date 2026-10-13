"""ref_*.ppp + ref_*.txt -> app/src/test/resources/daikin_referans.json. Kullanım: py -3 referans_json.py <veri> <json>"""
import glob, json, os, re, sys

def sayi(s): return float(s.replace(',', '.'))

def ppp_oku(yol):
    noktalar, islemler = [], []
    for satir in open(yol, encoding='utf-16').read().split('\n'):
        a = satir.split()
        if not a: continue
        if a[0] == '1':
            noktalar.append({'ad': a[1], 'o1': int(a[2]), 'v1': sayi(a[3]), 'o2': int(a[4]), 'v2': sayi(a[5]), 'debi': sayi(a[6])})
        elif a[0] == '2':
            kod, geri = int(a[2]), a[3:]
            girdi_n = {0: 1, 1: 2, 2: 2, 3: 2, 4: 2, 10: 2}.get(kod, 1)
            cikti_n = {0: 0, 1: 0, 10: 0, 2: 1, 3: 1, 4: 1}.get(kod, 2)
            girdi, cikti, param = geri[:girdi_n], geri[girdi_n:girdi_n + cikti_n], [sayi(x) for x in geri[girdi_n + cikti_n:]]
            islemler.append({'kod': kod, 'girdi': girdi, 'cikti': list(dict.fromkeys(cikti)), 'param': param})
    return noktalar, islemler

ALAN = {'Dry bulb temperature': 't', 'Wet bulb temperature': 'twb', 'Dew point': 'td', 'Relative humidity': 'rh',
        'Absolute humidity': 'W', 'Enthalpy': 'h', 'Density': 'rho', 'Specific volume': 'v', 'Pressure': 'p', 'Airflow': 'debi'}

def rapor_oku(yol):
    metin = open(yol, encoding='utf-8', errors='replace').read()
    nokta_kismi, _, islem_kismi = metin.partition('X. Actions')
    noktalar, adlar = {}, []
    for satir in nokta_kismi.split('\n'):
        hucre = [h.strip() for h in satir.split('|')]
        if len(hucre) < 2: continue
        if hucre[0] in ALAN:
            for k in range(0, len(hucre) - 1, 2):
                if hucre[k] in ALAN and k // 2 < len(adlar):
                    m = re.match(r'-?[\d,]+', hucre[k + 1])
                    if m: noktalar.setdefault(adlar[k // 2], {})[ALAN[hucre[k]]] = sayi(m.group())
        elif all(re.fullmatch(r'[A-Za-z][A-Za-z0-9]*', h) or h == '' for h in hucre):
            adlar = [h for h in hucre if h]
    islemler = {}
    for satir in islem_kismi.split('\n'):
        m = re.match(r'\s*([A-Za-z ]+): ([^(|]+)', satir)
        if not m: continue
        isimler = [x.strip() for x in m.group(2).split(',') if x.strip()]
        degerler = {k.strip(): sayi(v) for k, v in re.findall(r'([A-Za-z][A-Za-z ]*?)\s*=\s*(-?[\d,]+)', satir)}
        islemler[(m.group(1).strip(), tuple(isimler))] = degerler
    return noktalar, islemler

AD = {0: 'Show point', 1: 'Connect', 2: 'Mix air', 3: 'Cooling', 4: 'Heating', 5: 'Cooling capacity', 6: 'Heating capacity',
      7: 'Steam humidification', 8: 'Water humidification', 9: 'Cool to saturation', 10: 'Condensate', 11: 'Dehumidification',
      12: 'Bypass factor', 13: 'Discharge', 14: 'Sensible heat factor'}

def senaryo(ppp):
    taban = ppp[:-4]
    noktalar, islemler = ppp_oku(ppp)
    bek_n, bek_i = rapor_oku(taban + '.txt')
    hatalar_yol = taban + '.hatalar.txt'
    hatalar = [h for h in open(hatalar_yol, encoding='utf-8-sig').read().split('\n') if h.strip()] if os.path.exists(hatalar_yol) else []
    beklenen_islemler = {}
    for isl in islemler:
        anahtar_rapor = (AD[isl['kod']], tuple(isl['girdi'] + isl['cikti']))
        if anahtar_rapor in bek_i:
            ad = isl['cikti'][0] if isl['cikti'] else f"{isl['kod']}:" + ':'.join(isl['girdi'])
            beklenen_islemler[ad] = bek_i[anahtar_rapor]
    basinc = next(iter(bek_n.values()))['p']  # raporun "Pressure" satırı; rakımlı senaryoda da doğru basınç
    return {'ad': os.path.basename(taban), 'basinc': basinc, 'noktalar': noktalar, 'islemler': islemler,
            'beklenen_noktalar': bek_n, 'beklenen_islemler': beklenen_islemler, 'hatalar': hatalar}

if __name__ == '__main__':
    veri, cikti = sys.argv[1], sys.argv[2]
    s = [senaryo(p) for p in sorted(glob.glob(os.path.join(veri, '*.ppp'))) if os.path.exists(p[:-4] + '.txt')]
    json.dump({'senaryolar': s}, open(cikti, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(len(s), 'senaryo,', sum(len(x['beklenen_noktalar']) for x in s), 'nokta,', sum(len(x['beklenen_islemler']) for x in s), 'işlem')

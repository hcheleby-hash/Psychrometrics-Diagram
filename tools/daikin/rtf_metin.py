"""Daikin RTF raporunu düz metne çevirir (tablo hücreleri ' | ' ile). Kullanım: py -3 rtf_metin.py rapor.rtf"""
import re
import sys

ATLA = {"pict", "fonttbl", "colortbl", "stylesheet", "info", "header", "footer", "object", "themedata", "datastore"}


def rtf_metin(rtf):
    out, yigin, atla = [], [], False
    for m in re.finditer(r"\\([a-z]+)(-?\d+)? ?|\\'([0-9a-f]{2})|\\(.)|([{}])|([^\\{}\r\n]+)", rtf):
        kelime, _, hexb, sembol, parantez, metin = m.groups()
        if parantez == "{":
            yigin.append(atla)
        elif parantez == "}":
            atla = yigin.pop() if yigin else False
        elif atla:
            continue
        elif kelime:
            if kelime in ATLA or (sembol == "*"):
                atla = True
            elif kelime in ("par", "line", "row"):
                out.append("\n")
            elif kelime == "cell":
                out.append(" | ")
            elif kelime == "tab":
                out.append("\t")
        elif sembol == "*":
            atla = True
        elif hexb:
            out.append(bytes.fromhex(hexb).decode("cp1252"))
        elif metin:
            out.append(metin)
    t = re.sub(r"[ \t]+", " ", "".join(out))
    return re.sub(r"\n\s*\n+", "\n", t).strip()


if __name__ == "__main__":
    print(rtf_metin(open(sys.argv[1], encoding="latin-1").read()))

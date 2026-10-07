#!/usr/bin/env python3
"""Verifica che le citazioni tra «» nei file di analisi compaiano nel testo biblico.
Le citazioni con "..." si controllano pezzo per pezzo; i pezzi di una sola parola si ignorano.

Uso: python3 check_quotes.py <cartella assets> [numeri dei libri...]
"""
import glob
import os
import re
import sys
import unicodedata


def norm(s):
    s = unicodedata.normalize('NFD', s.lower())
    s = ''.join(c for c in s if not unicodedata.combining(c))
    s = s.replace('’', "'").replace('‘', "'")
    s = re.sub(r"[^a-z0-9' ]", ' ', s)
    s = s.replace("'", "' ")
    return re.sub(r'\s+', ' ', s).strip()


def main():
    assets = sys.argv[1]
    ids = [int(x) for x in sys.argv[2:]]
    all_text = {}
    for f in glob.glob(os.path.join(assets, 'bibbia', '[0-9][0-9].txt')):
        bid = int(os.path.basename(f)[:2])
        all_text[bid] = norm(' '.join(l.split('\t', 2)[2] for l in open(f, encoding='utf-8') if l.count('\t') >= 2))
    whole = ' '.join(all_text.values())
    bad = 0
    files = sorted(glob.glob(os.path.join(assets, 'analisi', '[0-9][0-9].txt')))
    for f in files:
        bid = int(os.path.basename(f)[:2])
        if ids and bid not in ids:
            continue
        for n, line in enumerate(open(f, encoding='utf-8'), 1):
            if line.startswith('chiave:'):
                continue
            for q in re.findall(r'«([^»]+)»', line):
                for piece in re.split(r'\.\.\.|…', q):
                    if len(piece.split()) < 2:
                        continue
                    nq = norm(piece)
                    if nq and nq not in all_text.get(bid, '') and nq not in whole:
                        bad += 1
                        print(f'{os.path.basename(f)}:{n}: «{piece.strip()}»')
    print('OK' if bad == 0 else f'{bad} citazioni non trovate')


if __name__ == '__main__':
    main()

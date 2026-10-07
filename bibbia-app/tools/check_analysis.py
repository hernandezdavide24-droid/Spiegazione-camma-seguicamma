#!/usr/bin/env python3
"""Controlla i file di analisi (assets/analisi/NN.txt) rispetto al testo biblico:
ogni capitolo presente una volta, sezioni crescenti che partono dal versetto 1 e non
superano l'ultimo versetto, campi dell'introduzione presenti, versetti chiave validi.

Uso: python3 check_analysis.py <cartella assets> [numeri dei libri...]
"""
import os
import re
import sys

REQUIRED = ['sintesi', 'autore', 'data', 'contesto', 'temi', 'struttura', 'chiave']


def load_books(assets):
    books = {}
    for line in open(os.path.join(assets, 'bibbia', 'libri.tsv'), encoding='utf-8'):
        p = line.rstrip('\n').split('\t')
        if len(p) == 6:
            books[int(p[0])] = (p[2], p[3], [int(x) for x in p[5].split(',')])
    return books


def parse_ref(ref, books):
    m = re.match(r'^((?:[1-3]\s*)?[A-Za-zÀ-ÿ]+)\.?\s*(\d+)(?::(\d+))?(?:-(\d+)(?::(\d+))?)?$', ref.strip())
    if not m:
        return False
    abbr = m.group(1).replace(' ', '')
    for _, (name, ab, counts) in books.items():
        if ab.lower() == abbr.lower() or name.replace(' ', '').lower() == abbr.lower():
            c = int(m.group(2))
            if c > len(counts):
                return False
            if m.group(3) and int(m.group(3)) > counts[c - 1]:
                return False
            return True
    return False


def check(assets, bid, books):
    name, _, counts = books[bid]
    path = os.path.join(assets, 'analisi', f'{bid:02d}.txt')
    errs = []
    if not os.path.exists(path):
        return [f'{name}: file mancante']
    mode, chap, fields, seen = '', None, {}, {}
    last_section = 0
    for n, raw in enumerate(open(path, encoding='utf-8'), 1):
        line = raw.strip()
        if not line or line.startswith('//'):
            continue
        if line == '#intro':
            mode = 'intro'
            continue
        if line.startswith('#cap '):
            mode = 'cap'
            parts = line[5:].split(' ', 1)
            chap = int(parts[0])
            if len(parts) < 2 or not parts[1].strip():
                errs.append(f'{name} {chap}: titolo mancante')
            if chap in seen:
                errs.append(f'{name} {chap}: capitolo ripetuto')
            if chap > len(counts):
                errs.append(f'{name} {chap}: il libro ha solo {len(counts)} capitoli')
            seen[chap] = {'sections': [], 'summary': ''}
            last_section = 0
            continue
        if mode == 'intro':
            m = re.match(r'^([a-z]+):\s*(.*)$', line)
            if m:
                fields[m.group(1)] = m.group(2)
            continue
        if mode == 'cap':
            if line.startswith('@'):
                v = int(line[1:].split(' ', 1)[0])
                if chap <= len(counts) and v > counts[chap - 1]:
                    errs.append(f'{name} {chap}: sezione @{v} oltre l\'ultimo versetto ({counts[chap - 1]})')
                if v <= last_section:
                    errs.append(f'{name} {chap}: sezione @{v} non crescente')
                if not seen[chap]['sections'] and v != 1:
                    errs.append(f'{name} {chap}: la prima sezione deve iniziare dal versetto 1')
                last_section = v
                seen[chap]['sections'].append(v)
            else:
                seen[chap]['summary'] += line
    for c in range(1, len(counts) + 1):
        if c not in seen:
            errs.append(f'{name} {c}: capitolo mancante')
        elif not seen[c]['summary']:
            errs.append(f'{name} {c}: sintesi mancante')
        elif not seen[c]['sections']:
            errs.append(f'{name} {c}: nessuna sezione')
    for k in REQUIRED:
        if k not in fields:
            errs.append(f'{name}: campo introduzione "{k}" mancante')
    for r in fields.get('chiave', '').split(';'):
        if r.strip() and not parse_ref(r, books):
            errs.append(f'{name}: versetto chiave non valido "{r.strip()}"')
    return errs


if __name__ == '__main__':
    assets = sys.argv[1]
    books = load_books(assets)
    ids = [int(x) for x in sys.argv[2:]] or sorted(books)
    total = 0
    for bid in ids:
        e = check(assets, bid, books)
        total += len(e)
        for x in e:
            print(x)
    print('OK' if total == 0 else f'{total} problemi')
    sys.exit(1 if total else 0)

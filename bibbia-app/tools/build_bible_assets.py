#!/usr/bin/env python3
"""Converte la Riveduta 1927 (OSIS) e i riferimenti incrociati di openbible.info
nel formato testuale letto dall'app (app/src/main/assets/bibbia/).

Uso: python3 build_bible_assets.py <riveduta.osis.xml> <ita-ita1927.txt> <vref.txt> \
        <cross_references.txt> <cartella_assets>

Fonti:
  - Testo: Riveduta 1927 di Giovanni Luzzi (pubblico dominio).
    * eBible.org "ita1927" (testo principale, versificazione vref):
      https://github.com/BibleNLP/ebible/blob/main/corpus/ita-ita1927.txt
      https://github.com/BibleNLP/ebible/blob/main/metadata/vref.txt
    * OSIS di CrossWire/open-bibles (struttura KJV, titoli dei Salmi e versetti
      che eBible non riporta per differenze di numerazione):
      https://github.com/seven1m/open-bibles/blob/master/ita-riveduta.osis.xml
    Il file OSIS perde spesso la prima lettera delle parole ("isse" per "disse"),
    quindi si usa solo dove eBible manca, con le correzioni manuali in FIXES.
  - Riferimenti incrociati: https://www.openbible.info/labs/cross-references/ (CC-BY)
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

NS = '{http://www.bibletechnologies.net/2003/OSIS/namespace}'

# (osis, nome italiano, abbreviazione)
BOOKS = [
    ("Gen", "Genesi", "Gen"), ("Exod", "Esodo", "Es"), ("Lev", "Levitico", "Lv"),
    ("Num", "Numeri", "Nm"), ("Deut", "Deuteronomio", "Dt"), ("Josh", "Giosuè", "Gs"),
    ("Judg", "Giudici", "Gdc"), ("Ruth", "Ruth", "Rt"), ("1Sam", "1 Samuele", "1Sam"),
    ("2Sam", "2 Samuele", "2Sam"), ("1Kgs", "1 Re", "1Re"), ("2Kgs", "2 Re", "2Re"),
    ("1Chr", "1 Cronache", "1Cr"), ("2Chr", "2 Cronache", "2Cr"), ("Ezra", "Esdra", "Esd"),
    ("Neh", "Neemia", "Ne"), ("Esth", "Ester", "Est"), ("Job", "Giobbe", "Gb"),
    ("Ps", "Salmi", "Sal"), ("Prov", "Proverbi", "Pr"), ("Eccl", "Ecclesiaste", "Ec"),
    ("Song", "Cantico dei Cantici", "Ct"), ("Isa", "Isaia", "Is"), ("Jer", "Geremia", "Ger"),
    ("Lam", "Lamentazioni", "Lam"), ("Ezek", "Ezechiele", "Ez"), ("Dan", "Daniele", "Dn"),
    ("Hos", "Osea", "Os"), ("Joel", "Gioele", "Gl"), ("Amos", "Amos", "Am"),
    ("Obad", "Abdia", "Abd"), ("Jonah", "Giona", "Gn"), ("Mic", "Michea", "Mi"),
    ("Nah", "Nahum", "Na"), ("Hab", "Abacuc", "Ab"), ("Zeph", "Sofonia", "Sof"),
    ("Hag", "Aggeo", "Ag"), ("Zech", "Zaccaria", "Zc"), ("Mal", "Malachia", "Ml"),
    ("Matt", "Matteo", "Mt"), ("Mark", "Marco", "Mc"), ("Luke", "Luca", "Lc"),
    ("John", "Giovanni", "Gv"), ("Acts", "Atti degli Apostoli", "At"), ("Rom", "Romani", "Rm"),
    ("1Cor", "1 Corinzi", "1Cor"), ("2Cor", "2 Corinzi", "2Cor"), ("Gal", "Galati", "Gal"),
    ("Eph", "Efesini", "Ef"), ("Phil", "Filippesi", "Fil"), ("Col", "Colossesi", "Col"),
    ("1Thess", "1 Tessalonicesi", "1Ts"), ("2Thess", "2 Tessalonicesi", "2Ts"),
    ("1Tim", "1 Timoteo", "1Tm"), ("2Tim", "2 Timoteo", "2Tm"), ("Titus", "Tito", "Tt"),
    ("Phlm", "Filemone", "Fm"), ("Heb", "Ebrei", "Eb"), ("Jas", "Giacomo", "Gc"),
    ("1Pet", "1 Pietro", "1Pt"), ("2Pet", "2 Pietro", "2Pt"), ("1John", "1 Giovanni", "1Gv"),
    ("2John", "2 Giovanni", "2Gv"), ("3John", "3 Giovanni", "3Gv"), ("Jude", "Giuda", "Gd"),
    ("Rev", "Apocalisse", "Ap"),
]
OSIS_TO_ID = {b[0]: i + 1 for i, b in enumerate(BOOKS)}

# Il file OSIS contiene alcuni caratteri Windows-1252 non convertiti.
CP1252_FIX = {'\x92': '’', '\x91': '‘', '\x93': '“', '\x94': '”',
              '\x85': '…', '\x96': '–', '\x97': '—'}


USFM = ("GEN EXO LEV NUM DEU JOS JDG RUT 1SA 2SA 1KI 2KI 1CH 2CH EZR NEH EST JOB PSA PRO "
        "ECC SNG ISA JER LAM EZK DAN HOS JOL AMO OBA JON MIC NAM HAB ZEP HAG ZEC MAL MAT MRK "
        "LUK JHN ACT ROM 1CO 2CO GAL EPH PHP COL 1TH 2TH 1TI 2TI TIT PHM HEB JAS 1PE 2PE 1JN "
        "2JN 3JN JUD REV").split()

# Refusi del file OSIS nei versetti presi da lì (verificati a mano).
FIXES = {
    (1, 31, 55): [('Poi abano', 'Poi Labano')],
    (5, 22, 30): [('padre ne solleverà', 'padre né solleverà')],
    (13, 6, 77): [('suo ontado', 'suo contado')],
    (16, 4, 23): [('; gnuno', '; ognuno')],
    (24, 9, 26): [('d’Israele e incirconcisa', 'd’Israele è incirconcisa')],
    (29, 2, 32): [('in erusalemme', 'in Gerusalemme')],
    (29, 3, 14): [('del iudizio', 'del giudizio')],
    (29, 3, 16): [('la terrà', 'la terra'), ('; a l’Eterno', '; ma l’Eterno')],
    (38, 1, 21): [('della nazioni', 'delle nazioni')],
}

# Accenti persi nel testo eBible ("e" al posto di "è"), verificati uno per uno.
# Le sostituzioni valgono per tutta la Bibbia, quindi devono essere frasi non ambigue.
EBIBLE_FIXES = [
    (' e stato ', ' è stato '), (' e stata ', ' è stata '),
    ('l’anno di remissione, e vicino!', 'l’anno di remissione, è vicino!'),
    ('su voi e caduta a terra', 'su voi è caduta a terra'),
    ('neppure una e caduta a terra', 'neppure una è caduta a terra'),
    ('quest’uomo e venuto in casa mia', 'quest’uomo è venuto in casa mia'),
    ('Dio e venuto nell’accampamento', 'Dio è venuto nell’accampamento'),
    ('uno del popolo e venuto', 'uno del popolo è venuto'),
    ('Il fuoco di Dio e caduto', 'Il fuoco di Dio è caduto'),
    ('L’Eterno e vicino a quelli', 'L’Eterno è vicino a quelli'),
    ('il vostro Dio, e Dio lassù', 'il vostro Dio, è Dio lassù'),
    ('la luna rimase la suo luogo', 'la luna rimase al suo luogo'),
    ('e sulla terra e tuo!', 'e sulla terra è tuo!'),
    ('riconobbe che l’Eterno Dio.', 'riconobbe che l’Eterno è Dio.'),
    ('Ecco, Iddio e colui che m’aiuta', 'Ecco, Iddio è colui che m’aiuta'),
    ('la sua benevolenza e per tutta', 'la sua benevolenza è per tutta'),
    ('la cui trasgressione e rimessa', 'la cui trasgressione è rimessa'),
    ('io so ch’egli e così', 'io so ch’egli è così'),
    ('e questo e il numero', 'e questo è il numero'),
    ('e non e Dio delle valli', 'e non è Dio delle valli'),
    ('questa non e una parola senza valore', 'questa non è una parola senza valore'),
    ('perché questo e il tutto dell’uomo', 'perché questo è il tutto dell’uomo'),
    ('il mio capo e coperto di rugiada', 'il mio capo è coperto di rugiada'),
    ('e le me guance, a chi mi strappava', 'e le mie guance, a chi mi strappava'),
    ('di calcare i mie cortili', 'di calcare i miei cortili'),
    ('Questo e un grave lutto', 'Questo è un grave lutto'),
    ('perch’egli e il nostro Dio', 'perch’egli è il nostro Dio'),
    ('E chi e il figliuolo d’Isai', 'E chi è il figliuolo d’Isai'),
    ('Questo e lo scritto', 'Questo è lo scritto'),
    ('dell’Eterno fu rivolta Giona', 'dell’Eterno fu rivolta a Giona'),
    ('Perché hanno tritano Galaad', 'Perché hanno tritato Galaad'),
    ('han sprezzato al legge', 'han sprezzato la legge'),
    ('l’Eterno mi perse di dietro al gregge', 'l’Eterno mi prese di dietro al gregge'),
    ('a motivo della grava età', 'a motivo della grave età'),
    ('venuto a chiamare i de’ giusti', 'venuto a chiamare de’ giusti'),
    ('della conoscenza do Cristo Gesù', 'della conoscenza di Cristo Gesù'),
    ('voglion vivere pienamente in Cristo', 'voglion vivere piamente in Cristo'),
]

# Rimandi alla numerazione originale italiana, es. "(21:6)" o "(H21-6)".
MARKER = re.compile(r'\(H?\d+[:-]\d+\)\s*')


def norm_words(s):
    return re.findall(r'\w+', s.lower())


def load_ebible(text_path, vref_path):
    out = {}
    with open(vref_path, encoding='utf-8') as refs, open(text_path, encoding='utf-8') as txt:
        for ref, t in zip(refs, txt):
            b, cv = ref.split()
            t = t.strip()
            if b in USFM and t and t != '<range>':
                c, v = cv.split(':')
                out[(USFM.index(b) + 1, int(c), int(v))] = t
    return out


def split_heading(full, body):
    """Separa il titolo del Salmo dal testo eBible usando il corpo OSIS come guida."""
    words = norm_words(body)[:3]
    if not words:
        return None, full
    pattern = r'\W+'.join(re.escape(w) for w in words)
    m = re.search(pattern, full, flags=re.IGNORECASE)
    if not m or m.start() == 0:
        return None, full
    return full[:m.start()].strip(), full[m.start():].strip()


def curly_quotes(s):
    """Il file OSIS usa virgolette dritte: le converte come in eBible (“…”).
    Una virgoletta a inizio testo o dopo uno spazio apre, altrimenti chiude."""
    out = []
    for i, ch in enumerate(s):
        if ch == '"':
            out.append('\u201c' if i == 0 or s[i - 1] in ' (' else '\u201d')
        else:
            out.append(ch)
    return ''.join(out)


def clean(s):
    for k, v in CP1252_FIX.items():
        s = s.replace(k, v)
    s = MARKER.sub('', s)
    s = s.replace('\t', ' ').replace('\n', ' ')
    return re.sub(r' {2,}', ' ', s).strip()


def convert_bible(osis_path, ebible, out_dir):
    root = ET.parse(osis_path).getroot()
    stats = {'ebible': 0, 'osis': 0}
    os.makedirs(out_dir, exist_ok=True)
    meta = []
    verses_per_chapter = {}
    total = 0
    for div in root.iter(NS + 'div'):
        if div.get('type') != 'book':
            continue
        bid = OSIS_TO_ID[div.get('osisID')]
        lines = []
        chapters = 0
        for ch in div.iter(NS + 'chapter'):
            c = int(ch.get('osisID').split('.')[1])
            chapters = max(chapters, c)
            for v in ch.iter(NS + 'verse'):
                n = int(v.get('osisID').split('.')[2])
                heading = ''
                note = v.find(NS + 'note')
                if note is not None:
                    heading = clean(''.join(note.itertext()))
                    tail = note.tail or ''
                    body = (v.text or '') + tail + ''.join(
                        ''.join(x.itertext()) + (x.tail or '') for x in v if x is not note)
                else:
                    body = ''.join(v.itertext())
                body = clean(body)
                key = (bid, c, n)
                if key in ebible:
                    full = clean(ebible[key])
                    for old, new in EBIBLE_FIXES:
                        full = full.replace(old, new)
                    if heading:
                        # Se il titolo non si separa in modo pulito (OSIS a volte mette la nota
                        # nel punto sbagliato, es. Salmo 60) si lascia il testo eBible intero.
                        heading, full = split_heading(full, body)
                    body = full
                    stats['ebible'] += 1
                else:
                    for old, new in FIXES.get(key, []):
                        if old not in body:
                            raise SystemExit(f'Correzione non applicabile in {key}: {old}')
                        body = body.replace(old, new)
                    body = curly_quotes(body)
                    stats['osis'] += 1
                if not body:
                    raise SystemExit(f'Versetto vuoto: {v.get("osisID")}')
                if heading:
                    lines.append(f'{c}\t0\t{heading}')
                lines.append(f'{c}\t{n}\t{body}')
                verses_per_chapter[(bid, c)] = max(verses_per_chapter.get((bid, c), 0), n)
                total += 1
        with open(os.path.join(out_dir, f'{bid:02d}.txt'), 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
        osis, name, abbr = BOOKS[bid - 1]
        counts = ','.join(str(verses_per_chapter[(bid, c)]) for c in range(1, chapters + 1))
        meta.append(f'{bid}\t{osis}\t{name}\t{abbr}\t{"AT" if bid <= 39 else "NT"}\t{counts}')
    with open(os.path.join(out_dir, 'libri.tsv'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(meta) + '\n')
    if len(meta) != 66:
        raise SystemExit(f'Attesi 66 libri, trovati {len(meta)}')
    print('testo da eBible', stats['ebible'], '- da OSIS', stats['osis'])
    return total, verses_per_chapter


def parse_ref(r):
    b, c, v = r.split('.')
    return OSIS_TO_ID[b], int(c), int(v)


def convert_xrefs(path, out_dir, vpc, per_verse=6, min_votes=3):
    by_verse = {}
    with open(path, encoding='utf-8') as f:
        next(f)
        for line in f:
            parts = line.rstrip('\n').split('\t')
            if len(parts) < 3:
                continue
            frm, to, votes = parts[0], parts[1], int(parts[2])
            if votes < min_votes:
                continue
            try:
                fb, fc, fv = parse_ref(frm)
                ends = [parse_ref(x) for x in to.split('-')]
            except (KeyError, ValueError):
                continue
            tb, tc, tv = ends[0]
            if (tb, tc) not in vpc or tv > vpc[(tb, tc)]:
                continue
            target = f'{tb}.{tc}.{tv}'
            if len(ends) == 2:
                eb, ec, ev = ends[1]
                if eb != tb:
                    continue
                target += f'-{ec}.{ev}' if ec != tc else f'-{ev}'
            by_verse.setdefault((fb, fc, fv), []).append((votes, target))
    xdir = os.path.join(out_dir, 'xref')
    os.makedirs(xdir, exist_ok=True)
    files = {}
    for (b, c, v), refs in sorted(by_verse.items()):
        refs.sort(key=lambda x: -x[0])
        files.setdefault(b, []).append(f'{c}\t{v}\t' + ';'.join(t for _, t in refs[:per_verse]))
    for b, lines in files.items():
        with open(os.path.join(xdir, f'{b:02d}.txt'), 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
    return sum(len(x) for x in files.values())


if __name__ == '__main__':
    osis, ebible_txt, vref, xrefs, out = sys.argv[1:6]
    total, vpc = convert_bible(osis, load_ebible(ebible_txt, vref), out)
    print('versetti', total, 'capitoli', len(vpc))
    print('versetti con riferimenti', convert_xrefs(xrefs, out, vpc))

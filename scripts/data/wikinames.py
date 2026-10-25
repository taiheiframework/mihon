"""Given and family names from Wikidata (CC0): the names most often borne by people with the
citizenship of a language's default country, counted per name; only those counts and the names'
own labels are kept, nothing about any person. A given name goes on the female or male list by
the gender of its bearers (both lists when at least a quarter are of each); the label in the
language is the name, the English label its romanised form unless a rule in romanize.py gives one.
"""

import collections
import unicodedata

import romanize
import text
import unihan
from common import sparql
from languages import COUNTRY_QID

FEMALE, MALE = "http://www.wikidata.org/entity/Q6581072", "http://www.wikidata.org/entity/Q6581097"


def _qid(uri):
    return uri.rsplit("/", 1)[1]


# The citizenships whose people a country's names are counted over: many Dutch people on Wikidata
# are citizens of the Kingdom of the Netherlands rather than of the Netherlands.
CITIZENSHIPS = {"NL": ["Q55", "Q29999"]}
# People born since 1920 only, so that the names are those of the living rather than of history.
BORN_SINCE = '?p wdt:P569 ?born . FILTER(?born >= "1920-01-01T00:00:00Z"^^xsd:dateTime)'


def _counts(kind, country, prop, by_gender):
    values = " ".join("wd:" + q for q in CITIZENSHIPS.get(country, [COUNTRY_QID[country]]))
    if by_gender:
        rows = sparql(f"{kind}-{country}", f"""
            SELECT ?name ?g (COUNT(?p) AS ?c) WHERE {{
              VALUES ?country {{ {values} }}
              ?p wdt:P27 ?country; wdt:{prop} ?name; wdt:P21 ?g .
              {BORN_SINCE}
              FILTER(?g IN (wd:Q6581072, wd:Q6581097))
            }} GROUP BY ?name ?g ORDER BY DESC(?c) LIMIT 1500""")
        out = collections.defaultdict(lambda: [0, 0])
        for r in rows:
            out[_qid(r["name"])][0 if r["g"] == FEMALE else 1] += int(r["c"])
        return out
    rows = sparql(f"{kind}-{country}", f"""
        SELECT ?name (COUNT(?p) AS ?c) WHERE {{
          VALUES ?country {{ {values} }}
          ?p wdt:P27 ?country; wdt:{prop} ?name .
          {BORN_SINCE}
        }} GROUP BY ?name ORDER BY DESC(?c) LIMIT 800""")
    return {_qid(r["name"]): int(r["c"]) for r in rows}


def _labels(kind, country, lang, ids):
    rows = []
    ids = sorted(ids)
    for start in range(0, len(ids), 400):
        chunk = " ".join("wd:" + i for i in ids[start:start + 400])
        rows += sparql(f"{kind}-labels-{country}-{lang}-{start}", f"""
            SELECT ?item ?label ?native ?en ?kana ?female ?ws WHERE {{
              VALUES ?item {{ {chunk} }}
              OPTIONAL {{ ?item rdfs:label ?label . FILTER(LANG(?label) = "{lang}") }}
              OPTIONAL {{ ?item wdt:P1705 ?native . FILTER(LANG(?native) = "{lang}") }}
              OPTIONAL {{ ?item rdfs:label ?en . FILTER(LANG(?en) = "en") }}
              OPTIONAL {{ ?item wdt:P1814 ?kana }}
              OPTIONAL {{ ?item wdt:P2521 ?female . FILTER(LANG(?female) = "{lang}") }}
              OPTIONAL {{ ?item wdt:P282 ?ws }}
            }}""")
    systems = collections.defaultdict(set)
    for r in rows:
        if "ws" in r:
            systems[_qid(r["item"])].add(_qid(r["ws"]))
    out = {}
    for r in sorted(rows, key=lambda r: (r["item"], r.get("label", ""), r.get("en", ""), r.get("kana", ""), r.get("female", ""), r.get("ws", ""))):
        r = dict(r, systems=systems.get(_qid(r["item"]), set()))
        # Serbian labels on Wikidata are in either script: a Latin one is turned into Cyrillic.
        if lang == "sr" and "label" in r and not any("\u0400" <= c <= "\u04ff" for c in r["label"]):
            cyr = romanize.serbian_cyrillic(r["label"])
            if cyr:
                r["label"] = cyr
            else:
                del r["label"]
        # The native label (P1705) where the item has no label in the language.
        if "label" not in r and "native" in r:
            r["label"] = r["native"]
        out.setdefault(_qid(r["item"]), r)
    return out


# The writing systems (P282) a name item of each script may state: an item that states others
# only (the Latin-script "Elena", whose Russian label is a transcription) is not the language's own
# spelling of the name.
SCRIPT_ITEMS = {"Latn": {"Q8229"}, "Cyrl": {"Q8209"}, "Grek": {"Q8216"}, "Arab": {"Q8196"},
                "Hebr": {"Q33513"}, "Deva": {"Q38592"}, "Thai": {"Q236376"},
                "Hans": {"Q8201", "Q185614"}, "Hant": {"Q8201", "Q178528"},
                "Jpan": {"Q82772", "Q48332", "Q82946", "Q8201"}, "Kore": {"Q8222", "Q8201"}}

# The languages whose names Wikidata spells in their own script as items of their own (Елена
# beside the Latin-script Elena), so that an item written in another script only is a
# transcription. Elsewhere (Arabic, Japanese) a Latin-script item's label in the language is the
# native name.
STRICT_WRITING = {"ru", "uk", "bg", "el", "th", "he"}

# English and other European names written in another script (जॉन, John), which Wikidata counts
# for people of India but which are no Hindi names; left out by their English label.
ENGLISH_NAMES = {"John", "Mary", "Paul", "Michael", "Peter", "David", "James", "George", "Thomas",
                 "William", "Robert", "Richard", "Joseph", "Charles", "Elizabeth", "Anne", "Margaret",
                 "Jennifer", "Daniel", "Mark", "Hans", "Francisco", "Oliver", "Edward", "Leonard",
                 "Arthur", "Martin", "Pavel", "Taylor", "Barry", "Carlos", "Tito", "Leander", "Jose", "José"}
FOREIGN_NAMES_LEFT_OUT = {"hi"}

# Names that Wikidata records as given names but that are none: the Vietnamese middle names Thị
# and Văn, a nickname ("macaroni"), Hindi labels that translate an English label instead of
# naming (समानता "equality" for Samantha, हवा "air" for Pawan), and Hindi labels that hold more
# than a name (an honorific, a family name, an epithet, "the Telugu language"), and Kaur, which
# stands after a Sikh woman's given name rather than for it. Thai: the Spanish Manuela and the
# Hindi Ritu, name items in Thai script with no Thai bearer, transcriptions of foreign names. By list: Maria or
# Marie as a man's name is the second half of Jean-Marie or Jan Maria, and ललिता is a woman's name.
NOT_GIVEN = {"vi": {"Thị", "Văn"}, "ar": {"مكرونه", "مكرونة"}, "th": {"มานูเอลา", "ริตู"},
             "hi": {"समानता", "संतुष्टि", "हवा", "मोहिनी अवतार", "श्री गायत्री देवी", "सुयश दास", "तेलुगु भाषा",
                    "कौर"}}
NOT_GIVEN_BY_GENDER = {(t, "male"): {"Maria", "Marie", "María"} for t in ("fr", "nl", "it", "pl", "de", "es", "pt")}
NOT_GIVEN_BY_GENDER[("hi", "male")] = {"ललिता"}

# Hindi labels of family-name items that are words, not names: बहन "sister", पति "husband".
NOT_FAMILY = {"hi": {"बहन", "पति"}}

# English labels that give an Arabic name in another language's or a Biblical form.
LATIN_FIXES = {"ar": {"زينب": "Zainab", "سليمان": "Suleiman"}, "hi": {"मोहित": "Mohit"}}

# Dutch writes the particles of a family name in lower case after a given name: Jan de Vries.
DUTCH_PARTICLES = ("Van der ", "Van den ", "Van de ", "Van ", "De ", "Den ", "Ter ", "Ten ", "Het ", "'T ")


def _native_form(tag, name):
    """The name as the data keeps it: Arabic without vowel marks or tatweel, so that مُصطفى and
    مصطفى are one name, and with the Arabic ي for the Persian ی (مریم is مريم); Dutch particles in
    lower case."""
    if tag == "ar":
        name = "".join(c for c in name if not unicodedata.combining(c) and c != "\u0640")
        name = name.replace("\u06cc", "\u064a")
    if tag == "nl":
        for p in DUTCH_PARTICLES:
            if name.startswith(p):
                return p.lower() + name[len(p):]
    return name

# The letters each Cyrillic alphabet has: a Russian name with the Serbian Ј is no Russian name.
CYRILLIC_LETTERS = {
    "ru": set("абвгдеёжзийклмнопрстуфхцчшщъыьэюя"),
    "uk": set("абвгґдеєжзиіїйклмнопрстуфхцчшщьюя’'"),
    "bg": set("абвгдежзийклмнопрстуфхцчшщъьюя"),
    "sr-Cyrl": set("абвгдђежзијклљмнњопрстћуфхцчџш"),
}


def _name_ok(name, script, tag=None, systems=frozenset()):
    letters = name.replace(" ", "").replace("-", "").replace("'", "").replace("’", "")
    if not letters or not text.in_script(letters, script):
        return False
    if tag in STRICT_WRITING and systems and not systems & SCRIPT_ITEMS.get(script, set()):
        return False
    if tag in CYRILLIC_LETTERS and not set(letters.lower()) <= CYRILLIC_LETTERS[tag]:
        return False
    if tag == "th" and " " in name:
        return False  # Thai writes a given or a family name as one word
    if tag == "ar" and any(len(part) < 3 for part in name.split()):
        return False  # a compound cut short, such as عبد ال
    return True


def _latin_ok(s):
    return bool(s) and all(c.isalpha() or c in " -'’" for c in s) and \
        all(ord(c) < 0x250 or 0x1E00 <= ord(c) <= 0x1EFF for c in s)


# The Latin initials a name's first letter may have in a romanisation, for the scripts whose
# latin form is an English label: a label whose initial does not fit belongs to another name.
ARABIC_INITIALS = {
    "ا": "AEIOU", "أ": "AEIOU", "إ": "EI", "آ": "A", "ع": "AEIOU", "ب": "B", "ت": "T", "ث": "TS",
    "ج": "JGD", "ح": "H", "خ": "K", "د": "D", "ذ": "DZT", "ر": "R", "ز": "Z", "س": "S", "ش": "SC",
    "ص": "S", "ض": "D", "ط": "T", "ظ": "ZD", "غ": "G", "ف": "F", "ق": "QKG", "ك": "K", "ل": "L",
    "م": "M", "ن": "N", "ه": "H", "و": "WOU", "ي": "YIE",
}
THAI_INITIALS = {
    "ก": "KG", "ข": "K", "ฃ": "K", "ค": "K", "ฅ": "K", "ฆ": "K", "ง": "N", "จ": "JC", "ฉ": "C",
    "ช": "CS", "ซ": "S", "ฌ": "C", "ญ": "YN", "ฎ": "D", "ฏ": "T", "ฐ": "T", "ฑ": "TD", "ฒ": "T",
    "ณ": "N", "ด": "D", "ต": "T", "ถ": "T", "ท": "T", "ธ": "T", "น": "N", "บ": "BP", "ป": "PB",
    "ผ": "P", "ฝ": "F", "พ": "PB", "ฟ": "F", "ภ": "P", "ม": "M", "ย": "Y", "ร": "R", "ฤ": "R",
    "ล": "L", "ฦ": "L", "ว": "WV", "ศ": "S", "ษ": "S", "ส": "S", "ห": "H", "ฬ": "L", "อ": "AEIOU",
    "ฮ": "H",
}
THAI_LEADING_VOWELS = set("เแโใไ")


def _initial_matches(tag, name, latin):
    if not latin or not latin[0].isupper():
        return False  # a nickname or a fault, such as "makarona"
    if tag == "ar":
        bare = name[2:] if name.startswith("ال") and len(name) > 2 else name
        rest = latin[3:] if bare is not name and latin[:3].lower() in ("al-", "el-", "al ", "el ") else latin
        rest = rest[2:] if bare is not name and rest[:2].lower() in ("al", "el") and rest is latin else rest
        options = ARABIC_INITIALS.get(bare[:1])
        return options is None or rest[:1].upper() in options
    if tag == "th":
        if " " in latin:
            return False  # "Krairiksh family"
        first = name.lstrip("".join(THAI_LEADING_VOWELS))[:1]
        options = THAI_INITIALS.get(first)
        return options is None or latin[0] in options
    return True


def _form(tag, script, name, en, kana, family_name=False):
    """The data entry for a name: its latin, ascii and reading where they differ, or None when it
    cannot be romanised."""
    entry = {"name": name}
    if script == "Latn":
        latin = name
    elif script == "Cyrl":
        latin = romanize.cyrillic(name, tag)
    elif script == "Grek":
        latin = romanize.greek(name)
    elif script == "Kore":
        latin = en if _latin_ok(en) else romanize.hangul(name)
    elif script == "Jpan":
        if not _latin_ok(en):
            return None
        latin = romanize.passport_hepburn(en)
        reading = romanize.hiragana_to_katakana(kana) if kana else romanize.hepburn_to_katakana(en)
        if not reading or not text.in_script(reading, "Jpan"):
            return None
        entry["reading"] = reading
    elif script in ("Hans", "Hant"):
        # Pinyin without tone marks; a family name by Unihan's readings, as in a passport.
        if family_name:
            latin = unihan.pinyin(name)
        else:
            latin = romanize.ascii_fold(en) if _latin_ok(en) else None
        if not latin or not latin.isascii() or not latin.isalpha():
            return None
    else:
        if not _latin_ok(en):
            return None
        # An English label may carry a scholarly transcription (Faraḥ) or another language's
        # letters (Hafız): the latin form is its plain Latin letters, each part capitalised
        # ("Al dawod" is Al Dawod).
        latin = " ".join(p[:1].upper() + p[1:] for p in romanize.ascii_fold(en).split(" "))
        if not _initial_matches(tag, name, latin):
            return None  # a label of another name (غادة labelled Hamada)
    if latin != name:
        entry["latin"] = latin
    ascii_form = romanize.ascii_fold(latin)
    if ascii_form != latin:
        entry["ascii"] = ascii_form
    if not all(" " <= c <= "~" for c in entry.get("ascii", latin)):
        return None
    return entry


# The writing system (P282) of the name items of a language whose people carry too few name
# statements on Wikidata, or whose counted names mostly have no label in the language (Thai given
# names): its name items are added to the counted ones.
WRITING_SYSTEM = {"th": "Q236376"}


def _native(kind, cls, lang):
    """Name items of the class whose native label (P1705) is in the language, or that are written
    in its script, for a language in WRITING_SYSTEM or a country whose people carry too few name
    statements on Wikidata: each counts as borne once."""
    script = WRITING_SYSTEM.get(lang)
    by_script = f"UNION {{ ?item wdt:P31 wd:{cls}; wdt:P282 wd:{script} }}" if script else ""
    rows = sparql(f"{kind}-native-{lang}", f"""
        SELECT DISTINCT ?item WHERE {{
          {{ ?item wdt:P31 wd:{cls}; wdt:P1705 ?n . FILTER(LANG(?n) = "{lang}") }} {by_script}
        }}""")
    return [_qid(r["item"]) for r in rows]


# The languages whose own names Wikidata often records as items in Latin script, with the label
# in the language beside them (Sunisa, labelled สุนิสา), so that the writing-system rule of
# STRICT_WRITING would leave them out with the transcriptions of foreign names (Anna, แอนนา).
OWN_ROMANISED = {"th"}


def _own_romanised(country, script, tag, counts, labels, added):
    """The name items in another script whose label in the language is the language's own name:
    those that at least half of their bearers on Wikidata (born since 1920) bear as citizens of the
    country. An added item has no counted bearer, so it is no candidate. Only the counts are
    read, nothing about any person."""
    candidates = sorted(item for item, r in labels.items()
                        if item in counts and item not in added and r.get("label")
                        and _name_ok(r["label"], script)
                        and not _name_ok(r["label"], script, tag, r.get("systems", set())))
    if not candidates:
        return set()
    values = " ".join("wd:" + i for i in candidates)
    rows = sparql(f"given-bearers-{country}", f"""
        SELECT ?name (COUNT(?p) AS ?c) WHERE {{
          VALUES ?name {{ {values} }}
          ?p wdt:P735 ?name .
          {BORN_SINCE}
        }} GROUP BY ?name""")
    everywhere = {_qid(r["name"]): int(r["c"]) for r in rows}
    return {i for i in candidates if 2 * sum(counts[i]) >= everywhere.get(i, 0) > 0}


def entry_latin(en):
    return romanize.ascii_fold(en).replace("-", "").replace(" ", "").capitalize() if en else None


def given(tag, conf, limit=120):
    country, script, lang = conf["country"], conf["script"], conf["wikidata"]
    counts = _counts("given", country, "P735", True)
    added = set()
    # Thai people on Wikidata carry many given-name statements, but most of those names have no
    # Thai label, so the counted names alone fill too short a female list: the name items written
    # in the script are added there too, as they are for family names.
    if len(counts) < 2 * limit or lang in WRITING_SYSTEM:
        # A given name without counted bearers gets a weight of one, in the list of its class.
        for cls, i in (("Q11879590", 0), ("Q12308941", 1)):
            for item in _native(f"given-{cls}", cls, lang):
                if item not in counts:
                    counts[item] = [1, 0] if i == 0 else [0, 1]
                    added.add(item)
    labels = _labels("given", country, lang, counts)
    own = _own_romanised(country, script, tag, counts, labels, added) if tag in OWN_ROMANISED else set()
    lists = {"female": [], "male": []}
    # At the same weight a name with a counted bearer comes before an added one, so an added name
    # fills a list but never takes the place of a counted one.
    for item, (f, m) in sorted(counts.items(), key=lambda kv: (-sum(kv[1]), kv[0] in added, kv[0])):
        r = labels.get(item, {})
        name = _native_form(tag, r["label"]) if r.get("label") else None
        systems = set() if item in own else r.get("systems", set())
        if not name or not _name_ok(name, script, tag, systems):
            continue
        if script in ("Hans", "Hant") and len(name) > 2:
            continue  # a transcribed foreign name, such as 约瑟夫
        entry = _form(tag, script, name, r.get("en"), r.get("kana"))
        if not entry:
            continue
        if tag in PATRONYMIC and name.endswith(PATRONYMIC[tag]):
            continue  # a patronymic recorded as a given name, such as Сергеевна
        if name in NOT_GIVEN.get(tag, set()):
            continue
        if tag in FOREIGN_NAMES_LEFT_OUT and r.get("en") in ENGLISH_NAMES:
            continue
        if script in ("Hans", "Hant") and unihan.pinyin(name, surname=False) != entry_latin(r.get("en")):
            continue  # an English name in characters (辛迪, Cindy), whose label is no pinyin
        if name in LATIN_FIXES.get(tag, {}):
            entry["latin"] = LATIN_FIXES[tag][name]
        total = f + m
        for key, n in (("female", f), ("male", m)):
            if name in NOT_GIVEN_BY_GENDER.get((tag, key), set()):
                continue
            # On both lists only when at least a third of its bearers are of each gender:
            # gender statements on Wikidata are not free of faults.
            if n * 3 < total:
                continue
            same = next((e for e in lists[key] if e["name"] == name), None)
            if same:
                same["weight"] += n  # one name under two items, such as مُصطفى and مصطفى
            elif len(lists[key]) < limit:
                lists[key].append(dict(entry, weight=n))
    return {k: _merge_unaccented(v) for k, v in lists.items()}


def _merge_unaccented(entries):
    """A name written without its diacritics (Jose beside José, Nguyen beside Nguyễn) is the
    same name: its weight goes to the accented one."""
    by_name = {e["name"]: e for e in entries}
    out = []
    for e in entries:
        accented = [o for o in entries if o is not e and o["name"] != e["name"] and romanize.ascii_fold(o["name"]) == e["name"]]
        if accented:
            by_name[accented[0]["name"]]["weight"] += e["weight"]
        else:
            out.append(e)
    return out


PATRONYMIC = {t: ("вна", "вич", "чна", "ична") for t in ("ru", "uk", "bg", "sr-Cyrl")}

FEMALE_RULES = {
    "ru": [("ский", "ская"), ("цкий", "цкая"), ("ой", "ая"), ("ов", "ова"), ("ев", "ева"), ("ёв", "ёва"), ("ин", "ина"), ("ын", "ына")],
    "bg": [("ски", "ска"), ("ов", "ова"), ("ев", "ева"), ("ин", "ина")],
    "uk": [("ський", "ська"), ("цький", "цька"), ("ов", "ова"), ("ев", "ева"), ("ін", "іна"), ("ин", "ина")],
    "pl": [("ski", "ska"), ("cki", "cka"), ("dzki", "dzka")],
    # The stress of -όπουλος moves in the genitive the female form is: Παπαδοπούλου.
    "el": [("όπουλος", "οπούλου"), ("ος", "ου"), ("ης", "η"), ("ας", "α"), ("ός", "ού"), ("ής", "ή"), ("άς", "ά")],
}


def _female(tag, name, wikidata_female):
    """The female form by the language's rule (Жуков is Жукова), or Wikidata's where no rule
    applies: its P2521 is sometimes another name (Жуковская)."""
    for male, female in FEMALE_RULES.get(tag, []):
        if name.endswith(male):
            return name[: -len(male)] + female
    if wikidata_female and wikidata_female != name:
        return wikidata_female
    return None


def _is_female_form(tag, name):
    return any(name.endswith(female) and len(name) > len(female) + 1 for _, female in FEMALE_RULES.get(tag, []))


def family(tag, conf, limit=160):
    country, script, lang = conf["country"], conf["script"], conf["wikidata"]
    counts = _counts("family", country, "P734", False)
    if lang in WRITING_SYSTEM:
        for item in _native("family-Q101352", "Q101352", lang):
            counts.setdefault(item, 1)
    labels = _labels("family", country, lang, counts)
    out, seen = [], set()
    for item, n in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0])):
        r = labels.get(item, {})
        name = _native_form(tag, r["label"]) if r.get("label") else None
        if name in seen:
            next(e for e in out if e["name"] == name)["weight"] += n
            continue
        if not name or name in seen or not _name_ok(name, script, tag, r.get("systems", set())):
            continue
        if script in ("Hans", "Hant") and len(name) > 2:
            continue  # a transcribed foreign name
        if _is_female_form(tag, name):
            continue  # Петрова is the female form of Петров, not a family name of its own
        if tag in FOREIGN_NAMES_LEFT_OUT and r.get("en") in ENGLISH_NAMES:
            continue
        if name in NOT_FAMILY.get(tag, set()):
            continue
        entry = _form(tag, script, name, r.get("en"), r.get("kana"), family_name=True)
        if not entry:
            continue
        if tag in FEMALE_RULES:
            female = _female(tag, name, r.get("female"))
            if female:
                form = _form(tag, script, female, None if script != "Latn" else female, None)
                if script == "Latn":
                    form = {"name": female, **({"ascii": romanize.ascii_fold(female)} if romanize.ascii_fold(female) != female else {})}
                if form:
                    entry["female"] = form
        if name in LATIN_FIXES.get(tag, {}):
            entry["latin"] = LATIN_FIXES[tag][name]
        if len(out) < limit:
            out.append(dict(entry, weight=n))
            seen.add(name)
    return _merge_unaccented(out)


def names(tag, conf):
    g = given(tag, conf)
    return {"female": g["female"], "male": g["male"], "family": family(tag, conf)}

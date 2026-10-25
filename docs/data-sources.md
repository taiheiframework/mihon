# Data sources

Every value Mihon draws from a language's data comes from the sources below. The data files are
under `shared/resources/dev/taihei/mihon/data/`; the scripts in `scripts/data/` derive them. The
build never runs the scripts and needs nothing but Maven Central; the raw downloads stay in a
cache outside the repository (`MIHON_DATA_CACHE`, `~/.cache/taihei-mihon-data` by default) and
never enter it. `scripts/data/sources.lock.json` records the URL, size, retrieval date and SHA-256
of each download; the table at the end of this page is generated from it. Tatoeba publishes
its exports weekly under the same URL, so a later download differs: the date and SHA-256 identify
the file the data was built from.

Each source's licence was checked before its data was committed. Only single words and names are
taken; no sentence, and no span of more than one word, from any source is reproduced.

## Sources

| What | Source | Licence |
| --- | --- | --- |
| Country names in each language, quotation marks, the default country of each language (likely subtags) | [Unicode CLDR](https://cldr.unicode.org) 47.0.0, the `cldr-localenames-full`, `cldr-misc-full` and `cldr-core` JSON packages | [Unicode License v3](https://www.unicode.org/license.txt), in `LICENSE-UNICODE` |
| Words of each language's text (except Latin) | Word frequencies counted from the per-language sentence exports of the [Tatoeba Project](https://tatoeba.org), written by the Tatoeba contributors | [CC BY 2.0 FR](https://creativecommons.org/licenses/by/2.0/fr/), attributed in `NOTICE` |
| Pinyin of Chinese family names | The kMandarin readings of the [Unicode Han Database](https://www.unicode.org/charts/unihan.html) (Unihan) 16.0.0, tone marks removed, with the readings family names take where they differ (`scripts/data/unihan.py`) | [Unicode License v3](https://www.unicode.org/license.txt), in `LICENSE-UNICODE` |
| Latin text | The words of Lorem Ipsum, derived from Cicero, *De finibus bonorum et malorum* (45 BC), in `scripts/data/lorem.txt` | public domain |
| United States given names and family names | [U.S. Census Bureau](https://www.census.gov/topics/population/genealogy/data.html): the 1990 first-name lists (`dist.female.first`, `dist.male.first`) and the 2010 surname list | U.S. Government work, 17 U.S.C. 105 |
| Latin given and family names | Classical Roman praenomina and nomina gentilicia, written in `scripts/data/names.py` | historical facts, written for this library (Apache 2.0) |
| Cities, their regions, Latin country names, and the given and family names of the other languages | [Wikidata](https://www.wikidata.org), through its Query Service; the queries are in `scripts/data/` | [CC0 1.0](https://www.wikidata.org/wiki/Wikidata:Licensing) |
| Address formats, postal code patterns, street patterns, phone patterns | Written for this library in `scripts/data/formats.py`, after the address metadata of [Google libaddressinput](https://github.com/google/libaddressinput); UPU material was used as a reference only | written for this library (Apache 2.0), based on data under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), attributed in `NOTICE` |

## Filters, used by the scripts and never shipped

- The words of each language are checked against the [LDNOOBW](https://github.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words)
  lists (CC BY 4.0) at commit `5faf2ba42d7b1c0977169ec3611df25a3c08eb13`, and every word on them is
  removed. The lists are downloaded into the cache, never committed. Six of the 25 languages
  have no list there: Vietnamese, Ukrainian, Bulgarian, Serbian, Greek and Hebrew.
- In scripts with case, a word that is capitalised wherever it stands inside a sentence is a
  proper noun and is removed (German keeps it: it capitalises nouns). The recurring characters of
  Tatoeba's sentences are removed in every script, and in German and the scripts without case
  the language's names too.
- Tatoeba's Mandarin sentences are in both Simplified and Traditional characters. A sentence
  counts for `zh-Hans` when it holds a character only Simplified writing uses and none only
  Traditional writing uses, by Unihan's kSimplifiedVariant and kTraditionalVariant; nothing of
  that classification ships.
- A few frequent words reflect what some of Tatoeba's sentences argue about rather than the
  language (the Arabic for separatists, the Italian for nationality, the Bulgarian for French),
  and are removed. Tatoeba's recurring characters are also removed behind a Hebrew or Arabic
  one-letter prefix (לתום, لتوم) and with the German genitive s (Toms).
- Japanese segments contain pieces of inflections: a word that starts with a small kana, ん or ー,
  or a single hiragana that is not a particle, is removed.
- Chinese, Japanese and Thai, which write no space between words, are split into words with
  [ICU4J](https://github.com/unicode-org/icu) 77.1 from Maven Central (Unicode License v3), run by
  `scripts/data/segment.py` only, never by the build or the library.

## Telephone numbers

Mihon's phone numbers are in a range reserved for fiction only where the country reserves one and
the reservation is cited here; elsewhere a number may be real.

| Country | Range | Primary source |
| --- | --- | --- |
| United States (NANP) | 555-0100 to 555-0199, any area code | [NANPA: 555 line numbers](https://www.nanpa.com/numbering/555-line-numbers) |

## Processing

- **Words.** Tatoeba's sentences of a language are split into words; a word must consist of
  letters of the language's script. The 1300 most frequent words that pass the filters are kept,
  each weighted by the square root of its count, so that the commonest words do not crowd out
  the rest.
- **Capitals.** For a script with case, the data holds the capital of each initial its words use,
  computed by the script (`str.upper`, with Turkish `i` to `İ` and Dutch `ij` to `IJ`), so that no
  platform case mapping runs at run time and the JVM and Scala.js cannot differ.
- **Names.** The Census lists keep their top 200 given names per gender and 300 surnames,
  weighted by frequency. From Wikidata, a language keeps the 120 given names per gender and the
  160 family names most often borne by people born since 1920 with the citizenship of its
  default country (P27 with P735 or P734; for the Netherlands also of the Kingdom of the
  Netherlands), weighted by that count; only the counts and the names' own labels are
  taken, nothing about any person. A given name is on the female or the male list by the gender
  of its bearers (both, when at least a third are of each). Where a country's people carry too
  few name statements, or most of their names have no label in the language (Thailand), name
  items with a native label in the language (P1705), or written in its script (P282), are added
  with a weight of one, after the counted names of the same weight. Names longer than two characters
  are left out in Chinese, where they are transcribed foreign names. In Russian, Ukrainian,
  Bulgarian, Serbian, Greek and Thai, whose names Wikidata spells as items of their own, a name
  item that states only another writing system (P282) is a transcription and is left out, except
  in Thai when at least half of the item's bearers on Wikidata are Thai citizens: Thai names are often
  recorded as Latin-script items with a Thai label (Sunisa, สุนิสา), where Anna (แอนนา), borne
  mostly elsewhere, stays out; and a
  Cyrillic name must use only its alphabet's letters (no Serbian Ј in a Russian name); a Thai name
  holds no space, and an Arabic name no part shorter than three letters (a compound cut short).
  Where the latin form is an English label (Arabic, Thai), its initial must fit the name's first
  letter, so a label of another name (غادة labelled Hamada) is left out; a Chinese given name must
  read as its label's pinyin, so an English name written in characters (辛迪, Cindy) is left out.
  A Russian, Ukrainian, Bulgarian, Polish or Greek family name gets its female form by rule
  (Петров, Петрова; Παπαδόπουλος, Παπαδοπούλου), and a female form Wikidata lists as a name of its
  own is left out; patronymics (Сергеевна) and the Vietnamese middle names Thị and Văn are not
  given names. Hebrew is spelled from its own name items, as Russian is; in Hindi, the commonest
  English names in Devanagari (जॉन, John) are left out by their English label. One name
  under two items (Arabic with and without vowel marks or tatweel, a name with and without its
  diacritics) is one entry with both weights. Dutch writes the particles of a family name in lower
  case (de Vries). A few names are corrected or left out by hand in `scripts/data/wikinames.py`:
  an Arabic nickname, Maria and Marie as a man's name in seven Latin-script languages (on their own
  they are the second half of Jean-Marie or Jan Maria), the Arabic latin forms Zainab and
  Suleiman where the English label has Zeynep and Solomon, Hindi labels that translate an English
  name instead of naming (समानता, "equality", for Samantha) or hold more than a name, the latin
  form Mohit, the most frequent European names written in Devanagari, and two foreign names
  written in Thai script (มานูเอลา, Manuela; ริตู, Ritu). Arabic names use the Arabic ي where a
  label has the Persian ی.
- **Romanisation.** The `latin` form is computed by rule where one exists: ICAO 9303 for Russian
  (with the national variants for Ukrainian, Bulgarian and Serbian), ELOT 743 for Greek, Hepburn
  as Japanese passports write it (no macrons, no apostrophe), pinyin without tone marks for
  Chinese, with ü as yu after l and n as Chinese passports write it (Unihan for family names, the
  English label for given names; Traditional Chinese uses pinyin too, not the Wade-Giles
  spellings common in Taiwan); elsewhere it is the name item's English label in plain Latin
  letters. Japanese given names on Wikidata are in hiragana, and so are Mihon's; their katakana
  reading is converted from the hiragana, a family name's from its Hepburn romanisation (Satō is
  サトウ). `ascii`, the form an e-mail address takes, folds the latin form to ASCII (é to e, ß to
  ss, ł to l). All of it is in `scripts/data/romanize.py`.
- **Cities.** The settlements of the country with the largest populations on Wikidata (an urban
  or metropolitan area, or a neighbourhood, quarter or district of a city, is not a city; Los
  Angeles, which Wikidata classes only as a charter city, is added by name; Thai municipality
  prefixes are removed, as are "Kreisfreie Stadt", "Ciudad de" and "Град"; Thai districts and
  provinces, Japanese metropolitan areas and regions, Taiwanese districts, Serbian municipalities,
  French and Brazilian metropolitan areas and Polish conurbations are left out, and so are the
  districts of Madrid, Barcelona, Hanoi, Ankara, Istanbul, Budapest, Belgrade and Seoul, two
  Vietnamese provinces that Wikidata classes as settlements, and Spanish places that are part of
  another city (an old town, a part of Benalmádena), by a list in `scripts/data/build.py`; of one
  city under two names (Palma, Palma de Mallorca) the more populous stays; a Vietnamese city
  that a 2025 reform dissolved as an administrative unit still counts, and a place Wikidata also
  gives Kosovo is left out of Serbia), named in the language (a Serbian Latin label in Cyrillic,
  letter for letter), weighted by population in
  thousands, with their first-level region where the address format names one.
- **Sizes.** Each language holds at least 1000 words, 100 given names per gender and 120 family
  names, in at most 80 KB of data, with one exception: Latin is written by hand by design, the
  503 words of Lorem Ipsum, 20 given names per gender and 30 family names. A test
  (`DataSpec`) holds every other language to these floors.

## Downloads

<!-- generated by scripts/data/docs.py from scripts/data/sources.lock.json -->

| File | Retrieved | Bytes | SHA-256 |
| --- | --- | --- | --- |
| [census/2010surnames-names.zip](https://www2.census.gov/topics/genealogy/2010surnames/names.zip) | 2026-10-24 | 12874389 | `117c41cb4668727b7627b2845b6df3f83eb2a22a1813f42c0ff4bdcab86de135` |
| [census/dist.female.first](https://www2.census.gov/topics/genealogy/1990surnames/dist.female.first) | 2026-10-24 | 149625 | `bd2f310fc4e5d5e5ea122c9d4342c9821145823118eb20db1647f305ec77b358` |
| [census/dist.male.first](https://www2.census.gov/topics/genealogy/1990surnames/dist.male.first) | 2026-10-24 | 42665 | `0a5078ef6effe3b483d15b0f7f95047662126c9bfb624ecd5e5b978fc0f2470b` |
| [cldr-47.0.0/cldr-core/LICENSE](https://cdn.jsdelivr.net/npm/cldr-core@47.0.0/LICENSE) | 2026-10-24 | 2033 | `b4c0ae8ef04f7059f96ce5bbe0467f9fe6f6d81bbe13517701dfeb961fb4d0b6` |
| [cldr-47.0.0/cldr-core/supplemental/likelySubtags.json](https://cdn.jsdelivr.net/npm/cldr-core@47.0.0/supplemental/likelySubtags.json) | 2026-10-24 | 218772 | `b0f791c37b7f1dedb57f6d0542b904ab0ceee7a476f5194c797f53e87511b9db` |
| [cldr-47.0.0/cldr-localenames-full/main/ar/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/ar/territories.json) | 2026-10-24 | 12282 | `fcefc7f4d7dc8f0d856d5ede1b8f71aa737e24454ef254206bc42f5ba20f6d79` |
| [cldr-47.0.0/cldr-localenames-full/main/bg/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/bg/territories.json) | 2026-10-24 | 12817 | `7a8b99d630664ec56a4a2f49ee4b11e2ad251d33b5d46034c0ca6cf8f4c87453` |
| [cldr-47.0.0/cldr-localenames-full/main/de/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/de/territories.json) | 2026-10-24 | 9868 | `7a588516781c3d235e7e0d4557f20c65e8c81bd1887f301283c616e1e26734ac` |
| [cldr-47.0.0/cldr-localenames-full/main/el/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/el/territories.json) | 2026-10-24 | 13108 | `9d5bb576a08da70cecbec83ea78d70931d64c81e6c492546621aed79a7adfe45` |
| [cldr-47.0.0/cldr-localenames-full/main/en/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/en/territories.json) | 2026-10-24 | 9956 | `158c1d575308f7e46912edbeda435c8fe2ef5dad280798231f3a432e406b1807` |
| [cldr-47.0.0/cldr-localenames-full/main/es/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/es/territories.json) | 2026-10-24 | 9868 | `3fc689e846a0d691972bcd8b94116423da420a1587a9984eefec8f626a44c26f` |
| [cldr-47.0.0/cldr-localenames-full/main/fr/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/fr/territories.json) | 2026-10-24 | 10130 | `1d3958a3bc424d7793956edb9cd4cc0ab59555b058798494e85e4be17ade454a` |
| [cldr-47.0.0/cldr-localenames-full/main/he/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/he/territories.json) | 2026-10-24 | 11928 | `90b157fa7f2846f032fd2c975c3ed90cf3bb68770a83e9d0727bc4c4207f4ac0` |
| [cldr-47.0.0/cldr-localenames-full/main/hi/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/hi/territories.json) | 2026-10-24 | 15945 | `8b1418383f309bca5a47f3f61e208717253309db37e17e543b3e563459efc1d7` |
| [cldr-47.0.0/cldr-localenames-full/main/hu/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/hu/territories.json) | 2026-10-24 | 10121 | `2315359c90f44c5de51d5466bf70b4b4914924cb4bdbef80d8650f9da071184c` |
| [cldr-47.0.0/cldr-localenames-full/main/it/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/it/territories.json) | 2026-10-24 | 9849 | `aa8d5a077c25f2fab1c6fbb21c14db8686d18a3480cd203c3dc56ed7c315d652` |
| [cldr-47.0.0/cldr-localenames-full/main/ja/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/ja/territories.json) | 2026-10-24 | 11751 | `042093cbb41ac1c877d3d0302800a3618075c77baf0a8810116096b62ed47da8` |
| [cldr-47.0.0/cldr-localenames-full/main/ko/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/ko/territories.json) | 2026-10-24 | 10611 | `052055b3fa8022983ce123d63c30153f1edffba78b9ee24d5fb0f83455a46439` |
| [cldr-47.0.0/cldr-localenames-full/main/nl/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/nl/territories.json) | 2026-10-24 | 9943 | `51f5d8ffc56cee8b610b2857eb491d151ad2866da49eccbd7b9ff0d6461f9b3c` |
| [cldr-47.0.0/cldr-localenames-full/main/pl/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/pl/territories.json) | 2026-10-24 | 9931 | `8a845b068d2545cddfd5b50fa1e982cd1d64a6ae41aabd6b48b704cf784089bb` |
| [cldr-47.0.0/cldr-localenames-full/main/pt/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/pt/territories.json) | 2026-10-24 | 9984 | `00ed17b43b0e82c3fa8e241fff0a0e961d7d625160f635a6bf7d58a7e52ca549` |
| [cldr-47.0.0/cldr-localenames-full/main/ru/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/ru/territories.json) | 2026-10-24 | 12730 | `6aa453161f1662df920a01a7bbee563e3e43be6229956739247af06d8bd60421` |
| [cldr-47.0.0/cldr-localenames-full/main/sr/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/sr/territories.json) | 2026-10-24 | 12910 | `ee4d8bb45d86f4aad482bc842aebfc833081a1c87a5e3bb5e196d0c05780806a` |
| [cldr-47.0.0/cldr-localenames-full/main/th/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/th/territories.json) | 2026-10-24 | 16270 | `640db35182b55b57efe5b53446f266bb1d7ff5b06571221cb263d0fb550de8cc` |
| [cldr-47.0.0/cldr-localenames-full/main/tr/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/tr/territories.json) | 2026-10-24 | 9853 | `0f3d474d0d1b94d956b4a2d6dd749358317383ef462e39c1ee0c084f47909d0d` |
| [cldr-47.0.0/cldr-localenames-full/main/uk/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/uk/territories.json) | 2026-10-24 | 13224 | `0ced254cb21895aef0050ee36d0b7797bbe944c8c46d401174cf68ff3de4a9fa` |
| [cldr-47.0.0/cldr-localenames-full/main/vi/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/vi/territories.json) | 2026-10-24 | 10062 | `26444cc1ce7ea0d8a088a5017172ff17b8e0d241b8701cf228d142a1c1a3c8d0` |
| [cldr-47.0.0/cldr-localenames-full/main/zh-Hant/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/zh-Hant/territories.json) | 2026-10-24 | 9846 | `9623d11205c24fa5053e060ed6b86020bed9c79b8a62ea2ac0df1467294f2fdc` |
| [cldr-47.0.0/cldr-localenames-full/main/zh/territories.json](https://cdn.jsdelivr.net/npm/cldr-localenames-full@47.0.0/main/zh/territories.json) | 2026-10-24 | 9873 | `539c540399611f96c574ee3f19c9d6abec7b8ff71c6cb651e4e1c1cd3ed3bed8` |
| [cldr-47.0.0/cldr-misc-full/main/ar/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/ar/delimiters.json) | 2026-10-24 | 267 | `3d602c24023aa1bd998335ebe3f497390a17093b74791ae6c797a974813d6de2` |
| [cldr-47.0.0/cldr-misc-full/main/bg/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/bg/delimiters.json) | 2026-10-24 | 267 | `f37a1033935efbb023767f3aa2dbe0565d00241431983babbe0d54fde2839ae8` |
| [cldr-47.0.0/cldr-misc-full/main/de/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/de/delimiters.json) | 2026-10-24 | 267 | `8885bcb7f04cc611a0904583a708396ecfc273cfaf69935441b1124f9cdc9557` |
| [cldr-47.0.0/cldr-misc-full/main/el/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/el/delimiters.json) | 2026-10-24 | 265 | `24cf80d4638d234949f9eb2830905691fc61e58c1b68e929f30fbef77a7888bd` |
| [cldr-47.0.0/cldr-misc-full/main/en/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/en/delimiters.json) | 2026-10-24 | 267 | `7dba09b7aba3f8c98292df1e6fee27cdc950d3542139472799ca350d877909bb` |
| [cldr-47.0.0/cldr-misc-full/main/es/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/es/delimiters.json) | 2026-10-24 | 267 | `72a69bb3fcaf42c08fd937c34a943b2654d522a3a11e350dec6e3c003e388ea9` |
| [cldr-47.0.0/cldr-misc-full/main/fr/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/fr/delimiters.json) | 2026-10-24 | 263 | `f77ccca4fe6749dcd5801f8dce618beea0318c4cfbff7934287e3d968d96c095` |
| [cldr-47.0.0/cldr-misc-full/main/he/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/he/delimiters.json) | 2026-10-24 | 267 | `52a6099efc608e956574551aff671f1ef03004ac0a55482eca1a69a6016c8888` |
| [cldr-47.0.0/cldr-misc-full/main/hi/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/hi/delimiters.json) | 2026-10-24 | 267 | `6036e1d19f25bbef2683d3135d1ce89cf5a226d85f40bae62c68aee950d1c15e` |
| [cldr-47.0.0/cldr-misc-full/main/hu/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/hu/delimiters.json) | 2026-10-24 | 265 | `1ec4b8d7ea4952da39ad30c6de0302d05a68614d925c82433e91f59927e10ece` |
| [cldr-47.0.0/cldr-misc-full/main/it/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/it/delimiters.json) | 2026-10-24 | 265 | `d016f3b3aa7cf9740195b32eec0192d1fb13a04185284e1faddf45705d242d8c` |
| [cldr-47.0.0/cldr-misc-full/main/ja/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/ja/delimiters.json) | 2026-10-24 | 267 | `b5381ed238e92f3bc63f94a24b12ad192a297dc58e15afd8cf17da5b239e0a6b` |
| [cldr-47.0.0/cldr-misc-full/main/ko/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/ko/delimiters.json) | 2026-10-24 | 267 | `4257ab3c82bb486a5c20182fbe2f7e8db575e6c1f3a18d06495670820210ce88` |
| [cldr-47.0.0/cldr-misc-full/main/nl/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/nl/delimiters.json) | 2026-10-24 | 267 | `17471b5aedd3812c47c066569bad5d850602a162538c7837e832451b499b94ef` |
| [cldr-47.0.0/cldr-misc-full/main/pl/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/pl/delimiters.json) | 2026-10-24 | 265 | `66361f409d3daa467fd243c1becdde63e061c16dfc6409208e7ae4520f3f254a` |
| [cldr-47.0.0/cldr-misc-full/main/pt/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/pt/delimiters.json) | 2026-10-24 | 267 | `9b00615b6df751390908e2cf64f8bd42b59f3adc005a217774da7f8199c84c96` |
| [cldr-47.0.0/cldr-misc-full/main/ru/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/ru/delimiters.json) | 2026-10-24 | 265 | `3cea69c5a49b41db4e30f6f09adb3c95648afaad2124f69d697fee43af9d0475` |
| [cldr-47.0.0/cldr-misc-full/main/sr/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/sr/delimiters.json) | 2026-10-24 | 267 | `8470817dd5544daa06c5f030cfb31dd9842b52fc44825844153f9d6e2b883266` |
| [cldr-47.0.0/cldr-misc-full/main/th/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/th/delimiters.json) | 2026-10-24 | 267 | `0023ed0aa9d41c0466ebaf0cc4267c11ae8912f274143d6e7c6489ef048b32fe` |
| [cldr-47.0.0/cldr-misc-full/main/tr/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/tr/delimiters.json) | 2026-10-24 | 267 | `468d8c8b30fd987d857bdba0bcda2ca30b3a17e4f60dde75af480376fa159d64` |
| [cldr-47.0.0/cldr-misc-full/main/uk/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/uk/delimiters.json) | 2026-10-24 | 265 | `9779da602c2fa68078607fe2a2d13f4b792f12cc0dd9dc32d09c3ccfe803c656` |
| [cldr-47.0.0/cldr-misc-full/main/vi/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/vi/delimiters.json) | 2026-10-24 | 267 | `2c74ef507b4874108962d74eae5e386f84b6895f4aa7bf48830f2ce361f2f05d` |
| [cldr-47.0.0/cldr-misc-full/main/zh-Hant/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/zh-Hant/delimiters.json) | 2026-10-24 | 298 | `08aa133e037b7e7fc6ddb7f3607e8481ccd8b20de041c47e79eafe2dbe6540c9` |
| [cldr-47.0.0/cldr-misc-full/main/zh/delimiters.json](https://cdn.jsdelivr.net/npm/cldr-misc-full@47.0.0/main/zh/delimiters.json) | 2026-10-24 | 267 | `70d8c955b62e4c94f36aafd9d015a0154fad3a11bf7fc4e82b241ecb8876cb16` |
| [icu4j-77.1.jar](https://repo1.maven.org/maven2/com/ibm/icu/icu4j/77.1/icu4j-77.1.jar) | 2026-10-24 | 14663227 | `b3640b9f416a4411fd33c59abbeea8fd57d024c23e1819bf9673220a97499fe3` |
| [ldnoobw-5faf2ba42d7b/ar](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/ar) | 2026-10-24 | 331 | `23dbd9c55257bf2a4965f46664dc7eac04b61ff4eb3571e41f7e964c7c3ed96b` |
| [ldnoobw-5faf2ba42d7b/de](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/de) | 2026-10-24 | 534 | `ebe44d7772897390e27f7bec412fdcdf0efc294e4db157f8487c4cd8d4fabd55` |
| [ldnoobw-5faf2ba42d7b/en](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/en) | 2026-10-24 | 3777 | `af851ecef1d5f212caba17339b12ac39cc2fef7d78c74876f67237644fcee8bd` |
| [ldnoobw-5faf2ba42d7b/es](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/es) | 2026-10-24 | 617 | `073334261cc2e7c08339faefd2f19f9623e8240a0ac18fe2a26c368b897496ce` |
| [ldnoobw-5faf2ba42d7b/fr](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/fr) | 2026-10-24 | 793 | `79763a705e08256b393952852bb63aa903f4485e0ef02f70735f5e50efff213c` |
| [ldnoobw-5faf2ba42d7b/hi](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/hi) | 2026-10-24 | 1123 | `83636e9b7a5260451e09e9e471f81dad9f082d107a82da77047e57d7856ed0cc` |
| [ldnoobw-5faf2ba42d7b/hu](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/hu) | 2026-10-24 | 780 | `a6943a971496f16e9a9d59ba6fde146d229699600e9636828d8d34ed9160aff5` |
| [ldnoobw-5faf2ba42d7b/it](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/it) | 2026-10-24 | 1451 | `d413f41018d58aa79d2c4809d0161f29ffd5652c4dc34c5b7b9ade680ee5599a` |
| [ldnoobw-5faf2ba42d7b/ja](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/ja) | 2026-10-24 | 2339 | `f83c4d0843ea6474c17bf171edd72b616b3af403f8613f2cde1bd841faf6f316` |
| [ldnoobw-5faf2ba42d7b/ko](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/ko) | 2026-10-24 | 588 | `0dd4b74ffea6e3bb35d658e6d8262b53ee99281eaa48ecb515a2065bfcc7f69d` |
| [ldnoobw-5faf2ba42d7b/nl](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/nl) | 2026-10-24 | 1909 | `8298ba0cdc3ca9a1989c349b8ad0148e4504c2804f2f2fb6c2f52bfb39c5ae71` |
| [ldnoobw-5faf2ba42d7b/pl](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/pl) | 2026-10-24 | 483 | `98cf824e3692255645c5a72d5913f26fd4f20930f725683b4c96bb312ca5d9b3` |
| [ldnoobw-5faf2ba42d7b/pt](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/pt) | 2026-10-24 | 629 | `05ef524b0bb7f83f87257f6ec4dabf70e16b8bb6930f769fb93deaf608ff196d` |
| [ldnoobw-5faf2ba42d7b/ru](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/ru) | 2026-10-24 | 2223 | `2598adabd2b761733d72a68d8ee4fcc1d2c38f2f85b6ff489985c6ce1d93bf75` |
| [ldnoobw-5faf2ba42d7b/th](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/th) | 2026-10-24 | 493 | `835036d87a3a67738e388cd6de92e3c995c186a1427b4e830fe7b85483c257e7` |
| [ldnoobw-5faf2ba42d7b/tr](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/tr) | 2026-10-24 | 1509 | `6c7ae03baee0f3fdc22eed169030567669005dad9a1923e0b571c4bd4f2f2ab5` |
| [ldnoobw-5faf2ba42d7b/zh](https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/5faf2ba42d7b1c0977169ec3611df25a3c08eb13/zh) | 2026-10-24 | 2675 | `a1d9aa037c8b039ef3b40148b3364ce2ca62ce4a955b7082a16ad99f6cbd1bc0` |
| [tatoeba/ara_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/ara/ara_sentences.tsv.bz2) | 2026-10-24 | 769162 | `c8d9c09f2db910ee1fb05054f0badb7a38195ea952b39f9e4ce5b92ec8ec70bc` |
| [tatoeba/bul_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/bul/bul_sentences.tsv.bz2) | 2026-10-24 | 299656 | `f9e608e6639084b28424a2cc50ab54c7e330f1ad21eab6c5fc44a0a59389c9ef` |
| [tatoeba/cmn_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/cmn/cmn_sentences.tsv.bz2) | 2026-10-24 | 1259217 | `edf090f0e0a043792e379560ddb56816757baa3dc99541fdca999c83b98a47ee` |
| [tatoeba/deu_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/deu/deu_sentences.tsv.bz2) | 2026-10-24 | 12108183 | `ce1f925b5fa76535f970f0ff02dc35899a4dec6c2eefd526cf832b4c791625ea` |
| [tatoeba/ell_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/ell/ell_sentences.tsv.bz2) | 2026-10-24 | 524073 | `9f5e81ad034a68e81736b0caba621fae3391a40ab91f7087c7dbfbde68537ae4` |
| [tatoeba/eng_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/eng/eng_sentences.tsv.bz2) | 2026-10-24 | 24918676 | `f634eaaf26623c2a2e170be2dae54b18388479311ae8619d04889df4e5d021f3` |
| [tatoeba/fra_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/fra/fra_sentences.tsv.bz2) | 2026-10-24 | 9781712 | `3cbc43eab20185907b3ff4eae8aa60d20b9caf5133076c1c1c013d169cd87a99` |
| [tatoeba/heb_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/heb/heb_sentences.tsv.bz2) | 2026-10-24 | 2401757 | `529ecc874447d5a47e3d4d69af6d96ac38c624b7da532962e228ba1ef5f379e3` |
| [tatoeba/hin_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/hin/hin_sentences.tsv.bz2) | 2026-10-24 | 179395 | `7e45c13ea3ccb15ed68bbc655f41b3be5f387bd42d2613efda179b32c540621c` |
| [tatoeba/hun_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/hun/hun_sentences.tsv.bz2) | 2026-10-24 | 6000177 | `47f12baca1054362df65d8cc96376099ffe0a174a5859889c5d0186adfd49269` |
| [tatoeba/ita_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/ita/ita_sentences.tsv.bz2) | 2026-10-24 | 9596820 | `1cccd6614ee66899ebf6d22549a29a2b52531c68483e508c60decc7135a0c7cc` |
| [tatoeba/jpn_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/jpn/jpn_sentences.tsv.bz2) | 2026-10-24 | 3417887 | `25d727283d297f812b2c9ec36f3a6812a8a60ed9d6d5427978c4e020a83782c3` |
| [tatoeba/kor_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/kor/kor_sentences.tsv.bz2) | 2026-10-24 | 194349 | `63bae2eec70e19d53d3bc46e9b7f2de3231cf0834265788983c0a222eb8f0186` |
| [tatoeba/nld_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/nld/nld_sentences.tsv.bz2) | 2026-10-24 | 2501052 | `87431fcb868dd4d2ac2a691b3934ecda7e9cfbde7a22970419afb668252fa374` |
| [tatoeba/pol_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/pol/pol_sentences.tsv.bz2) | 2026-10-24 | 1737662 | `34a0d8105ced64ebc77dc82aec67f50434a6f484d725a5cb3ffd6755fa2b3751` |
| [tatoeba/por_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/por/por_sentences.tsv.bz2) | 2026-10-24 | 6398543 | `ddb8dc071797c5e52d97e7b3f0d607290881ab2bfae4004a7c37007811f1c234` |
| [tatoeba/rus_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/rus/rus_sentences.tsv.bz2) | 2026-10-24 | 15570556 | `4cc471dbea673819ffada4735a9eadb4cd360074f9feee2fd6481bc5c7081760` |
| [tatoeba/spa_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/spa/spa_sentences.tsv.bz2) | 2026-10-24 | 6370284 | `29efd66854f11ad3480720ceda6e7b42563d40df4e660d9b0721c56a61753e9a` |
| [tatoeba/srp_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/srp/srp_sentences.tsv.bz2) | 2026-10-24 | 628108 | `0b5f4ea46bb1cbb9d2bf3ae87a22246c6bb8953607ca869b3960c01643af48f3` |
| [tatoeba/tha_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/tha/tha_sentences.tsv.bz2) | 2026-10-24 | 72082 | `d03a70b0189cf410e3b6ce4d6e0daa4062a01518d71ed1efd2fbed619669e5a4` |
| [tatoeba/tur_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/tur/tur_sentences.tsv.bz2) | 2026-10-24 | 8585287 | `364ad61c0a4739fd58a6f3e18ea2355ceee6a5b510a135fa27cb3e8ef3dca32f` |
| [tatoeba/ukr_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/ukr/ukr_sentences.tsv.bz2) | 2026-10-24 | 1996218 | `fcb9454e62978b2636d2465cbba4d383a76e6cf12030d8c675fc4ba420abf68c` |
| [tatoeba/vie_sentences.tsv.bz2](https://downloads.tatoeba.org/exports/per_language/vie/vie_sentences.tsv.bz2) | 2026-10-24 | 422555 | `3533911c9a693e12dfd2a77be695a847a631dac46f01cb3a27017b20bdc5ea66` |
| [unihan-16.0.0/Unihan.zip](https://www.unicode.org/Public/16.0.0/ucd/Unihan.zip) | 2026-10-24 | 8382485 | `b8f000df69de7828d21326a2ffea462b04bc7560022989f7cc704f10521ef3e0` |
| wikidata/cities-BG-100000-665ec17aeaa9.json (Wikidata Query Service) | 2026-10-24 | 1655 | `1c9a9e7fa96dbb730edf8df23090550e4f59b775982c0df8a85b09bdd13cd7d3` |
| wikidata/cities-BG-20000-130c9c5f7b47.json (Wikidata Query Service) | 2026-10-24 | 9224 | `7e31ecf857ff2006c3c6dc83908a6969d3eb96788bfee3d979cd58b5eabe35cb` |
| wikidata/cities-BG-5000-efbc29b2287e.json (Wikidata Query Service) | 2026-10-24 | 32141 | `61b209dc1caedbfe6322f78f6380832ed48611cfc172aa441dcd1947ff89aa81` |
| wikidata/cities-BR-100000-530e0f7500b1.json (Wikidata Query Service) | 2026-10-24 | 79819 | `56ddb1bfd3de16978cc00ad644feca2551b598c2f82cb35aac235d686615f236` |
| wikidata/cities-CN-100000-7f8d78abb95d.json (Wikidata Query Service) | 2026-10-24 | 105174 | `151896c770842a051f3aeb977db714e59bb56649c7b2f4c342b3b915cac4673f` |
| wikidata/cities-DE-100000-0324275d73fa.json (Wikidata Query Service) | 2026-10-24 | 22214 | `7b1f627d3fcfd3857672b260c9df253f08941b95ce0016d61f0c64859296e2e8` |
| wikidata/cities-DE-20000-023ddce59a8d.json (Wikidata Query Service) | 2026-10-24 | 103955 | `76bd98d144418ea351dde2c5f21928d52cd287c0a9f4511e977372e5eeea3b7b` |
| wikidata/cities-EG-100000-bacafd9f9b54.json (Wikidata Query Service) | 2026-10-24 | 18714 | `4c3dc0c4f7ea2f923c3b0f161b7dce1cb42a40594afdce4b2d1fda02496d7874` |
| wikidata/cities-EG-20000-0606da3cd7c7.json (Wikidata Query Service) | 2026-10-24 | 41765 | `c1b35d842f868af368c5b640308a4af93c0987b15ca6abc34af9717e00cb23d9` |
| wikidata/cities-EG-5000-442c5ed17675.json (Wikidata Query Service) | 2026-10-24 | 58007 | `a4d78017f0e29d7c611503280f0d5d8d09d3cd28d6c17e1fc4889b6e36728989` |
| wikidata/cities-ES-100000-301b4da2639b.json (Wikidata Query Service) | 2026-10-24 | 17744 | `8f38f0613ea1a3cda73379c7e2e8c2bb768d313ec62f71b9b07591d380576047` |
| wikidata/cities-ES-20000-f4f7c5d459c0.json (Wikidata Query Service) | 2026-10-24 | 83427 | `2504738b95cf5d00aa08c5ff15fa1431257a05def05cf7248fb9704a8998c7e8` |
| wikidata/cities-FR-100000-81c1d12eb591.json (Wikidata Query Service) | 2026-10-24 | 10793 | `04365a1e9cdae8a818e7d5b34bedf575d7c869893fe801ba2ee19f5b72f78353` |
| wikidata/cities-FR-20000-4220080714ec.json (Wikidata Query Service) | 2026-10-24 | 23307 | `6c809b3f0d4332d5cdc9db2784ee2380ec77a7eeb6d46e3c0c56d79fb53255ab` |
| wikidata/cities-FR-5000-a37ef3a45ae9.json (Wikidata Query Service) | 2026-10-24 | 39978 | `369d335dc7312457e5925cccde8cc3929fd0ec638f4d1dd87016590130544825` |
| wikidata/cities-GR-100000-df4fee062ad3.json (Wikidata Query Service) | 2026-10-24 | 2183 | `b52c6d3095ee662fd01bf3725b4c37a2932c077cd035ec91199ae20e5a10cc8d` |
| wikidata/cities-GR-20000-622259c9430e.json (Wikidata Query Service) | 2026-10-24 | 26484 | `3b634ac04d216298c4c81d18f4d05ae143f95b50f877938fae60624091aa5d4a` |
| wikidata/cities-GR-5000-78125a678356.json (Wikidata Query Service) | 2026-10-24 | 68523 | `300aacdd48bba6f47445184f27b81225d68c8618eed19077215bd3e024ad7c27` |
| wikidata/cities-HU-100000-1acdb8b06fbf.json (Wikidata Query Service) | 2026-10-24 | 2184 | `8ae730262b66e12eab2dfcca7262441299a50bfaef57571adb939ad75c96c422` |
| wikidata/cities-HU-20000-5074eb7f2145.json (Wikidata Query Service) | 2026-10-24 | 16014 | `a1be6281832ffdb65b5226b647025e705640a1728ccce5e8de3f972a222be16a` |
| wikidata/cities-HU-5000-66d7aad7b509.json (Wikidata Query Service) | 2026-10-24 | 70455 | `d3a41b8e81ff21e4cb19e4af59f2acbfe2ba1035eb54a363bc99446c1cecdedd` |
| wikidata/cities-IL-100000-e584fc2c6588.json (Wikidata Query Service) | 2026-10-24 | 4279 | `d3fb7c69aca8915a81effdcedaea407f2c00817aa02d7107fa5b0b43ce312933` |
| wikidata/cities-IL-20000-ffbc75f0fb0b.json (Wikidata Query Service) | 2026-10-24 | 23085 | `17aa62d7e9d513534caaa3b5b724ae8f745be34b1f43d0a60e8cb85e4edbbfea` |
| wikidata/cities-IL-5000-2eaf9c19863c.json (Wikidata Query Service) | 2026-10-24 | 46085 | `57a7ee71d88bd8b50bb5a0b8dbf74258e9647dc452d64c630e72a363d76f29a6` |
| wikidata/cities-IN-100000-8bf51776b61b.json (Wikidata Query Service) | 2026-10-24 | 84827 | `8f2a687a3776374af2330d334960ed53ea0b44ccaceeadcefb80de16108f31ed` |
| wikidata/cities-IT-100000-e4c480b9a9dd.json (Wikidata Query Service) | 2026-10-24 | 10240 | `e8c13815192e24352c020bec971ec81abe541aaca6534ca63ce5c043203a9af6` |
| wikidata/cities-IT-20000-367b8074df86.json (Wikidata Query Service) | 2026-10-24 | 24280 | `cab8c84d509b19fa8fb09c8d4746f3e51451972f3f8e28021b48b19773cabf14` |
| wikidata/cities-IT-5000-870fcfdb8d92.json (Wikidata Query Service) | 2026-10-24 | 36524 | `39d09b81313125758eb354d93a1108fce19412279e2be44f4e60398551c9d122` |
| wikidata/cities-JP-100000-8b95b80c3dad.json (Wikidata Query Service) | 2026-10-24 | 74022 | `59be0a7e252ca05bba2650fa63446fab00cfb588cb5e97f6d31d030b1cd2c149` |
| wikidata/cities-KR-100000-31191716f4d1.json (Wikidata Query Service) | 2026-10-24 | 19157 | `986b54fda8e8714af96d3876d9edbcf3d4782fecb1dd54b1b940a0ce8c9bfc50` |
| wikidata/cities-KR-20000-5703bde40fa7.json (Wikidata Query Service) | 2026-10-24 | 24113 | `ed6c743343e6739b99a7189b6f3ceb8a354fd0cd8e8e12e55715b57110d53fe2` |
| wikidata/cities-KR-5000-468c0975da93.json (Wikidata Query Service) | 2026-10-24 | 27261 | `e0b9dc131251e36827d0c27d9062f26734d97f4dbcdfbf18bf396d39f77af615` |
| wikidata/cities-NL-100000-e6217f5d649e.json (Wikidata Query Service) | 2026-10-24 | 5828 | `2c7ccd6605cd2970e8e9de07a09e979d94b63ae121377dd5c231d051c1e7c0ee` |
| wikidata/cities-NL-20000-0e1029ce8594.json (Wikidata Query Service) | 2026-10-24 | 32156 | `026967677372e380fbe8ed6861009cd914fd3a9d334bc5eb2c317036b3d5ce51` |
| wikidata/cities-NL-5000-b1233e08b946.json (Wikidata Query Service) | 2026-10-24 | 87457 | `8ab130463e7aa2c4a74952426e7f7dcab6cfdba91643661201581b065d6097ad` |
| wikidata/cities-PL-100000-5dfed7d47873.json (Wikidata Query Service) | 2026-10-24 | 10015 | `6471d77d7a5572df4b89b77efcbe384cf9bff2787f2c44a87edb4274169d8451` |
| wikidata/cities-PL-20000-d260a4b6d677.json (Wikidata Query Service) | 2026-10-24 | 56706 | `69b179ec7c7e047a245e5c661a717f3fcb2aa743a9721157f43210712aef908e` |
| wikidata/cities-RS-100000-57c5e0d6192f.json (Wikidata Query Service) | 2026-10-24 | 2979 | `3a334e56e5264df174f8a2d863e0a0ba8ed2141df24db7bc25bd570b4d407e2f` |
| wikidata/cities-RS-20000-1b6d5c90a918.json (Wikidata Query Service) | 2026-10-24 | 15520 | `2c8ba420e16764113e5249271e931dddbde31b1d1783a0e42329c2d065784ad3` |
| wikidata/cities-RS-5000-d222b589498d.json (Wikidata Query Service) | 2026-10-24 | 28034 | `3e82d72a6b273a41193d5fb1024044137ce1f2d52732d8c0ec2cc76d0645a0a1` |
| wikidata/cities-RU-100000-de9f6ebddd09.json (Wikidata Query Service) | 2026-10-24 | 47630 | `d4d8209c486db11b1f2e0328234c93cf4877b05979698239e57c9a7b316503f4` |
| wikidata/cities-RU-20000-e32f03889713.json (Wikidata Query Service) | 2026-10-24 | 104609 | `b6243c87bece29384eecd794366b69c3aeca60f5bd678ea116bc0a1e40cc7824` |
| wikidata/cities-TH-100000-949324704591.json (Wikidata Query Service) | 2026-10-24 | 11119 | `d72997ab11a3d96c42dae9b63adeeca39d31bdb92c69f9b0497f73216329dde8` |
| wikidata/cities-TH-20000-c9b1a7cf2094.json (Wikidata Query Service) | 2026-10-24 | 52555 | `bdf44178a81cf437e0341bd8a1c17006253b59de5704afd0a4fdc5e567dc5203` |
| wikidata/cities-TH-5000-2acb4259fcc5.json (Wikidata Query Service) | 2026-10-24 | 79867 | `b535d56b6ede49abedcfb0207a520a52022dd4bd776b1d8d1873b1d3e070bddc` |
| wikidata/cities-TR-100000-14712e29cb42.json (Wikidata Query Service) | 2026-10-24 | 55199 | `e3f42e68858fc75313ebe9b04e54d235f7b60fa900f1823a035b99bd3c68fe58` |
| wikidata/cities-TW-100000-af1a7de3842f.json (Wikidata Query Service) | 2026-10-24 | 20529 | `26a9276ef184d6ce07cee9d01e9e300f205df65cd5963f85b0d552e9a7849df2` |
| wikidata/cities-TW-20000-34fda2389fd0.json (Wikidata Query Service) | 2026-10-24 | 68062 | `99c7c44594951196a6ba098ef52b2157b84f5283194dad544d7c7f511a5fe1c3` |
| wikidata/cities-UA-100000-ac81501c3803.json (Wikidata Query Service) | 2026-10-24 | 11866 | `973ce28f534cd35e0dd37ecb55f1f7e3d23da2ddb470a9c317757d7246357d52` |
| wikidata/cities-UA-20000-75e19afb4cb5.json (Wikidata Query Service) | 2026-10-24 | 52620 | `a74334d76b706d3177bdf4a07b36c29f91a2ac34b3390396bdf7e6f2793d1aa8` |
| wikidata/cities-US-100000-5dc2183a6c63.json (Wikidata Query Service) | 2026-10-24 | 92466 | `8f1b00feb15e705b8e4d902889993213c1fcb3a072b20ce339db47b6f8ed3c91` |
| wikidata/cities-VN-100000-c9a83edd0887.json (Wikidata Query Service) | 2026-10-24 | 1664 | `becb72ff4b0082209bfaf461aaa1eff61bf694910f7c30b85bac14dc49585238` |
| wikidata/cities-VN-100000-f3fb6ad21831.json (Wikidata Query Service) | 2026-10-24 | 8207 | `d8b58eb50def5d2c40a33d5ad99c8003caaef7aefcb91b5abf994f8e52928e58` |
| wikidata/cities-VN-20000-4dded645815c.json (Wikidata Query Service) | 2026-10-24 | 1664 | `becb72ff4b0082209bfaf461aaa1eff61bf694910f7c30b85bac14dc49585238` |
| wikidata/cities-VN-20000-b8bba13051ef.json (Wikidata Query Service) | 2026-10-24 | 8468 | `59b03f08cd6365bf4c823ae949284f26c2df70d88ab467d2fc81eb2cebed7165` |
| wikidata/cities-VN-5000-3e41dc7a5c95.json (Wikidata Query Service) | 2026-10-24 | 8468 | `59b03f08cd6365bf4c823ae949284f26c2df70d88ab467d2fc81eb2cebed7165` |
| wikidata/cities-VN-5000-583785f8f7d5.json (Wikidata Query Service) | 2026-10-24 | 1664 | `becb72ff4b0082209bfaf461aaa1eff61bf694910f7c30b85bac14dc49585238` |
| wikidata/cities-extra-US-4cf89280a2e7.json (Wikidata Query Service) | 2026-10-24 | 351 | `c3d86b9969fc7d4f754a484d1e56e975deea5b5d4d5a1b5529dca34bdaf7e7f0` |
| wikidata/cities-shared-RS-250289b2a621.json (Wikidata Query Service) | 2026-10-24 | 4084 | `7eba1a0638c0cb9960598e82e1f348121821af8ca0196e4cf176a1cb541dbba5` |
| wikidata/city-labels-BG-bg-de5f75484ac5.json (Wikidata Query Service) | 2026-10-24 | 29007 | `c870e07297a8b56f6146cd272fb2229bcf8d91992a0065cef58f1a236c213f4b` |
| wikidata/city-labels-BR-pt-228b4e4349f3.json (Wikidata Query Service) | 2026-10-24 | 103673 | `db31bb50d2b819a1fced30a3b8422e5b671c2fc08624da99c7f5bc011fd81704` |
| wikidata/city-labels-BR-pt-50cf63a323e5.json (Wikidata Query Service) | 2026-10-24 | 103673 | `0692a5ce77eae7bc00e54f3b0ded47e03dc77171b86bb5325bfb93b736c0e3e3` |
| wikidata/city-labels-BR-pt-8c920a078c80.json (Wikidata Query Service) | 2026-10-24 | 103673 | `6f74e623eb32b82750bf6e6ca8d38ea658a9b9043a62738b3c727e530f976437` |
| wikidata/city-labels-CN-zh-hans-553ed5985d1e.json (Wikidata Query Service) | 2026-10-24 | 93577 | `675ba85b3ddd01b2d95b9fcea76058076d98d27b81ad7d1f3ec5cafbe0a7f700` |
| wikidata/city-labels-DE-de-f19e9ae94504.json (Wikidata Query Service) | 2026-10-24 | 91377 | `a4072d376b6de5c506825a98a585d254cab50a96118a23e64c598ca505212a3c` |
| wikidata/city-labels-EG-ar-1b0e05d24f40.json (Wikidata Query Service) | 2026-10-24 | 37265 | `cdd63e467ea6ee72697f4779fc31f9935c04665d15bc650ed4edefff6ee75032` |
| wikidata/city-labels-EG-ar-2930630c62c8.json (Wikidata Query Service) | 2026-10-24 | 52079 | `097b43e68e5474d94f44cff7d9a36bbedb589e9bb712990daf3e2afaf10f47b3` |
| wikidata/city-labels-ES-es-fbb62a2467df.json (Wikidata Query Service) | 2026-10-24 | 70133 | `a7a1d2f2d037a77f23cc1f456d483c0e4928c260becf661950540786d5ee7081` |
| wikidata/city-labels-FR-fr-349adbfdd210.json (Wikidata Query Service) | 2026-10-24 | 35435 | `3b8a946dc4297dabac586867e096833edf98952193a072ad4399c2409552e469` |
| wikidata/city-labels-GR-el-5a2dcb3e79aa.json (Wikidata Query Service) | 2026-10-24 | 63467 | `130d68271147f7966f6c2e970db027f669ef59e5ed75e59377c5bcf58712fe3b` |
| wikidata/city-labels-HU-hu-7dd16ae2a271.json (Wikidata Query Service) | 2026-10-24 | 62101 | `134ef94e402f86c6c64c7bf53bfdb1e164b7f7e3c3c29530b4ab9f0d41309400` |
| wikidata/city-labels-IL-he-443dd6e9e9aa.json (Wikidata Query Service) | 2026-10-24 | 41407 | `5bdee80122e979a7ca0d845f835695ae601a4341d91253b800eba14ae26b1cf9` |
| wikidata/city-labels-IN-hi-1433c2392afb.json (Wikidata Query Service) | 2026-10-24 | 122462 | `4862efa49ed06c22423d43a56c009be723950e1323666762f663aaf5b76b954c` |
| wikidata/city-labels-IN-hi-a86a1c5dca98.json (Wikidata Query Service) | 2026-10-24 | 120948 | `e0817e0d8baaff87dac49d643de0ea57d36784d289cfe7f0ecd55b901c80dc34` |
| wikidata/city-labels-IN-hi-bdd97ff6da3a.json (Wikidata Query Service) | 2026-10-24 | 78106 | `d0cd3b1e443e4113c219cf08857078bfa7cdbf3cda98bcc98653f93d88ab06ed` |
| wikidata/city-labels-IT-it-801e41e1db04.json (Wikidata Query Service) | 2026-10-24 | 32008 | `2a98c6de96eb56bad2aefeb31c9e9313f8771415671e9102d22e748a6b5228d4` |
| wikidata/city-labels-JP-ja-6a2252f6c211.json (Wikidata Query Service) | 2026-10-24 | 65074 | `ad9cf10bbf6dc27e1892be7abddfeca83a1b21fe1e3c6eab4117a39977e82783` |
| wikidata/city-labels-KR-ko-23df5fd28079.json (Wikidata Query Service) | 2026-10-24 | 23945 | `620a0f9cb217935ccdba310bdeb9e260d17cfdd1ac19aca7b01364179a372795` |
| wikidata/city-labels-NL-nl-608153fabe07.json (Wikidata Query Service) | 2026-10-24 | 76408 | `5e46157d8632b47e6548cd0ab9b93efed40784ca427dbd139e25c4915a005efd` |
| wikidata/city-labels-PL-pl-7725dda728dd.json (Wikidata Query Service) | 2026-10-24 | 48447 | `40ecf9ff059e1359570f99398d6b64103157123f03c226e15120fd1310997a1c` |
| wikidata/city-labels-RS-sr-1d46a7be9daa.json (Wikidata Query Service) | 2026-10-24 | 16964 | `2aa9b5d5fb266d0d102e5f8c85dc53873394407456e287e483241ee8871b8bc0` |
| wikidata/city-labels-RS-sr-bc9975792691.json (Wikidata Query Service) | 2026-10-24 | 25359 | `d415278f04a974d776801e488d45f94f915b016820456419b622cdda43d14afa` |
| wikidata/city-labels-RU-ru-4197e32babab.json (Wikidata Query Service) | 2026-10-24 | 98227 | `b34f751fafed758c48659cec2ba6bf3ea6469123cb606321b6caa15027fc15aa` |
| wikidata/city-labels-RU-ru-dab211ae6cc5.json (Wikidata Query Service) | 2026-10-24 | 43610 | `af1513611c70261ce09429c17b5fd42acda38be521ae3e5605bceac550644fcd` |
| wikidata/city-labels-TH-th-25490ef40a0d.json (Wikidata Query Service) | 2026-10-24 | 82703 | `4eb06305b0ef2ca642cfe5a94330da01647fb0697c7df9773aa9e97ec61bbd9c` |
| wikidata/city-labels-TH-th-4cae14377bc5.json (Wikidata Query Service) | 2026-10-24 | 53980 | `2f2725d4235c6382ea12f43b43f3412e1f8e73b2fc9dde3407dc357736819a1f` |
| wikidata/city-labels-TR-tr-e8ace1856515.json (Wikidata Query Service) | 2026-10-24 | 47570 | `fb80753e7caec7331cabf0010eb072c7d2f7acbd233ad85c59b2d66fe8ff60d4` |
| wikidata/city-labels-TW-zh-hant-0495549add72.json (Wikidata Query Service) | 2026-10-24 | 60931 | `50501cfc7614e7f5b7bb2f6ba568e5fadb267cdcc5ba8a5e43dbc4700a5b35e3` |
| wikidata/city-labels-UA-uk-6869220462fe.json (Wikidata Query Service) | 2026-10-24 | 47869 | `c68adc574450fbf28c2a7a62ab130d425f58a9737a199f4891a7b60a6510c4af` |
| wikidata/city-labels-US-en-25f7489b3c6f.json (Wikidata Query Service) | 2026-10-24 | 119567 | `c34553cb046f4625592088630ddb267f5c3b34b83a3f691bbb9eb126b179e88e` |
| wikidata/city-labels-US-en-426fada9f9a0.json (Wikidata Query Service) | 2026-10-24 | 119567 | `2267d9677f1927486919c12587ca6229a42a0ae070f3c1cbcf129f7447043056` |
| wikidata/city-labels-US-en-612bee56effd.json (Wikidata Query Service) | 2026-10-24 | 119226 | `93d484df82180cf0dd916ca68d99e747f7f0ca23b8d9f165c93e53fbdf506a4c` |
| wikidata/city-labels-US-en-a9eb31ac0a9d.json (Wikidata Query Service) | 2026-10-24 | 119567 | `8af2d0d374a753988ae8e6c18aeb7d00c4a42a2cf6cb02e4078a9f9255ccff57` |
| wikidata/city-labels-VN-vi-85169fafd49c.json (Wikidata Query Service) | 2026-10-24 | 1481 | `f317cad6f020a5e1b1c6bc30453c4f9ca4a49ca6d0f8fcef118b721605fb8a8f` |
| wikidata/city-labels-VN-vi-c19a236b13ca.json (Wikidata Query Service) | 2026-10-24 | 7460 | `923e9b24413fd89d98652ca7ccac3be904001c5e827e716c286ecb2551dd3751` |
| wikidata/countries-la-c7530040f26b.json (Wikidata Query Service) | 2026-10-24 | 49396 | `7713e175339f68f3b240a8bc7b6a57884afaec221f5eb054cf3447fc79a9b32e` |
| wikidata/family-BG-4b759afa922b.json (Wikidata Query Service) | 2026-10-24 | 206092 | `b7d6f4c740a799cc617c247e59f8d9acecfc993637c4723c37c729436451966f` |
| wikidata/family-BG-aba469810502.json (Wikidata Query Service) | 2026-10-24 | 206054 | `52121facdff8af87ff75e484cbccc8f764598691be4ffc1c449a063723498366` |
| wikidata/family-BR-272ba75fb40b.json (Wikidata Query Service) | 2026-10-24 | 205977 | `8d8970869556d2a4a8582fd9220e081a9c4dd8cf4219ea9bb4c587c584049604` |
| wikidata/family-BR-9ffa5150eff0.json (Wikidata Query Service) | 2026-10-24 | 206099 | `88b070942627aed65841a4523e7463cd630c3d8b6d38b569a938270cab8daddb` |
| wikidata/family-CN-7b5e10dcd049.json (Wikidata Query Service) | 2026-10-24 | 205721 | `092cac0460c84bcec0d91a1bf7af97cc37f8bd86c8f3b05788ea5986f7557862` |
| wikidata/family-CN-d5284ee6a1f6.json (Wikidata Query Service) | 2026-10-24 | 205743 | `678bff1a0381a7d4ff53d6f6feca7cd44513630ee303617dabb710736b267dca` |
| wikidata/family-DE-59bdee8ca71a.json (Wikidata Query Service) | 2026-10-24 | 206251 | `20858d6a3299d13881f2e999e6af89e9293f2c14f37b61cdf33fbb31cb31bb4f` |
| wikidata/family-DE-7cf2fda9bcf8.json (Wikidata Query Service) | 2026-10-24 | 206325 | `241a19a3f469cddba9237ed3f9d7dc4c521f9f07de12c1a5823df6fa0df5f568` |
| wikidata/family-EG-0fd7b11e42f6.json (Wikidata Query Service) | 2026-10-24 | 205806 | `a0ccaf93f853702d5d790a57a5b236bd1f72ff56059449325b691a80473c1d07` |
| wikidata/family-EG-ff4b60381854.json (Wikidata Query Service) | 2026-10-24 | 205807 | `27afedca4b9c59241433eb3cf46c620c009785ea409870c3187627b49156ab0f` |
| wikidata/family-ES-2c10c261fe23.json (Wikidata Query Service) | 2026-10-24 | 206416 | `0ca86fda6cb8eff982091c350e42e82af58efc9eea40d76c15ba7aa7656190ea` |
| wikidata/family-ES-d8d3fc044bd5.json (Wikidata Query Service) | 2026-10-24 | 206470 | `4441730546ce2d760c412529e3256159df12633c1f98eb25a23253a4cf03b483` |
| wikidata/family-FR-02af825bb251.json (Wikidata Query Service) | 2026-10-24 | 206439 | `147096691372db6350f5ff273a34f48e57de3d0ee3a0a5fa0ca84e20f1e24581` |
| wikidata/family-FR-e56419339363.json (Wikidata Query Service) | 2026-10-24 | 206542 | `e3b9389bb961ad52dd5445db56a1e275144252b6f716b79d7fda2ff3838c6aa5` |
| wikidata/family-GR-1c946feaa971.json (Wikidata Query Service) | 2026-10-24 | 205827 | `39057518f9c49822c3b844b07b793dbb3ee64223ac59160714765ab6c433d4f9` |
| wikidata/family-GR-937e27e724ab.json (Wikidata Query Service) | 2026-10-24 | 205872 | `c2ba5b9079718f35c7cd04756cc9687d68af6a3aa31b6b4901be3a584b1e2b70` |
| wikidata/family-HU-7b42c228af6e.json (Wikidata Query Service) | 2026-10-24 | 206368 | `8e2e06ea2ee3f73b5a824b277dc85dd2579593b63c089c9e9a42b3ab5669e828` |
| wikidata/family-HU-89f99096ddf4.json (Wikidata Query Service) | 2026-10-24 | 206627 | `5ceb5b81dca7e830f10c17fe0858638c923ba8d019562e295b5420fe5d01f323` |
| wikidata/family-IL-a4cb9416b565.json (Wikidata Query Service) | 2026-10-24 | 205976 | `4602cb44fa58923c2b0632fc36b81b7fba2aba30c772ed9e17173ea909a14427` |
| wikidata/family-IL-cffa9b3c2f63.json (Wikidata Query Service) | 2026-10-24 | 205904 | `77de8751712fa87ef80ecc5e2a458fc33db19cd2eb2546f4376f7b9b5362d2ba` |
| wikidata/family-IN-4e40b7b6587c.json (Wikidata Query Service) | 2026-10-24 | 205731 | `3be4809f7bfc33c1dcb3d61d303d46d81c7e37e61483e23f042d0b7d7fb863db` |
| wikidata/family-IN-b8152dee77da.json (Wikidata Query Service) | 2026-10-24 | 205799 | `5fd91787a91f38897dfe2a3e1a4313be316387de19718ecc181aa8c568bb2972` |
| wikidata/family-IT-26198b0bac62.json (Wikidata Query Service) | 2026-10-24 | 206424 | `fc6d17f895cc2a83e624e6fd89641f1ca47bc380cd7632ce35ac06e43e61c227` |
| wikidata/family-IT-78194286b4ac.json (Wikidata Query Service) | 2026-10-24 | 206447 | `ccf6462127ffbf999978335b019308054ac092f885826eeff6b074bcf94dd5bc` |
| wikidata/family-JP-4a9c58ab192e.json (Wikidata Query Service) | 2026-10-24 | 206722 | `dd7dd894334b9978458a9a06a2543a6d0cbac86bf032f108183c68c414fca40d` |
| wikidata/family-JP-d30e8fb96448.json (Wikidata Query Service) | 2026-10-24 | 206654 | `c2533845a3d68d4376e7b29517a3ac685ab656bf9be8e94505f5c3a88b2c4198` |
| wikidata/family-KR-05a0cdcdf084.json (Wikidata Query Service) | 2026-10-24 | 127371 | `60a10c43ce40066b71ce72883c6c94dc496e019967484027787bcb743b2ea339` |
| wikidata/family-KR-28612b9b308d.json (Wikidata Query Service) | 2026-10-24 | 116028 | `f0a2a4b0b8980aefe8c911bc1c96276313597a1a3a140bcb7f24b2e0c15580af` |
| wikidata/family-NL-20a82e3acb35.json (Wikidata Query Service) | 2026-10-24 | 206071 | `bfe7bee71f69c796bb89fba8140bcfd92ba48677a393578a34e4a18033f03fcf` |
| wikidata/family-NL-6da167129d93.json (Wikidata Query Service) | 2026-10-24 | 187939 | `84f73e340e8a4d9715a888aeaf0e7936cf9a408b0ab88be533a295340e1e3284` |
| wikidata/family-PL-d4bb1a46b99f.json (Wikidata Query Service) | 2026-10-24 | 206441 | `9dd0d4dfbd0977e0ec01bc19294126e8b097d6d88dc839da97ba123b9f114177` |
| wikidata/family-PL-edd792da2035.json (Wikidata Query Service) | 2026-10-24 | 206407 | `fceaa48eb6a2f339a050dd79cf7cadf2b312c13ba82316a8c68d5c45adc865c2` |
| wikidata/family-Q101352-native-th-63dceffdd433.json (Wikidata Query Service) | 2026-10-24 | 27797 | `aa1d1521ce7673597177b44c26ba6705c89fc75faf503d1af5864d405907f6df` |
| wikidata/family-RS-461f0f9afb32.json (Wikidata Query Service) | 2026-10-24 | 205830 | `a858aa6910d7f6f507680bcf2f8a35e0fb680f13cfc9518e097eddeca8fce02e` |
| wikidata/family-RS-6b10cec0751d.json (Wikidata Query Service) | 2026-10-24 | 205844 | `b546de7fbba05af102176b37919e7ebc4dfbbc688bad6cf8e6650c29b175c046` |
| wikidata/family-RU-78067d505050.json (Wikidata Query Service) | 2026-10-24 | 206276 | `7449874f7e883e264d57fa5e1a239b0e2fb9ecccb29d1a80209e90bd298c00a4` |
| wikidata/family-RU-dc23de2a4a51.json (Wikidata Query Service) | 2026-10-24 | 206236 | `503b377a8bb515d950e4f469b795916082ea4a44a1a1cc73ab5a53497e069bfb` |
| wikidata/family-TH-1990f3e0e550.json (Wikidata Query Service) | 2026-10-24 | 206223 | `b9e4bac2a0d5fdc08abf30c631de2f06ff0e59bc98e0845435a72f43c0264e2d` |
| wikidata/family-TH-a9ece3b41392.json (Wikidata Query Service) | 2026-10-24 | 206167 | `a21202e8b6f999d6b6c3e243ba9c9efe07f3408e9f594677cd2c9741140a1c65` |
| wikidata/family-TR-4bf792f7af11.json (Wikidata Query Service) | 2026-10-24 | 206467 | `bc268d9a42ac1a437e8b2d8278cbc458dd8f1447b4771202daf43bc55e8badc3` |
| wikidata/family-TR-f8f2074addd6.json (Wikidata Query Service) | 2026-10-24 | 206172 | `1f8996e9e0e3fd417b62ac3fac0df88fd4c4cca0892646e2b15989367891a692` |
| wikidata/family-TW-85294bd5d8ce.json (Wikidata Query Service) | 2026-10-24 | 148373 | `0e66357d9171b922c51db1be3953756c35437a8915fd11b258bd3b058df9ba0e` |
| wikidata/family-TW-9c5c57d7efb4.json (Wikidata Query Service) | 2026-10-24 | 170493 | `ff84d7943dd7b997002fdead5efcd833462a43b923be133e923115fbe4905bca` |
| wikidata/family-UA-18a76e916af3.json (Wikidata Query Service) | 2026-10-24 | 206099 | `50903ba188f823596bd1fe76e5f01d61573bf8782dc36d9cc71fb2481c9b80db` |
| wikidata/family-UA-9f98553d34f9.json (Wikidata Query Service) | 2026-10-24 | 206078 | `3d2cedaf2a023f84dec9b753f0998ec2af310f484dba5e4d371209a922ec6b7c` |
| wikidata/family-VN-2b652a81df98.json (Wikidata Query Service) | 2026-10-24 | 72336 | `fb008330768bf47c2211c87080aa9c628dd7c9eb171906379e3369e01213aa80` |
| wikidata/family-VN-6a35dd4251e3.json (Wikidata Query Service) | 2026-10-24 | 64371 | `0aa95fb1e5cd814035d73829bb0dc1f6ed3ff5d7e2bddf988def41d5800bc6b6` |
| wikidata/family-labels-BG-bg-0-646a187cf4ee.json (Wikidata Query Service) | 2026-10-24 | 187053 | `72b813bcb47a07c735421b59e3836e25badd6538601183f7ebecd526d7dec8ae` |
| wikidata/family-labels-BG-bg-0-f2cfc97bef4d.json (Wikidata Query Service) | 2026-10-24 | 192642 | `f360b2231f1bfc52393bf03f8ba4b50dc0eaea1b5b7bdf59024995a29d1da382` |
| wikidata/family-labels-BG-bg-400-62ea9f75042c.json (Wikidata Query Service) | 2026-10-24 | 170640 | `9540b52d4ade871d155551942b7068468918e643f775df66d4f4b40cc7f81e5d` |
| wikidata/family-labels-BG-bg-400-72d7be1b2f87.json (Wikidata Query Service) | 2026-10-24 | 172666 | `816191373c9320788ff358cf487dda98c6fce014e8f048de7ead1401bceec9de` |
| wikidata/family-labels-BR-pt-0-7f035e7c65ea.json (Wikidata Query Service) | 2026-10-24 | 179093 | `8b321548383f6e62276f6495f0f378f28de3241489d6d99049c4a64db4d28538` |
| wikidata/family-labels-BR-pt-0-b9bb85f4fced.json (Wikidata Query Service) | 2026-10-24 | 178174 | `21f585681d118c98f134b238d699a6dcac7f930596d09fc7fe4e49c2e476e94c` |
| wikidata/family-labels-BR-pt-400-aa75eeb16417.json (Wikidata Query Service) | 2026-10-24 | 181639 | `b299599402f83744a8230bc589a7dec653aecc625e996a5afc32289ea44eb187` |
| wikidata/family-labels-BR-pt-400-ceefdf016fb6.json (Wikidata Query Service) | 2026-10-24 | 181840 | `50b64686ed8ce39daeffbdcedbe1f5aad2703a6ca396713895bde9cee46d44a4` |
| wikidata/family-labels-CN-zh-hans-0-5cb607f97daf.json (Wikidata Query Service) | 2026-10-24 | 116858 | `253a8c24455453c268772a0d25e2c37ffd012a4781d42c3774a7712170f6f48b` |
| wikidata/family-labels-CN-zh-hans-0-b35ab4c2bbd6.json (Wikidata Query Service) | 2026-10-24 | 124938 | `2cdb99f35cc4e37f4e907b129ce171627ad3140b1ca2e95a2adc352ca24edc30` |
| wikidata/family-labels-CN-zh-hans-0-bee1465850a7.json (Wikidata Query Service) | 2026-10-24 | 167534 | `9ea130dd19f422c7fb12b33df98758321a306fa7761e95fe05c6ccc5e8ea435b` |
| wikidata/family-labels-CN-zh-hans-0-f0f4f5a204da.json (Wikidata Query Service) | 2026-10-24 | 167729 | `0bb677b73cb93c42cb1c41346577a222ac277a4559994ecda2326d5604dab31b` |
| wikidata/family-labels-CN-zh-hans-400-01594beb6b32.json (Wikidata Query Service) | 2026-10-24 | 113949 | `a2214a9e3c36f608209fb08d774b4626ab1a4cd88bd4fa17814df69234004370` |
| wikidata/family-labels-CN-zh-hans-400-1bac068a9410.json (Wikidata Query Service) | 2026-10-24 | 121242 | `c8c1d96f02fd3d6a7695e47eb52dab1e1dda18f9a7736f67d3d70e1658c5354c` |
| wikidata/family-labels-CN-zh-hans-400-8af2a3f63f14.json (Wikidata Query Service) | 2026-10-24 | 164340 | `0a61bdab0fec405b026cc76545e499fbb7165036db29b984b5210dd359efc4e2` |
| wikidata/family-labels-CN-zh-hans-400-e55a0f167812.json (Wikidata Query Service) | 2026-10-24 | 163597 | `0413ec9c15ea45d6fe99dec6c7581b6fa5bdd7427cf61869060dc2a82aed96d2` |
| wikidata/family-labels-DE-de-0-4589cf8abcd1.json (Wikidata Query Service) | 2026-10-24 | 200105 | `93b87160fd55927747db28bc2cce0a1cb6667963ade2803a83c74adbb8735d73` |
| wikidata/family-labels-DE-de-0-a7a0c8ccfc54.json (Wikidata Query Service) | 2026-10-24 | 199667 | `6183a64d0500f58ed88bfc750d6d10da0868a1b4f3e3b5c7055128b62fe7c06a` |
| wikidata/family-labels-DE-de-0-e086120db78e.json (Wikidata Query Service) | 2026-10-24 | 157545 | `7df20b11edad521c822d6f6d22982f5c538d1d66d2a84a2f72c23af26a833ffa` |
| wikidata/family-labels-DE-de-400-54a61ef69a3b.json (Wikidata Query Service) | 2026-10-24 | 201236 | `0b2633b081b3321e02141bd28309566c75f1a0df3ca021f20b03c7a5a350abcd` |
| wikidata/family-labels-DE-de-400-ba97112c631f.json (Wikidata Query Service) | 2026-10-24 | 156251 | `e8a52f5cb838269c3ded3de54a228ccb6f11de754a45e5f1f3940767e995994c` |
| wikidata/family-labels-DE-de-400-cc3966e6c1e8.json (Wikidata Query Service) | 2026-10-24 | 200482 | `ac1c370346fe30afcf8b3e6d1fd271c37cea236213c0cb07d70ce3ebb1ffd4f8` |
| wikidata/family-labels-EG-ar-0-9ca1e73ee660.json (Wikidata Query Service) | 2026-10-24 | 118887 | `c83b760b775976ed42e72f205003dfd5595a1757ccbcfb1e6fd262f2e7e5d2be` |
| wikidata/family-labels-EG-ar-0-9cd044681955.json (Wikidata Query Service) | 2026-10-24 | 171972 | `2200f04be41258ce67636fe4f43f26045d17463401af800f79783d7122df8400` |
| wikidata/family-labels-EG-ar-0-a006cb5dac96.json (Wikidata Query Service) | 2026-10-24 | 174554 | `b0485b75df5fd4c1254e6891f170d7c884ea97d78c163319590acabaec1196e8` |
| wikidata/family-labels-EG-ar-0-fc360fbce5ff.json (Wikidata Query Service) | 2026-10-24 | 129493 | `6c856f2caba933d764da55b7e1c4a5cdf69121c39cac06269ce859f3ed126ce3` |
| wikidata/family-labels-EG-ar-400-01938e1c7618.json (Wikidata Query Service) | 2026-10-24 | 114442 | `63617db08db9111c12b8836f282295913ac1bc9d7795b875bdfcd3d64e4dcc30` |
| wikidata/family-labels-EG-ar-400-1e235d7ee769.json (Wikidata Query Service) | 2026-10-24 | 173278 | `dade3a40113ff372b93c49ddeac1db4c1ef98ade7db6e54d8bd877770ed97a8f` |
| wikidata/family-labels-EG-ar-400-d2b0a653df36.json (Wikidata Query Service) | 2026-10-24 | 174656 | `682bd9954feb4cf50f227b26f1d0ccffca3ed2c1468439b9c544b5655b385041` |
| wikidata/family-labels-EG-ar-400-d2c338834749.json (Wikidata Query Service) | 2026-10-24 | 125572 | `ec379731f28cb5ec0f8692b0a6dcaaf58557bdf91cbcc15998d18cc77baa237d` |
| wikidata/family-labels-ES-es-0-1f3acf67bf55.json (Wikidata Query Service) | 2026-10-24 | 186675 | `431543ba9762704da2e2f14d886f2c2bfcb255bfe06e2120fd45b02e34e6c6a5` |
| wikidata/family-labels-ES-es-0-f89215c32a48.json (Wikidata Query Service) | 2026-10-24 | 186632 | `5918debea373d460756ac76d2e2784d7ca442eb55474fb05548db5ba074cd8c6` |
| wikidata/family-labels-ES-es-400-2476027c4d85.json (Wikidata Query Service) | 2026-10-24 | 187502 | `2b6f3580f25b44b5dae1fb154312bcf7fce91863ce239096d067927d65c51c94` |
| wikidata/family-labels-ES-es-400-a411f1228638.json (Wikidata Query Service) | 2026-10-24 | 185519 | `fdc75f73d82cd4a86d112f21375bd817258a8b7fddfaad4b610e82fdec5c8d27` |
| wikidata/family-labels-FR-fr-0-5b310f5760cd.json (Wikidata Query Service) | 2026-10-24 | 180466 | `e61564090ea669dbc4035e0d7a4ac1f2ea23e5b9c5a195f1ea6c3ee99c6853b3` |
| wikidata/family-labels-FR-fr-0-6fc811a88f74.json (Wikidata Query Service) | 2026-10-24 | 138780 | `d29c3896ffac4ad08674b15f2af4a9bed4bf6761d8644da05bcf481d7cdb72d0` |
| wikidata/family-labels-FR-fr-0-805dedbdf54f.json (Wikidata Query Service) | 2026-10-24 | 180786 | `1edb140ccd368653157bc3d73ed842f1755d9aeb4d6899f1e2a1c8812c05c89d` |
| wikidata/family-labels-FR-fr-400-70ba74296c91.json (Wikidata Query Service) | 2026-10-24 | 180848 | `541fad4e8c5d3f87c8d7c8cc1fafb2cbecf123c18b7b3b4b8d7cdc94eca6700f` |
| wikidata/family-labels-FR-fr-400-93fc9824dabd.json (Wikidata Query Service) | 2026-10-24 | 138842 | `912f114492fa095c6fcc25f1e7d308055f598c67770593cbbed07f4d6ccc80fd` |
| wikidata/family-labels-FR-fr-400-c670f48109da.json (Wikidata Query Service) | 2026-10-24 | 180726 | `421e40b9f5bc3969a50a804bc834772aaffdea521d44b6a817dfa67b250735a4` |
| wikidata/family-labels-GR-el-0-16e1dd2aef38.json (Wikidata Query Service) | 2026-10-24 | 175404 | `3da5adc782a066cddc17b770baa0d9aaf27b57e4fad2495f6ee7a22499b2207f` |
| wikidata/family-labels-GR-el-0-41fb75775645.json (Wikidata Query Service) | 2026-10-24 | 185759 | `4b13448edfd9444747bd7883cfb831ee1a98123f188c38d15c09d2faa11c40bd` |
| wikidata/family-labels-GR-el-400-34902902cae2.json (Wikidata Query Service) | 2026-10-24 | 201200 | `17b271c47a46567e2b100732b55d5141664b130a60e64e4a26c8368ba9d686c0` |
| wikidata/family-labels-GR-el-400-f48b004a8c6f.json (Wikidata Query Service) | 2026-10-24 | 194199 | `b0cb6b95c91aafc3dd8b6338afa4f977fc97ec054548d5fb3d5a970a1012e6de` |
| wikidata/family-labels-HU-hu-0-4abed0891112.json (Wikidata Query Service) | 2026-10-24 | 199507 | `41b335e704dd06f66b6998eebac4f729f14334c4a218c98f4545980303223336` |
| wikidata/family-labels-HU-hu-0-66f132094343.json (Wikidata Query Service) | 2026-10-24 | 200256 | `88e252a4cac36a2b705de287dd9d7ea89a4eba5383ba069674e233541c1d0b33` |
| wikidata/family-labels-HU-hu-400-a9c02c4e8c2c.json (Wikidata Query Service) | 2026-10-24 | 197751 | `582b80d26662b4c367f5f634d0a97d134863d3d0340ed68fb4fbb7b42f6392bb` |
| wikidata/family-labels-HU-hu-400-e24d92a538f9.json (Wikidata Query Service) | 2026-10-24 | 197039 | `83f5f80162f2ef978c21f890db8ede8d37d8a47f837d37718c1802e08491ae84` |
| wikidata/family-labels-IL-he-0-71b3223c38c5.json (Wikidata Query Service) | 2026-10-24 | 195073 | `a88810053aa1996ceddbd794fb0bf0a00757054b5efa276a0bb334837022726f` |
| wikidata/family-labels-IL-he-0-fac8914671c4.json (Wikidata Query Service) | 2026-10-24 | 195355 | `21fcbcc5dc8baf551a79f98fa7ce289ae96b8c3ef0afa127952e71f32243c135` |
| wikidata/family-labels-IL-he-400-d5ea41830605.json (Wikidata Query Service) | 2026-10-24 | 179853 | `089f113b9291ee21bcdcef45e78956a15b59b5ae7949ed59bb23f9c7970431fe` |
| wikidata/family-labels-IL-he-400-f3ec5a03ffad.json (Wikidata Query Service) | 2026-10-24 | 181951 | `26fa6fffb5e29a24e03c895cd0eb69eeb0a87fee4333e870e8c32a9a490dfe76` |
| wikidata/family-labels-IN-hi-0-6379fd0853e9.json (Wikidata Query Service) | 2026-10-24 | 142955 | `1ae070f81d9503cce7da5c2a1d782b3766be9a7496fde097218122408cea05ff` |
| wikidata/family-labels-IN-hi-0-72d7d2c294ec.json (Wikidata Query Service) | 2026-10-24 | 142857 | `31a1c43a27c7850d18826696317c960e5f52320e8458de278fc015a27729eff1` |
| wikidata/family-labels-IN-hi-400-49b0bf7a8d86.json (Wikidata Query Service) | 2026-10-24 | 145809 | `8ad8a3e430426296a91e630b59ceb844bf3b9bb1f2dd16966f13b9165de05291` |
| wikidata/family-labels-IN-hi-400-89e4fb0e042e.json (Wikidata Query Service) | 2026-10-24 | 145983 | `1695bd47f04bcb1c39e985a0dbc809546184d222c4e8bdfd6ea452468e10cb1a` |
| wikidata/family-labels-IT-it-0-381e18280e2f.json (Wikidata Query Service) | 2026-10-24 | 175302 | `096823af327615e70c975e2961c1fd5a09be2202af04742d6fbb3a5b063bf8b0` |
| wikidata/family-labels-IT-it-0-61fd90879143.json (Wikidata Query Service) | 2026-10-24 | 175158 | `ce698920c6d70be2bb73e6720421d8b06595a8746eb9b87ce59681a9d54658f7` |
| wikidata/family-labels-IT-it-400-443b22e6223d.json (Wikidata Query Service) | 2026-10-24 | 176552 | `a83a6e2aaa5b715a07bc1718cfae6351cde2c26bc0e66e4ea1cd5fa4303ffb00` |
| wikidata/family-labels-IT-it-400-57654c7b3f1f.json (Wikidata Query Service) | 2026-10-24 | 176368 | `5b032b74dbffc8206d1520ea7d9d87166208d1e0c814949c3e410a5239842930` |
| wikidata/family-labels-JP-ja-0-14293389f0e8.json (Wikidata Query Service) | 2026-10-24 | 150505 | `771fc93d6c9170586769f2f0d278084c250c86067a0b8011722675dbca5cd470` |
| wikidata/family-labels-JP-ja-0-1ff1ec3792a5.json (Wikidata Query Service) | 2026-10-24 | 176797 | `febe3446dd494dc03555664de5febaf0af726b7674e9b792c70a515b4c42959a` |
| wikidata/family-labels-JP-ja-0-5b215bfbee04.json (Wikidata Query Service) | 2026-10-24 | 221422 | `579ea20da257ad43ce480f5c54ad71d558f83c25fe3a9a28e0ee31471d19ed16` |
| wikidata/family-labels-JP-ja-0-d67263a6854e.json (Wikidata Query Service) | 2026-10-24 | 221617 | `57a99f2754607cd73fca55ee68c0d6d7256ba026cd17c5767a93cf10804fc531` |
| wikidata/family-labels-JP-ja-400-0e8bd3b24090.json (Wikidata Query Service) | 2026-10-24 | 155793 | `3a931656b7d5a6d2c3a038e0e9137508f514772c3a40ee2fd5da0d22e4e72db9` |
| wikidata/family-labels-JP-ja-400-21fa55cd7fbb.json (Wikidata Query Service) | 2026-10-24 | 187156 | `9c83976a113721f8c57a4983d9baff2e75799039bfb990ce5352068ecd9a7e87` |
| wikidata/family-labels-JP-ja-400-4065c65a7f9b.json (Wikidata Query Service) | 2026-10-24 | 230763 | `8571a31ffbe08a89e965fe49aecd08028fbe1ea5739b27efd2614a0403104ee2` |
| wikidata/family-labels-JP-ja-400-c40aea86bf0f.json (Wikidata Query Service) | 2026-10-24 | 230182 | `2daad71a77bd9a91b48ac8483507bc8687e5d123040fa24e7d683c58dbc5cf43` |
| wikidata/family-labels-KR-ko-0-dc578b7435d9.json (Wikidata Query Service) | 2026-10-24 | 268943 | `507a7e00342d0eefa85debaf667983f943a93ce91633d6cf69b895039c2e09df` |
| wikidata/family-labels-KR-ko-0-f2f18898b394.json (Wikidata Query Service) | 2026-10-24 | 272870 | `0bed779fbb36e9e333430286bf5612f3359171797a7ce2d62875c8a60968b514` |
| wikidata/family-labels-KR-ko-400-6b1ed83c9492.json (Wikidata Query Service) | 2026-10-24 | 58612 | `7dc2638901a539d35d62a545d6e4ea6343dcd5a4c2f70719983229962d362076` |
| wikidata/family-labels-KR-ko-400-a8c7a9f7faa3.json (Wikidata Query Service) | 2026-10-24 | 36381 | `8c86939ec696caf4dbad18c507d47842177d7d7fca8646999c556ca1811c66b6` |
| wikidata/family-labels-NL-nl-0-1fee7bec9027.json (Wikidata Query Service) | 2026-10-24 | 166019 | `43b78a2eb6ca478b028684042e89bb8ed006970e32909ad27deec729b9c34a7f` |
| wikidata/family-labels-NL-nl-0-6c97d04299ea.json (Wikidata Query Service) | 2026-10-24 | 124748 | `772cb583444a114296b167abe7530e7e7b96e1e7346f92fc3db471a2b8db559b` |
| wikidata/family-labels-NL-nl-0-a823ee96599d.json (Wikidata Query Service) | 2026-10-24 | 179799 | `8c2670b8e43e6b2a103f7589d4301618a7f7830977284816b6e9b0929664e647` |
| wikidata/family-labels-NL-nl-400-229786e61734.json (Wikidata Query Service) | 2026-10-24 | 143371 | `94702af3d078c533d67de678e54ae8366ce70b50d9abf3e677cf0de966c287bf` |
| wikidata/family-labels-NL-nl-400-6d30bc616ebe.json (Wikidata Query Service) | 2026-10-24 | 109974 | `0297aa1101976e6016e6dcf1658a723a650e0516991ea280496b0da5771d1fda` |
| wikidata/family-labels-NL-nl-400-951e0df7170e.json (Wikidata Query Service) | 2026-10-24 | 178893 | `a805d2449b132b0f2eb2796d1ae4fea40f51af060ec163836572edf74c3b6bb1` |
| wikidata/family-labels-PL-pl-0-059743a25a01.json (Wikidata Query Service) | 2026-10-24 | 216195 | `f4e2d6fb212ec41582df5ecbfc2048451c21d04afba2f824ea82d814f445e283` |
| wikidata/family-labels-PL-pl-0-de5d71bfbe2f.json (Wikidata Query Service) | 2026-10-24 | 216421 | `08750b731d9877081b5191a8718c511e44f71e449680046b3cb225dae034dadf` |
| wikidata/family-labels-PL-pl-400-00f278b112a7.json (Wikidata Query Service) | 2026-10-24 | 218930 | `17442f00102385c38e05cd95a252f185e4eaffcc1f17bf6ca6d35afcdddb3d3f` |
| wikidata/family-labels-PL-pl-400-e647f278a62f.json (Wikidata Query Service) | 2026-10-24 | 218888 | `f0d36af1dd63551b58ed17a3ad3f6676e125abf851bdce299a79dadaa1534a95` |
| wikidata/family-labels-RS-sr-0-68bf66140f80.json (Wikidata Query Service) | 2026-10-24 | 185776 | `e1eaded430caf8f858d2f775f0139d5949dc9d776338f1e156e43eb11b477c43` |
| wikidata/family-labels-RS-sr-0-e188b772da74.json (Wikidata Query Service) | 2026-10-24 | 186311 | `ac16804b996b1aa7f1c75ec22d6a7a41de39a6a0339e44d4bd60f7e578ff641c` |
| wikidata/family-labels-RS-sr-400-5d288fb6e562.json (Wikidata Query Service) | 2026-10-24 | 177108 | `d99144d1b1ca3daad9b0c3ee5374e61479f923194bdbd098f23ebf613288386d` |
| wikidata/family-labels-RS-sr-400-bcec929b7fa0.json (Wikidata Query Service) | 2026-10-24 | 175254 | `280e5ed045f8b7c90e89e260a61e0549e880c092e4be75ca1aea33574a002c10` |
| wikidata/family-labels-RU-ru-0-04a772a2c3ff.json (Wikidata Query Service) | 2026-10-24 | 204717 | `71ff3e80956a5c456798e2e9889dd1b8a76756bb299e296a503bc13736190a3f` |
| wikidata/family-labels-RU-ru-0-cf62b364a1f2.json (Wikidata Query Service) | 2026-10-24 | 206643 | `e3d1138d6dde26f4fd6854fd4e755efd1f4b0e99eda7ae923d648b479e94bdf7` |
| wikidata/family-labels-RU-ru-0-fbcb00fb0a4a.json (Wikidata Query Service) | 2026-10-24 | 157042 | `476c94437d5260812c35ec14fbaabdad157888d29647011ac29b1a9c702a9390` |
| wikidata/family-labels-RU-ru-0-fdf405f11e39.json (Wikidata Query Service) | 2026-10-24 | 134454 | `b9e807a01c47b1ac7203a9fc2417f80007ae4228e199bb5ac2a0f8ff1bb2f237` |
| wikidata/family-labels-RU-ru-400-0461c4655343.json (Wikidata Query Service) | 2026-10-24 | 203833 | `8dcba550c1aee8582d33b07ffd44328acc9a770e248efdf9c664812e18aeb504` |
| wikidata/family-labels-RU-ru-400-2f052e63ff3a.json (Wikidata Query Service) | 2026-10-24 | 156640 | `b16e64cd32c93dd38e6726207623fee4f074525ad6756a15d530a2fa2614d99b` |
| wikidata/family-labels-RU-ru-400-45f41277b92e.json (Wikidata Query Service) | 2026-10-24 | 204383 | `bc7f337871229fc709cd6cb7809e9b44ba00096500028376effdde127e0cf29c` |
| wikidata/family-labels-RU-ru-400-c75fd28957e9.json (Wikidata Query Service) | 2026-10-24 | 132432 | `90bf068f68af0060450b4970103d40f712b812d39aad6e9301e6c87851bcb4ca` |
| wikidata/family-labels-TH-th-0-3d376ae95b3a.json (Wikidata Query Service) | 2026-10-24 | 162734 | `95b77c3cdc82764b43654fc9043c924d072f8e1f9e3e2b25b8114446454889b2` |
| wikidata/family-labels-TH-th-0-5208b43624c8.json (Wikidata Query Service) | 2026-10-24 | 92972 | `94aeb01f65a1eaf818360ca01e65984c909416276613c6bc5e2c7c5a12f61dd9` |
| wikidata/family-labels-TH-th-0-5a29d13b2f8c.json (Wikidata Query Service) | 2026-10-24 | 163004 | `3d0994f2b76ee1b96aa0cd12fb78a695b63794429bfa7cc0352f7cb6b281d4ab` |
| wikidata/family-labels-TH-th-0-8e72784647cf.json (Wikidata Query Service) | 2026-10-24 | 119590 | `3ba9c9476f710507b448b4778b30ace9aa26947b8e03cd37134ea75b9f7fb498` |
| wikidata/family-labels-TH-th-0-fb3a10ed6367.json (Wikidata Query Service) | 2026-10-24 | 92975 | `485618eac50f6d0ea65abdf261b2a2b61f1365ada564a16cdd7492cbd8bf6d59` |
| wikidata/family-labels-TH-th-400-1d21cab9c711.json (Wikidata Query Service) | 2026-10-24 | 92527 | `9bf632d231d891779baf9d345d73e8bf0935b1416b817d8fcf10e3c7a129ce85` |
| wikidata/family-labels-TH-th-400-28473710a211.json (Wikidata Query Service) | 2026-10-24 | 136947 | `8690a3a1eec8c189744f9b2c4bdddcb0e90c754255a16ff37da2a9b767eae850` |
| wikidata/family-labels-TH-th-400-49bda85f9671.json (Wikidata Query Service) | 2026-10-24 | 92311 | `9937c8369eb02f71853207a00e9a261fff4676bdcb58cace57fbc09dbe7a816c` |
| wikidata/family-labels-TH-th-400-5fa67759d782.json (Wikidata Query Service) | 2026-10-24 | 95791 | `582805b38daec940f0e8d997553077c3a43966711d244534570f1d8497e8fc7a` |
| wikidata/family-labels-TH-th-400-acdf1d7eaf76.json (Wikidata Query Service) | 2026-10-24 | 136935 | `ef2cab50ef0053d45d701962ee5630b8946835f3793f922694e7c15e2fa61576` |
| wikidata/family-labels-TH-th-800-2f19b7849be3.json (Wikidata Query Service) | 2026-10-24 | 8367 | `805c9f668274a53f2620e0c5c39e8c140159ea0c20c266d547bb6c14a6a159d7` |
| wikidata/family-labels-TH-th-800-80120d870330.json (Wikidata Query Service) | 2026-10-24 | 8246 | `0b4d7f3ff96a1df39f059e9be3ff8897bfa503a3c8a47a1d1bf49e2b910ff668` |
| wikidata/family-labels-TH-th-800-c0aaa8c7d96a.json (Wikidata Query Service) | 2026-10-24 | 6003 | `70ab43706b64f74281fdc99e0bb4e7b6f660ec7476bb190be819d42fb7d7e256` |
| wikidata/family-labels-TH-th-800-d4daf1cf203d.json (Wikidata Query Service) | 2026-10-24 | 6259 | `5758a9c0cfb222dbf946c872b32a7c2cef455bd9f2e7468ed637c8d08e2076e0` |
| wikidata/family-labels-TR-tr-0-38aae3a3a7a1.json (Wikidata Query Service) | 2026-10-24 | 212825 | `21dbfad2ded3a5578bd9a733ff6e45408721961b3534d5a1ec929fc12c8567e3` |
| wikidata/family-labels-TR-tr-0-6fd5d994907e.json (Wikidata Query Service) | 2026-10-24 | 213072 | `6531f0ee78e324ff29a49157825290efb4c095bc0b191e0e7e8b761f3de54583` |
| wikidata/family-labels-TR-tr-400-2a3301ec1876.json (Wikidata Query Service) | 2026-10-24 | 217693 | `23f0a41041ba83bd46e9d5a7856eee3d5c04cc494737f3dfc2a701606604372c` |
| wikidata/family-labels-TR-tr-400-616377876403.json (Wikidata Query Service) | 2026-10-24 | 217800 | `ae68837aef45df5bc45ff0dcca0a35f03e7d5c0f0fccec622bbdf82d44b8e969` |
| wikidata/family-labels-TW-zh-hant-0-979389264403.json (Wikidata Query Service) | 2026-10-24 | 171899 | `1eea585972310b19fdbfd1780da536fc0adbcfd1969d6ad77b40325b010772c0` |
| wikidata/family-labels-TW-zh-hant-0-cc3cf2871f8f.json (Wikidata Query Service) | 2026-10-24 | 172052 | `190d90c7d73f168f2f7e5b581218f8ac69e0263a28c4696f7998eac1a8f5b79e` |
| wikidata/family-labels-TW-zh-hant-400-5fe1ff6715f1.json (Wikidata Query Service) | 2026-10-24 | 78749 | `c3683ea51262e41858c6e1b3fd4df418c5d837636435a7bfa7a166e44bb71efb` |
| wikidata/family-labels-TW-zh-hant-400-80777864ac34.json (Wikidata Query Service) | 2026-10-24 | 114485 | `3726dfeb51a9821e6eaf83e464c4a98922da45184283ac9d17d1c5b8e096057c` |
| wikidata/family-labels-UA-uk-0-4e0773356736.json (Wikidata Query Service) | 2026-10-24 | 290936 | `4f254c36f300b8068a396a5b66f644e1aaf03ade102981a59cae8c8d8863c861` |
| wikidata/family-labels-UA-uk-0-d663433e7ed4.json (Wikidata Query Service) | 2026-10-24 | 293385 | `742312b2706398b2fb7726f8983d322ebd53cee98229d3edabe6fe3410c004b0` |
| wikidata/family-labels-UA-uk-400-065826a8ced7.json (Wikidata Query Service) | 2026-10-24 | 205096 | `e56294a154db0227814106eb303938895dc273ebfad440f76ec712789f47eb7f` |
| wikidata/family-labels-UA-uk-400-aa9e4d4a6e8b.json (Wikidata Query Service) | 2026-10-24 | 202379 | `fdac99fa9da2890b8382ff166b1908fa0461237d5eda3446c97a73825c85dff9` |
| wikidata/family-labels-VN-vi-0-1a88e9c018c3.json (Wikidata Query Service) | 2026-10-24 | 117503 | `112cee26a17cb3dfd2a968569e796f19b4bd4dfd9df3c2a72ee8b49f0f1995f4` |
| wikidata/family-labels-VN-vi-0-756f9d7ce09c.json (Wikidata Query Service) | 2026-10-24 | 130942 | `801737e654802668649eafd5873b49f64561d9bc6857fef8afed3a47f7bc14dd` |
| wikidata/given-BG-5c3854836298.json (Wikidata Query Service) | 2026-10-24 | 486717 | `e65b22b7e515b5ad956d36e689429a3149c8ee39f790e01d46ec4c4d194e330b` |
| wikidata/given-BG-7c69a04e1a9e.json (Wikidata Query Service) | 2026-10-24 | 546076 | `3e2480d5322e11c9dedabec74ad8f8f41e5e10f40476234142343a553594d8d7` |
| wikidata/given-BR-61a54126e08f.json (Wikidata Query Service) | 2026-10-24 | 546398 | `b594941ea4f205994534529108ecd982c4802621ed04efc3cb2c700c9b2e8445` |
| wikidata/given-BR-9c9c4f4655e8.json (Wikidata Query Service) | 2026-10-24 | 546660 | `70a08b1937df75ea29005ea6a84c30e93315770be484d96db0d2633915bdffe5` |
| wikidata/given-CN-39a41b65f54e.json (Wikidata Query Service) | 2026-10-24 | 546436 | `8f2732454d6a42d906135b560fd03e347de1ae4162c8b046549790e491262a3d` |
| wikidata/given-CN-9d0aa69ac030.json (Wikidata Query Service) | 2026-10-24 | 546431 | `ee046c05877b58ffca3495dce9bab55ef9a3e3100489ac8bbf9ab43708cb0038` |
| wikidata/given-DE-17de68463ec8.json (Wikidata Query Service) | 2026-10-24 | 546761 | `35376904124990ed82588570206a88a6f358701cf6a20e1f354ee45f6790b03e` |
| wikidata/given-DE-3230bf4e384d.json (Wikidata Query Service) | 2026-10-24 | 547100 | `8be3063fadfbade4e14e05e4930c5e941e42c2622d8225947067c04cead6be3e` |
| wikidata/given-EG-1711a0e3bb2f.json (Wikidata Query Service) | 2026-10-24 | 501971 | `39bb1d238818faf71be3a298fd11e2303057d4163b930da701ab288502c3b06c` |
| wikidata/given-EG-cc9a6bcf950d.json (Wikidata Query Service) | 2026-10-24 | 400391 | `98827996089c507ce4a6697800aedba5973cefaa47fa82079feed847f2ef7a57` |
| wikidata/given-ES-7df1b3675cbb.json (Wikidata Query Service) | 2026-10-24 | 546530 | `4921991b791500fd4bce1a4d47636c8b0d9e22f77bb8029652a1cc0a064df885` |
| wikidata/given-ES-8482122ff7ca.json (Wikidata Query Service) | 2026-10-24 | 547028 | `d3aafd2a56f63ce7b5868ec7d0e898db567623bfae81aa47a965e0ea8a025527` |
| wikidata/given-FR-5bfabf5eaf87.json (Wikidata Query Service) | 2026-10-24 | 547351 | `9edb40ff2eb13d99287632a52493d6d09d925689548f6a345e2495b68d61da7c` |
| wikidata/given-FR-e9c3a1a7b0e3.json (Wikidata Query Service) | 2026-10-24 | 546890 | `3f7dffc55278783b7661482b291bc856cb624b11d5030db9e22afd475683727e` |
| wikidata/given-GR-2071b7b80502.json (Wikidata Query Service) | 2026-10-24 | 545905 | `a53419dfdd8bb495c11d17abb3b38645221554126e456bf60c65928502678ecf` |
| wikidata/given-GR-af11ad25e3ca.json (Wikidata Query Service) | 2026-10-24 | 545991 | `147e83bd164b14b65b236cd1e4089529ddff7ee791279f2dda0ff5ea7f64b780` |
| wikidata/given-HU-dddb05066c32.json (Wikidata Query Service) | 2026-10-24 | 545947 | `4fb292fd3811058e46cae78fd5b5c5aa8d531dcf054923a962879dd9c7fa97b7` |
| wikidata/given-HU-f207f26a5164.json (Wikidata Query Service) | 2026-10-24 | 546355 | `2eb0722914e0806321fdcfe7253ce54a8829fcfb7ad6ad94139b1b09060674f8` |
| wikidata/given-IL-76a35b4578c2.json (Wikidata Query Service) | 2026-10-24 | 546144 | `f31736fc3d7f6929383418f9a9745d4e0889775bcab5f3111303443fdacc474a` |
| wikidata/given-IL-fc3d003ab047.json (Wikidata Query Service) | 2026-10-24 | 546187 | `82f91d5b8480cc657f7ad4d75beaa57f617f464104ea4a1127813858644ec7be` |
| wikidata/given-IN-18a9c55ffc79.json (Wikidata Query Service) | 2026-10-24 | 546112 | `01f7ed479b23aaa0a1cd10e9fccea29c3062e3a34a9aa4d586f737e34d628981` |
| wikidata/given-IN-fd1a893c8dfa.json (Wikidata Query Service) | 2026-10-24 | 546132 | `52847ee501572f2e57839ae50fd2eb56fb1564355d6fa854d45655da3abd84c2` |
| wikidata/given-IT-3fc86bd61f7b.json (Wikidata Query Service) | 2026-10-24 | 546214 | `787805811ab94d979b6e389aa4ab016e27174b01498bc651ffcd15f0bdbda3ea` |
| wikidata/given-IT-60d00c171a5b.json (Wikidata Query Service) | 2026-10-24 | 546508 | `b7534ba7f102da85110e2a50c2bd887d255c58a6638849df019cfb77c81e9908` |
| wikidata/given-JP-a7bdef80c751.json (Wikidata Query Service) | 2026-10-24 | 547569 | `b2ac791649b4b9dc716e8c13f3b544704af9e40029622360e64b941143d73c6a` |
| wikidata/given-JP-e2b71b19b112.json (Wikidata Query Service) | 2026-10-24 | 547590 | `db0b54ae5944268b3adcf24025b9afdc248610c263ff6f1bd645ed7747e6ce07` |
| wikidata/given-KR-27fe57aeba78.json (Wikidata Query Service) | 2026-10-24 | 546632 | `a1415222c9b124e2ec5c646bf2709d0918678cb7c9c94de98483345a61ab4a12` |
| wikidata/given-KR-f5d299121cd5.json (Wikidata Query Service) | 2026-10-24 | 546542 | `4610ef565f0c8db8917c0f9db11a35613395a69ea86e6fe1ea3ef51af9e0003f` |
| wikidata/given-NL-40997d34e208.json (Wikidata Query Service) | 2026-10-24 | 546276 | `e667e9cca5435e0df8b1b15f8253fa741519f9b88aee3206803c69e2d96e03f6` |
| wikidata/given-NL-baf6eccb6ea5.json (Wikidata Query Service) | 2026-10-24 | 243231 | `86a4c89c443b4485bbc6422e8b6baeda1f9fa8b00aad35f2b5b27d8d87ee34d4` |
| wikidata/given-PL-a12c348a5e6f.json (Wikidata Query Service) | 2026-10-24 | 546017 | `2df04e722369627678d6450a8e6d2c860eeda3ba8e3680108598eb942a85d356` |
| wikidata/given-PL-a1556f440136.json (Wikidata Query Service) | 2026-10-24 | 545844 | `a908af82774b3209fbf28e4c407d05e8e6b85c54efc9888fec69e5c90095c0f9` |
| wikidata/given-Q11879590-native-th-00a116f2bef4.json (Wikidata Query Service) | 2026-10-25 | 13284 | `36e41c16f4901e677b8446a4f18e7cd5c38a419fd74b8a18adee0e155c06d521` |
| wikidata/given-Q12308941-native-th-02a96d2b6250.json (Wikidata Query Service) | 2026-10-25 | 15679 | `b920987e90245ca89d085668a522f05a00d8e3f75c12b93565c6b004613162c9` |
| wikidata/given-RS-1a62c1887ddc.json (Wikidata Query Service) | 2026-10-24 | 446880 | `6f8f3ef14bb0d04feca1e4527247608408317b98544185771e24d9e7ee316471` |
| wikidata/given-RS-c6944c0f5930.json (Wikidata Query Service) | 2026-10-24 | 402425 | `6caefcd9d09d8db9a16ff35f644185f650643f593930783fb99300692b1393ba` |
| wikidata/given-RU-c511b7d00e5e.json (Wikidata Query Service) | 2026-10-24 | 546072 | `a2a31b1fd6d6c3c56c52893c7ef569b2dab7a33c2506d50b599ce23e93fa7b62` |
| wikidata/given-RU-d24ebbc8efe2.json (Wikidata Query Service) | 2026-10-24 | 546067 | `3182d814f93602bb06bd1d6e5ceda12dd362fd801a71e716eefc180ec61c6c3d` |
| wikidata/given-TH-64de92dd5042.json (Wikidata Query Service) | 2026-10-24 | 435687 | `3a1666669bb1cffde2c22693e1e899251a8fbb1087a408c0ee5e887619b95f28` |
| wikidata/given-TH-d84ff6b601bb.json (Wikidata Query Service) | 2026-10-24 | 411632 | `e910c02eba1d4dde18d72843f0ae9fdb4bbf720c4cc23dda6f16083dfd4357e9` |
| wikidata/given-TR-75f9339daad0.json (Wikidata Query Service) | 2026-10-24 | 546965 | `a6f3b7873db91063545c2eecc0021a3abe30f03536a4f0dd3febba869f17e8ce` |
| wikidata/given-TR-b23aa501cbd9.json (Wikidata Query Service) | 2026-10-24 | 546552 | `80f281cf1e729a091e62e48025c5bbc60ce2c8d9f6437607d64f6aee720e2a1c` |
| wikidata/given-TW-2abe3632adcd.json (Wikidata Query Service) | 2026-10-24 | 546365 | `63f621951bc6a0115dc5f4466005383e818499c1164f7a581fd89e32e1435417` |
| wikidata/given-TW-4fa40d918d34.json (Wikidata Query Service) | 2026-10-24 | 546368 | `0eb1c4abc2beade0196a9374bb14b02115de650b5072825c12200dc3c74f30b4` |
| wikidata/given-UA-6f2445c6cd16.json (Wikidata Query Service) | 2026-10-24 | 519530 | `dcaa6626e93b382770625221727085ad8098a461a6d5b25638bb653d121fdb34` |
| wikidata/given-UA-c2c5ac9dcdca.json (Wikidata Query Service) | 2026-10-24 | 546851 | `6d89a00924e88594a49ab4036c042368794019107d03372a2c76747d6b3f4153` |
| wikidata/given-VN-af5938172bba.json (Wikidata Query Service) | 2026-10-24 | 220940 | `fbb391dbf9880c9335d428f451d03a27d0ddfdc5a5ff4035a22e7e1db42a91c3` |
| wikidata/given-VN-c86a6ca1426c.json (Wikidata Query Service) | 2026-10-24 | 183795 | `be51471b48419146fc3a15fb0946532f775f2d3dec85669b46f6b09a2fc0e4ce` |
| wikidata/given-bearers-TH-974a297a5628.json (Wikidata Query Service) | 2026-10-25 | 9646 | `579255dc8276a59246b2b1869f27e75c979219d6bb16d2714809522abcaae0e8` |
| wikidata/given-bearers-TH-e8b9921e25cd.json (Wikidata Query Service) | 2026-10-25 | 9646 | `8cddf9512fb714852618232998cf0273955d1320dd01f998405097c3ae982166` |
| wikidata/given-labels-BG-bg-0-ef8d1569a824.json (Wikidata Query Service) | 2026-10-24 | 165790 | `45174df33b48471076ca845df4f0f5423066ce22d6f320cd96ee30ec7eaa1602` |
| wikidata/given-labels-BG-bg-0-fd2cca54c71b.json (Wikidata Query Service) | 2026-10-24 | 163506 | `8ad7953cfdfcbc4fa88cc76f89053203c3df26e747043a4ff3513f8106091327` |
| wikidata/given-labels-BG-bg-1200-808c6e198837.json (Wikidata Query Service) | 2026-10-24 | 117560 | `0fe3b09deb9edc5d3ad9e5e80da149723fcd7a8c19921cabed2b7cf5dcf5d65c` |
| wikidata/given-labels-BG-bg-1200-dc550eac20fb.json (Wikidata Query Service) | 2026-10-24 | 50218 | `e079795555a4832ed32224de8d8b98014fbe8d32188a551d63e639a1f09c9a66` |
| wikidata/given-labels-BG-bg-400-6e7a58e08e19.json (Wikidata Query Service) | 2026-10-24 | 163866 | `551333f7c365f31b7edc28bd358624f6a8f0acdbbdd925df3d5fe1edec07443c` |
| wikidata/given-labels-BG-bg-400-d48ab05aed97.json (Wikidata Query Service) | 2026-10-24 | 156208 | `3b5a2dcbf777c631711b32eeae3f7528816ffdb2ced7b6c13cceae9066796c78` |
| wikidata/given-labels-BG-bg-800-0b7edb79a6a5.json (Wikidata Query Service) | 2026-10-24 | 163083 | `1b60f89b371aea53abae48661c5f5ee2c6de769c99b090ca051747ebe8a14abe` |
| wikidata/given-labels-BG-bg-800-8f280b58f329.json (Wikidata Query Service) | 2026-10-24 | 156514 | `abe4979f4ec31bc643f8ea3a570984f671fcf52c1df21d44db076b48e8b4834f` |
| wikidata/given-labels-BR-pt-0-2cadaeb6e751.json (Wikidata Query Service) | 2026-10-24 | 168794 | `77f0d1d98999ecf5dadaa3a6b893d669a224283891aa627d497e898b14608d12` |
| wikidata/given-labels-BR-pt-0-407b49a884f0.json (Wikidata Query Service) | 2026-10-24 | 168406 | `bb1be9eab90e741d9345d6d30d8e544cb4052f4597878a62cacbe37ab35f292e` |
| wikidata/given-labels-BR-pt-1200-5c0ac8b30143.json (Wikidata Query Service) | 2026-10-24 | 134079 | `595f0a7bfabf9483e6650a5b8ca16678e834e9a4e9dba1c8efdaa4791b6583e8` |
| wikidata/given-labels-BR-pt-1200-cba08ffcf96d.json (Wikidata Query Service) | 2026-10-24 | 134796 | `87ac8feb54b3a9da6689e600af1c61d3adb787c0e0e0ff9113332af71b0174d3` |
| wikidata/given-labels-BR-pt-400-44740a1ae019.json (Wikidata Query Service) | 2026-10-24 | 174302 | `39be313b4042934277146da9c4bb6696bfe51d6606b5a5bdd841155ea96ff923` |
| wikidata/given-labels-BR-pt-400-4e1fa1693586.json (Wikidata Query Service) | 2026-10-24 | 176167 | `5fd1be2c1721c93e67abe662fe8256c29ed40555bf099633016718cf24da8147` |
| wikidata/given-labels-BR-pt-800-17e8c207c5cb.json (Wikidata Query Service) | 2026-10-24 | 175752 | `3ac19354d0daf5dcbb9b3072ce9c28763de4f9064f02d558b6857a84b19f51f9` |
| wikidata/given-labels-BR-pt-800-beab77412052.json (Wikidata Query Service) | 2026-10-24 | 175885 | `72bcd7d00f781daca2be4a21b2bdddfcccd3cfd27733e17f4a2ce823b8f5a4b5` |
| wikidata/given-labels-CN-zh-hans-0-2af6094efb35.json (Wikidata Query Service) | 2026-10-24 | 167142 | `c84f695fca32e9f5a97288af2153d6ea19fb07fda72d0ff3d63fb434f9cd5591` |
| wikidata/given-labels-CN-zh-hans-0-357a04c66685.json (Wikidata Query Service) | 2026-10-24 | 166168 | `96779cc1c83165ba0374b9e522a43f8cb06f23edf9153725036627d8f7591bab` |
| wikidata/given-labels-CN-zh-hans-0-a5354b6d5ab0.json (Wikidata Query Service) | 2026-10-24 | 109687 | `17c466fedb64ec92c4536573b3d748ecd729f83612046eba3e5b2719ac953ae2` |
| wikidata/given-labels-CN-zh-hans-0-faa2b77de8dd.json (Wikidata Query Service) | 2026-10-24 | 124262 | `afbf06b143c08f3a3c477a699eacdce8df00aca22411330085be4e4ade5477bf` |
| wikidata/given-labels-CN-zh-hans-1200-4947ab17b9a7.json (Wikidata Query Service) | 2026-10-24 | 47156 | `cd37e09fd23ca8cd453247f21bf7550e51c51eff9fbefb2ff70eccf385d25561` |
| wikidata/given-labels-CN-zh-hans-1200-4bd49a77675e.json (Wikidata Query Service) | 2026-10-24 | 53257 | `c474a275b759d36bd0776144b12ab77b1b20b96734beddc6b099e35685fbf252` |
| wikidata/given-labels-CN-zh-hans-1200-b6a0b53c375a.json (Wikidata Query Service) | 2026-10-24 | 78993 | `3ad9b619dea0faac0adb022b371d708aa01a248d0e49e937a035cad53e422d4d` |
| wikidata/given-labels-CN-zh-hans-1200-dc7f3908076f.json (Wikidata Query Service) | 2026-10-24 | 70398 | `1a95121648d7e91e248212e205655ee8b87ab684c4cbcc51ed92ca02648b0b58` |
| wikidata/given-labels-CN-zh-hans-400-16dc70359d63.json (Wikidata Query Service) | 2026-10-24 | 111521 | `38f4bfe29aba02a3701258193196f43c8fab5640394d4ba03dfa1c4614358074` |
| wikidata/given-labels-CN-zh-hans-400-3204f58af023.json (Wikidata Query Service) | 2026-10-24 | 120553 | `94d12badd4fba7c94b4f5001b66bcfd6b8032ecd9d121967551fe54c4bfde3dc` |
| wikidata/given-labels-CN-zh-hans-400-330a9e72cbba.json (Wikidata Query Service) | 2026-10-24 | 162668 | `7b9441ad44accd074ac8934deb170f33314fbec8d50e2187a4229f8129cbba9c` |
| wikidata/given-labels-CN-zh-hans-400-6ccc32503593.json (Wikidata Query Service) | 2026-10-24 | 156541 | `4caf52b8d69e433419cf54b6731682b1ae1ff192734249c2bffc6245391ad90c` |
| wikidata/given-labels-CN-zh-hans-800-12d788ddfbaa.json (Wikidata Query Service) | 2026-10-24 | 151210 | `a2379661cb44092604f99f39e99aa64678fcc1ee833bdd1ffd5e3fb6c0290e73` |
| wikidata/given-labels-CN-zh-hans-800-2cf41ae8f59b.json (Wikidata Query Service) | 2026-10-24 | 146347 | `e79c0dc62718ac47e8b970ea17fa5ed41460651e4127951db274b12d693ff207` |
| wikidata/given-labels-CN-zh-hans-800-2fae2648d7c4.json (Wikidata Query Service) | 2026-10-24 | 109512 | `34563890f5304cee8b3bd7698a65a3cdb34b1f7e347e32597c1adb4ef8e8c3e7` |
| wikidata/given-labels-CN-zh-hans-800-79c23c0d6dc0.json (Wikidata Query Service) | 2026-10-24 | 104396 | `3d5498b1941575a5226f72ce27de0402d8a60e8c1991b8b612e646e7761b7681` |
| wikidata/given-labels-DE-de-0-af5571b59805.json (Wikidata Query Service) | 2026-10-24 | 175720 | `311dad79f81ddcee51fa1c33558ba0c176df9b474f30ec3be7e07c372d24be02` |
| wikidata/given-labels-DE-de-0-d98f3b668fc3.json (Wikidata Query Service) | 2026-10-24 | 133711 | `202f30dd32ec754b1cdb2d39a647b2a0a238817979fca780aa25c3831a9246cf` |
| wikidata/given-labels-DE-de-0-dd5abbae074a.json (Wikidata Query Service) | 2026-10-24 | 176339 | `e13d9e855b102816863e16ffafb91229ae9540a8cf0e622ebdb204ddeff498cf` |
| wikidata/given-labels-DE-de-1200-44fb4528c467.json (Wikidata Query Service) | 2026-10-24 | 130237 | `133b46c93ff1c177acea9f173b493432540965a2ee3a27ff3ef16d8050c9db9d` |
| wikidata/given-labels-DE-de-1200-90efd0361cfb.json (Wikidata Query Service) | 2026-10-24 | 99414 | `009e98549af0082ff7a1738bc4b19905eba05fd85d725948dee59f881b34fbfc` |
| wikidata/given-labels-DE-de-1200-ad4795bd5fa6.json (Wikidata Query Service) | 2026-10-24 | 130522 | `eb626421417c03e1ae854a17fc5d7fed1e5b899ab95c3bc4ccd569529f848775` |
| wikidata/given-labels-DE-de-400-56f869d6d1ab.json (Wikidata Query Service) | 2026-10-24 | 133533 | `f0fc3b62340b06ff47e1a95d5cf6aa5553b51b45001deeb6bd581c56d93b5e1a` |
| wikidata/given-labels-DE-de-400-c49a85ca0340.json (Wikidata Query Service) | 2026-10-24 | 175539 | `94f51b83b10759ae9fed6ab62dc3de071baed1cd666de309c27665d117d0e802` |
| wikidata/given-labels-DE-de-400-efb8107edae0.json (Wikidata Query Service) | 2026-10-24 | 174919 | `11ee5c4dde8eff0fdf68a1aaf13e90113d38d41c073a558d57ccff4b4dc20513` |
| wikidata/given-labels-DE-de-800-728f3cc1ff8f.json (Wikidata Query Service) | 2026-10-24 | 177002 | `7cf5ed77036d157d3d3a3a7e53c73b7f3c2cb55c85f182053ffee705da21d4d2` |
| wikidata/given-labels-DE-de-800-7e9a0bbf58ed.json (Wikidata Query Service) | 2026-10-24 | 176950 | `c8acdfa288662d2db524bd63d04553757a641f736c31f08410360983c0713e6b` |
| wikidata/given-labels-DE-de-800-e8070f9626e1.json (Wikidata Query Service) | 2026-10-24 | 134506 | `01b7a8914ff2a78a78614ac5572db13e06c089f539fd7283479890e762980059` |
| wikidata/given-labels-EG-ar-0-1b0565d9678e.json (Wikidata Query Service) | 2026-10-24 | 121528 | `171e726a2aa552f0b0f320e5ee891d070895aa624fd82489563287487a956a44` |
| wikidata/given-labels-EG-ar-0-cfb34f7dd80d.json (Wikidata Query Service) | 2026-10-24 | 190368 | `9f2bc9d610fd17c9b4645f0b7c130a2997bf00d4d02bb3a994edf0e73ec0b123` |
| wikidata/given-labels-EG-ar-0-dbb544981b94.json (Wikidata Query Service) | 2026-10-24 | 197527 | `3fb778183d96a6eda683a40270496f7578f7abea7582b767bfa5dbf7d66ed564` |
| wikidata/given-labels-EG-ar-0-fffea04bbee6.json (Wikidata Query Service) | 2026-10-24 | 137950 | `2cbe9019cbcbbbb6b771e6840a938ee5669fd4b63884403c71c9a927273bb0ad` |
| wikidata/given-labels-EG-ar-1200-2a93ddfe37cb.json (Wikidata Query Service) | 2026-10-24 | 71992 | `aad3f6b9d8d768d5fe407dd063ec7c43faac58e8d8a8fbfa46f92df08294cc86` |
| wikidata/given-labels-EG-ar-1200-8b18da4c6d48.json (Wikidata Query Service) | 2026-10-24 | 50212 | `5e69b8cffcc359d3d0bf18c16a6e1aa3f7d84922368ab4609098a35cde0e2ad9` |
| wikidata/given-labels-EG-ar-1200-f96c457b038e.json (Wikidata Query Service) | 2026-10-24 | 46071 | `49a8c5819089159de68d4c4b14ab95fcbf15396e4c62197746a058ac2ee920f2` |
| wikidata/given-labels-EG-ar-400-869297c3b767.json (Wikidata Query Service) | 2026-10-24 | 193584 | `61be52dd1318a959428227c93707dd95c2b268ae4ade862d5c321b2b3181e9fe` |
| wikidata/given-labels-EG-ar-400-a6b98aa14128.json (Wikidata Query Service) | 2026-10-24 | 126299 | `3fd9334d354bfdef46a68236e91d34c883e43cf90bed80a16beb0bfe1b7e47df` |
| wikidata/given-labels-EG-ar-400-e562526c7fa9.json (Wikidata Query Service) | 2026-10-24 | 203303 | `81278291c140cb1be5eb7f40479a89ca6c7459a2f0792e85f73469ee606e6d42` |
| wikidata/given-labels-EG-ar-400-ebb4fb702188.json (Wikidata Query Service) | 2026-10-24 | 133840 | `337a8bd92fc241e9de74a907d79210f737d99a114e293a213d4840b65e55235d` |
| wikidata/given-labels-EG-ar-800-286dc737b03c.json (Wikidata Query Service) | 2026-10-24 | 213160 | `d4532fd347951e86887efa188c0c9bd4ef57fbeb77e514bb5e832146a0a460c8` |
| wikidata/given-labels-EG-ar-800-49e97bb0f504.json (Wikidata Query Service) | 2026-10-24 | 149134 | `cfce8620f528b56f8cef42058e326f52d26213b12c2677bd7e48674b23f5424d` |
| wikidata/given-labels-EG-ar-800-4ee111188291.json (Wikidata Query Service) | 2026-10-24 | 144743 | `50b8f7121491edc80ea2468934861208e356fbab43d0dc51f08ebaf7e3e955f3` |
| wikidata/given-labels-EG-ar-800-7c55a9a8f23f.json (Wikidata Query Service) | 2026-10-24 | 123810 | `d52c4bb14981f21ff23616b84620b44f91e29ee10cadb35ed2db7b8841328964` |
| wikidata/given-labels-ES-es-0-773203555112.json (Wikidata Query Service) | 2026-10-24 | 172606 | `ad4257aeb74f512f1f0c293c51952cccecc91de15e17049012a3c3c478a7bb06` |
| wikidata/given-labels-ES-es-0-aac0fbd64c89.json (Wikidata Query Service) | 2026-10-24 | 170864 | `16e027d63106bff4b4cb03587b546ca671ce14584d2e7f8d21a17bfcc12f3955` |
| wikidata/given-labels-ES-es-1200-0d3b176b7314.json (Wikidata Query Service) | 2026-10-24 | 138301 | `b4f72044a224f95a10da2b500b4bca21f0ada61c8e1fdcdc37233522205860b6` |
| wikidata/given-labels-ES-es-1200-19deb430f4a2.json (Wikidata Query Service) | 2026-10-24 | 135273 | `797e37bebd8ec2195bd80f19e5b6dd0b33c19b4317774fbdb0f4b2fac387c347` |
| wikidata/given-labels-ES-es-400-0b98ec0639a6.json (Wikidata Query Service) | 2026-10-24 | 177345 | `7d3a11d5dd8276a565bd62108130eef7a142e0bfdce07de4f095a67bc49c5af6` |
| wikidata/given-labels-ES-es-400-78100f394d56.json (Wikidata Query Service) | 2026-10-24 | 176770 | `fd2c3a6911bd8ea7b77aa53adea454c06ea6414f2522d5f01c49e1391fc6452e` |
| wikidata/given-labels-ES-es-800-01bf8986025c.json (Wikidata Query Service) | 2026-10-24 | 189036 | `2fc0624a39165392ab46434b7956ce774cf162d3abc2ba8a3363f6ed3477bcd5` |
| wikidata/given-labels-ES-es-800-ab6dd25b1320.json (Wikidata Query Service) | 2026-10-24 | 189592 | `e47bdb73833823362f7296c9c54f2c6f09e74c9956b8c7f1196f963a80945231` |
| wikidata/given-labels-FR-fr-0-0899d7f67bd8.json (Wikidata Query Service) | 2026-10-24 | 175159 | `90b885bdc1578fc0b36b8a4fe7beb8b3c84b3cbd6ec58ca69c72908018020a9a` |
| wikidata/given-labels-FR-fr-0-32d052b88cea.json (Wikidata Query Service) | 2026-10-24 | 176313 | `3a5274e41b73c30f1825c23324118b463410623a05350a814ab20fdb62060737` |
| wikidata/given-labels-FR-fr-0-82e03d848d75.json (Wikidata Query Service) | 2026-10-24 | 132271 | `379a2d8d6fb88540f169b6dfa286e7edf5e0b1faeecda6bb505767e6b44b4364` |
| wikidata/given-labels-FR-fr-1200-155122f4c1f5.json (Wikidata Query Service) | 2026-10-24 | 130192 | `a5e5b379fe743dc0b3ad3148662af9dc2825a219b10f5c72d01f9212079528f7` |
| wikidata/given-labels-FR-fr-1200-2c795008e5db.json (Wikidata Query Service) | 2026-10-24 | 131635 | `bb2e9b8af6ec628d18c4a1788967ba0ea684b44b6c3345ecff62797bef4c1fe5` |
| wikidata/given-labels-FR-fr-1200-7dbb6d512ca8.json (Wikidata Query Service) | 2026-10-24 | 100404 | `732aad6f8cf1d07f69b5dae85b0bcd677795cf24dcd8d62b9aee4753d6acb2f2` |
| wikidata/given-labels-FR-fr-400-3d79c3a4e910.json (Wikidata Query Service) | 2026-10-24 | 175551 | `edff3838fd060751e39aa06ec3c621ddc9509f8fd1edb55481253e0118a4ec92` |
| wikidata/given-labels-FR-fr-400-ce321e96b09c.json (Wikidata Query Service) | 2026-10-24 | 175386 | `e707a242ad0e8c7be000ed76a2984d24f49e85daaad3893160424a8f00081a86` |
| wikidata/given-labels-FR-fr-400-ef0e89bf2291.json (Wikidata Query Service) | 2026-10-24 | 131391 | `5cf3bce6c2a29cee565d234000fcf3ea2bb340379086714b72cf70249dc95c41` |
| wikidata/given-labels-FR-fr-800-532c83d8863d.json (Wikidata Query Service) | 2026-10-24 | 183426 | `3a79ffdc3e44a4c86ee5d6df47c9fb20be89c245e1a473fb04aa75f49a63b148` |
| wikidata/given-labels-FR-fr-800-649e7c41c761.json (Wikidata Query Service) | 2026-10-24 | 181012 | `2a814d94c91255b1a40f4ff5476957cd13d023bc49fafc28abe6a357b5a3fd1d` |
| wikidata/given-labels-FR-fr-800-e54759fad305.json (Wikidata Query Service) | 2026-10-24 | 136316 | `0d353a45364713c625134afe03e709de67e73d81e56556b15639fb1829886d13` |
| wikidata/given-labels-GR-el-0-658ea995744e.json (Wikidata Query Service) | 2026-10-24 | 164439 | `de2734acec63fd1a16ca4b3a8f9977454e2d4f2ef196551af7d465f610f754b0` |
| wikidata/given-labels-GR-el-0-7d76d44da185.json (Wikidata Query Service) | 2026-10-24 | 166833 | `5f00e1497459050730a5818721808a648e6688c513fb65dd9932f32e0f274c1d` |
| wikidata/given-labels-GR-el-1200-60d2192bf6c9.json (Wikidata Query Service) | 2026-10-24 | 157483 | `bc5f681dc0a498f8d7d2ec3b288ff660151105ebad418b26e54741fd262bace1` |
| wikidata/given-labels-GR-el-1200-bdeb6732a654.json (Wikidata Query Service) | 2026-10-24 | 154664 | `fabcb162c278afb90aa574aca095ffdb7fbf707be60b4ef4e68e00b69296445c` |
| wikidata/given-labels-GR-el-400-06f6f66032af.json (Wikidata Query Service) | 2026-10-24 | 160984 | `d78b9bc9662d40d8ea62392135b3030f19ab2965c5418f9936ac12c242ad9179` |
| wikidata/given-labels-GR-el-400-3bf4469b809b.json (Wikidata Query Service) | 2026-10-24 | 162749 | `ed72176450cdd4194cf68476c2aa5ce276120403314ccff40d89bd816f48f364` |
| wikidata/given-labels-GR-el-800-f18877c35846.json (Wikidata Query Service) | 2026-10-24 | 178950 | `f84c03a4417127770fd912baf23bcf672bb5f6bc83f10d6177d4c32182d93745` |
| wikidata/given-labels-GR-el-800-fe6827c9a77f.json (Wikidata Query Service) | 2026-10-24 | 181759 | `d656ff7e6fa067e0c6002ce878f93765e023dd1855ec4663dbdf0ee9bead3235` |
| wikidata/given-labels-HU-hu-0-c98c45fb10bd.json (Wikidata Query Service) | 2026-10-24 | 175533 | `1c736598bd71375405e22048d587e01adbd10b3726c2ded12bcfb81b475ddc9a` |
| wikidata/given-labels-HU-hu-0-fb01f7331863.json (Wikidata Query Service) | 2026-10-24 | 172812 | `d9dadc939bf6782fd70d82adb8f712c1238ea19a2e38474e557fbcb13e69e7a1` |
| wikidata/given-labels-HU-hu-1200-53d18682d023.json (Wikidata Query Service) | 2026-10-24 | 113684 | `82390ba2ee4d7ee2411495a5a5b88383c9fbcb4b7e4b3fd6cf6730cbe9ae2acf` |
| wikidata/given-labels-HU-hu-1200-fa9fca7820d7.json (Wikidata Query Service) | 2026-10-24 | 124899 | `6f0847a767fd88f7f1fb7b29c894989d496aa8a389c27e79559f77f56ad8024a` |
| wikidata/given-labels-HU-hu-400-b46a72795767.json (Wikidata Query Service) | 2026-10-24 | 168951 | `c25172b07ba9ec2a10ab43bd31780d369a3023e47f841361f254ffed6a56f2bf` |
| wikidata/given-labels-HU-hu-400-efb98a467771.json (Wikidata Query Service) | 2026-10-24 | 171670 | `4b8b9b8ba244b2fa774b204170687401eb02e0167d3ca8959628b1a86a67ed93` |
| wikidata/given-labels-HU-hu-800-48b498df2edd.json (Wikidata Query Service) | 2026-10-24 | 175262 | `d2f6bdd48270258dafa9ad34844d0600baa7d9374c3eba1488fdc4b06aa7cc53` |
| wikidata/given-labels-HU-hu-800-7ad72898500b.json (Wikidata Query Service) | 2026-10-24 | 177991 | `8fc224b29ce71d7ca1057323907e4fc72ddcb4d15563ebb2054e963628d73fdd` |
| wikidata/given-labels-IL-he-0-21e73171ed62.json (Wikidata Query Service) | 2026-10-24 | 188830 | `db50945304dbfb32c5af6a621a1e04e7e562827974a2e145b72f48dc00b7e454` |
| wikidata/given-labels-IL-he-0-a39f42d3321d.json (Wikidata Query Service) | 2026-10-24 | 190851 | `b6a07dbd240811c56672e8078e4f1ce793cfd51477115339db2d4bd3dbd7f009` |
| wikidata/given-labels-IL-he-1200-06667018b390.json (Wikidata Query Service) | 2026-10-24 | 119304 | `66141717ff2b4a960f32e63c2e95bf1aac8b11e669780578a23f3a6ec35aab7a` |
| wikidata/given-labels-IL-he-1200-9e4a4644de8b.json (Wikidata Query Service) | 2026-10-24 | 122041 | `8acc7f238325ebbf8ed2c070f876817b23776ed03c1d1713e424d91edc96156a` |
| wikidata/given-labels-IL-he-400-226d80c42cf8.json (Wikidata Query Service) | 2026-10-24 | 178265 | `8ad764e83b46334a740fce13d29e00435f2e4317e6891bd2767468b45aeb8c19` |
| wikidata/given-labels-IL-he-400-b51b33a84679.json (Wikidata Query Service) | 2026-10-24 | 177311 | `3a68107491998430cc113dde53cb4a6311291081fb54ebd15b63e450eef03036` |
| wikidata/given-labels-IL-he-800-18b98372446e.json (Wikidata Query Service) | 2026-10-24 | 191107 | `d9605c78ef4792fac25398709e219dcfba0876ea6e00ff98dd56afd914783d31` |
| wikidata/given-labels-IL-he-800-50772920a4bf.json (Wikidata Query Service) | 2026-10-24 | 192126 | `3d9b824893cfb0acabebcf8a06ac7b426d682ee57a393eacfd6a633a8b7669be` |
| wikidata/given-labels-IN-hi-0-a690ee0b6301.json (Wikidata Query Service) | 2026-10-24 | 145975 | `833ae84b14c1d02e43966cf8a4397ca14850f9ac9b1bb9ff30cbdb565efaa283` |
| wikidata/given-labels-IN-hi-0-c052198d0b8a.json (Wikidata Query Service) | 2026-10-24 | 147805 | `3b4762ba0732abb3f7c573d3fbe6d89602e107318a4b500d9e834de75caef1c4` |
| wikidata/given-labels-IN-hi-1200-3b05e309eebe.json (Wikidata Query Service) | 2026-10-24 | 92399 | `2e8b68aad8e73f51b7cf8fd8b0004c8fdebd765e2bd45cc3a3c441097761b649` |
| wikidata/given-labels-IN-hi-1200-e9d7d9e70f11.json (Wikidata Query Service) | 2026-10-24 | 90411 | `f04d6781abc17581887d64f0bee562c94ad0e48f45a0a725ab9d3c1459537992` |
| wikidata/given-labels-IN-hi-400-5bab3a7e5182.json (Wikidata Query Service) | 2026-10-24 | 143681 | `fc694f9a52ebe2643fad6af33480df2b81c93728b0caf5c0d349260089fc759e` |
| wikidata/given-labels-IN-hi-400-d8a6108c8290.json (Wikidata Query Service) | 2026-10-24 | 146691 | `e2d8fa994838165b4decda5089207ca8426cd3ae372789093f721b0ae65fbe71` |
| wikidata/given-labels-IN-hi-800-99e60f264806.json (Wikidata Query Service) | 2026-10-24 | 163841 | `93f252afd3b87d1bb4e9938a19808750ec67c4f7f7bf58a69c5ac8b54e050c01` |
| wikidata/given-labels-IN-hi-800-e1eae100cb2c.json (Wikidata Query Service) | 2026-10-24 | 163964 | `fcfb79904146eb7084e9cb392c85541fee39dcb414a81e9b7bccc4f336cd7997` |
| wikidata/given-labels-IT-it-0-d4d13a1b03d3.json (Wikidata Query Service) | 2026-10-24 | 176098 | `dd0ffe51a11b1aaaa40ec772e73fef1920bf3c3efe4c59c79ae5ed690c3c8b1d` |
| wikidata/given-labels-IT-it-0-ee9c643dccdb.json (Wikidata Query Service) | 2026-10-24 | 176340 | `477ceb8281b0c53ded4d1c714ab7814651a1559d7abdac6761ab83457279845e` |
| wikidata/given-labels-IT-it-1200-a149fe963ed5.json (Wikidata Query Service) | 2026-10-24 | 133042 | `8379d3874971abad6c069df7262bd0cbd7a52a12e67de374385a6f4d8608c933` |
| wikidata/given-labels-IT-it-1200-b08e8777b4e0.json (Wikidata Query Service) | 2026-10-24 | 133328 | `8db43a335dfecc2d87b80985736b815de233b7a5885cda6b5ea30b42207a5f18` |
| wikidata/given-labels-IT-it-400-32f01bbdf0e7.json (Wikidata Query Service) | 2026-10-24 | 173595 | `802704ac9e2146a7cb5f5b9a8f169350ad40ee5c05a9212cbaf90d3eaf7bb43c` |
| wikidata/given-labels-IT-it-400-8ea3ee98b775.json (Wikidata Query Service) | 2026-10-24 | 173437 | `7f75836636860a9281637179839a2c9a8a896ea3d50cbd7d5a0301f03a44c37c` |
| wikidata/given-labels-IT-it-800-a7a7f7f32977.json (Wikidata Query Service) | 2026-10-24 | 178010 | `8cdc582e13d73a197860b7a9c9a5140818d6f2113933592f764771b45d7778a8` |
| wikidata/given-labels-IT-it-800-a8722a1a67d6.json (Wikidata Query Service) | 2026-10-24 | 178465 | `3ceb6e62d8ef4fa55917a6c47826681afb4e13c110e1b385371c566ae34b194e` |
| wikidata/given-labels-JP-ja-0-0a6994752d3f.json (Wikidata Query Service) | 2026-10-24 | 149267 | `a02ffbda2b962f405a4a1ddac2e91c57a41fe683c173a24a8229abe8078659ed` |
| wikidata/given-labels-JP-ja-0-1abb87b02073.json (Wikidata Query Service) | 2026-10-24 | 190591 | `071a01b2055337dbad23b8db30948f2945191ad9246030a6cb5197fbff890c2c` |
| wikidata/given-labels-JP-ja-0-6d2f22ed0802.json (Wikidata Query Service) | 2026-10-24 | 233806 | `b4c3955ae59001c8eb3c938cbe173dc304ee491c815a6a292ab38294fc621537` |
| wikidata/given-labels-JP-ja-0-b15d9c97abab.json (Wikidata Query Service) | 2026-10-24 | 235637 | `8e2384f1a409c32d8453593074ebe6c01848b6282b376f5a0971f0f4ea1a6f5c` |
| wikidata/given-labels-JP-ja-1200-1f5339e72f4d.json (Wikidata Query Service) | 2026-10-24 | 117106 | `4f525518984867cbed22b2dd78e244713fbe25bb1ef756f848f56759fc4a0ae0` |
| wikidata/given-labels-JP-ja-1200-6a0e43a1a746.json (Wikidata Query Service) | 2026-10-24 | 90049 | `ea1b349444f0a68328f72735448e509c0a3324904b8280f2e955b62cf830b773` |
| wikidata/given-labels-JP-ja-1200-701e727838bd.json (Wikidata Query Service) | 2026-10-24 | 145344 | `5b7bfb1bcc168b1eab381e5663521d7daadd7580ea7375eab165af24960830ba` |
| wikidata/given-labels-JP-ja-1200-a27b580ad3d9.json (Wikidata Query Service) | 2026-10-24 | 143773 | `25733773355f7987d9097950e4e222e66a7bbb14993a23726bacde131e40a958` |
| wikidata/given-labels-JP-ja-400-33c9a25da5fc.json (Wikidata Query Service) | 2026-10-24 | 184017 | `d1d1465863bae1ebae690ebb2e0ac69b991745c30c2bd22cefba467d26afabc7` |
| wikidata/given-labels-JP-ja-400-3998d1415253.json (Wikidata Query Service) | 2026-10-24 | 231648 | `bff2ca14d1aa56e14c5496666fd9989032c088b4dbc8bc2625408790bbff8a84` |
| wikidata/given-labels-JP-ja-400-7be756b57506.json (Wikidata Query Service) | 2026-10-24 | 143675 | `78c4a176c053b8c87e2800680fa3ef424ba2dae58df22123758982f98a3523cf` |
| wikidata/given-labels-JP-ja-400-db04e85ac5f2.json (Wikidata Query Service) | 2026-10-24 | 230152 | `98c05a1b30909bfa67bbb1bbfb2895bef5f0f6acf1befa5a79e8c717c7c84062` |
| wikidata/given-labels-JP-ja-800-0a8507b59551.json (Wikidata Query Service) | 2026-10-24 | 181703 | `b5203748d4194122f2449f33b1133a7f0d76274af9e688a7679293562e20f78f` |
| wikidata/given-labels-JP-ja-800-5a875be831a0.json (Wikidata Query Service) | 2026-10-24 | 138587 | `a6b42f11a0a08abb8941fb12e9ba817e7f02919af351078400e679bbe31cd837` |
| wikidata/given-labels-JP-ja-800-8ae3a6423237.json (Wikidata Query Service) | 2026-10-24 | 226196 | `f75ab08f0bbc15687bc18679c03e745c2281f3ee2587cbcd7d7e47ef27f1cbef` |
| wikidata/given-labels-JP-ja-800-f83f1daca960.json (Wikidata Query Service) | 2026-10-24 | 227500 | `26653c338afc96bd54e56c3512965ca932e2cf59c8cc9540676b5aa0d71d01ce` |
| wikidata/given-labels-KR-ko-0-d2f6b2246cf0.json (Wikidata Query Service) | 2026-10-24 | 242437 | `f9ba4eba8e3119e66e01471d89e679e4ef38e62ba987c2f387213154cadfbd36` |
| wikidata/given-labels-KR-ko-0-d6deb1b35192.json (Wikidata Query Service) | 2026-10-24 | 236833 | `5ec5708d66fcbd3f07f03ba822873e83cfe2910004428ed796bcff729cdf3f32` |
| wikidata/given-labels-KR-ko-1200-71a64481c4c3.json (Wikidata Query Service) | 2026-10-24 | 122561 | `99317076fa1eb64d52d71346c091b44a66e4004650f47b1ae550eac52f6975e8` |
| wikidata/given-labels-KR-ko-1200-b7f1383819d5.json (Wikidata Query Service) | 2026-10-24 | 128066 | `b0f79b870f3ffcbf7f6d73cb6a3a1e9f0b37dfa38796e0a31a451f0b6c8f06ba` |
| wikidata/given-labels-KR-ko-400-557faa6abd2e.json (Wikidata Query Service) | 2026-10-24 | 220713 | `071cd48b1253ef30d1cc6c63b8e53dcaab5f4fc4d30960b1b5913495f2076946` |
| wikidata/given-labels-KR-ko-400-dff0563c9096.json (Wikidata Query Service) | 2026-10-24 | 220600 | `d22543ff383e93b2d4be20e873b8ced3a923b0d1e936bf1fda40273ab397a790` |
| wikidata/given-labels-KR-ko-800-24e33bbca8da.json (Wikidata Query Service) | 2026-10-24 | 220459 | `63a9d55e1539df701e91dea3bd350ece04e11fa195ec980b28ee541426f7fee7` |
| wikidata/given-labels-KR-ko-800-4afb9c52c590.json (Wikidata Query Service) | 2026-10-24 | 220461 | `77d8d6a08fa49044ac9b401380ecce97ea3542cb4b6a75feac0b15c0e898ef2e` |
| wikidata/given-labels-NL-nl-0-1a3c50334177.json (Wikidata Query Service) | 2026-10-24 | 168403 | `5e22b22a1c8f8dfcce53c0e070dc8c281aa74d45d77acebdd0e3987b44440310` |
| wikidata/given-labels-NL-nl-0-28292f9e6381.json (Wikidata Query Service) | 2026-10-24 | 124512 | `a82f91bf805d476a54b7cb14293adcb60d17207ef4f36aec479a503fee8cf7ec` |
| wikidata/given-labels-NL-nl-0-a0839e6c4949.json (Wikidata Query Service) | 2026-10-24 | 165366 | `fcebaff0ccf85f6c36b7951dda37537a7ce73b15bbc23dd04a774c3dc2d9b0d9` |
| wikidata/given-labels-NL-nl-1200-77830ae09b64.json (Wikidata Query Service) | 2026-10-24 | 119788 | `22839c9b292887cd39a2ded72cb86d5da7da45e71028bd3d68886a09dd7f9fc3` |
| wikidata/given-labels-NL-nl-400-617d8fe0ab27.json (Wikidata Query Service) | 2026-10-24 | 84278 | `b0f268e973f40649937a834c96f3c05b79246ed218ad765d27d23e5a8be07874` |
| wikidata/given-labels-NL-nl-400-785b9282a827.json (Wikidata Query Service) | 2026-10-24 | 169943 | `0f51396e69ade961b762c4e565de92b0e676998adf6e801da73ba280d5eb9d0a` |
| wikidata/given-labels-NL-nl-400-fb09f5d08176.json (Wikidata Query Service) | 2026-10-24 | 111165 | `2c4d5b3c43292bd7e4abf42dd8e5a3d39070c216afddf2bc6a9136ac40d02fe1` |
| wikidata/given-labels-NL-nl-800-0f681f608cd4.json (Wikidata Query Service) | 2026-10-24 | 173614 | `0c61b932e6509f06023588584bdecf3482234e3173da528853da160edad395bf` |
| wikidata/given-labels-PL-pl-0-3b42b1520907.json (Wikidata Query Service) | 2026-10-24 | 182193 | `dbe37fff83ea362fc161a51bd3377d3f3bc70d8ac6dc4ba8df3804f28e50bbed` |
| wikidata/given-labels-PL-pl-0-a88b78d9485c.json (Wikidata Query Service) | 2026-10-24 | 187810 | `70f678c2f51b24c002b5e7bc54e495f0e140b63499cb34156fd59a52b8442800` |
| wikidata/given-labels-PL-pl-1200-64c8e3435780.json (Wikidata Query Service) | 2026-10-24 | 125127 | `213a4c069074bc85f9c1737448f454d4cbbb87a0fdbbd940ae0341a1ba4f4587` |
| wikidata/given-labels-PL-pl-1200-7017954c08b8.json (Wikidata Query Service) | 2026-10-24 | 126232 | `9950e1bed78561b678c3ecb8bcceaa0cb1cd2ef1a704521e725ea09f68546a24` |
| wikidata/given-labels-PL-pl-400-6c4a4294b6cc.json (Wikidata Query Service) | 2026-10-24 | 187408 | `16b31332e40728e06bbb10852ba831f3ddc317f3537b729e22b20c745c20679c` |
| wikidata/given-labels-PL-pl-400-bc53691c4074.json (Wikidata Query Service) | 2026-10-24 | 189007 | `140d87503206bc9104464497ba30336e3b81b4713fdb74785e01c4955228fffd` |
| wikidata/given-labels-PL-pl-800-533b0f2d3922.json (Wikidata Query Service) | 2026-10-24 | 185474 | `47a23219b0b4f98cddfb0c3ad45873cbf6f700e485aa0069c8f7cf9e57d93eb9` |
| wikidata/given-labels-PL-pl-800-6e6ba585a19f.json (Wikidata Query Service) | 2026-10-24 | 188349 | `40a3a04d37860ce16258d0b066ec30dccfdf974897833f8f5b8bbac76c253fb0` |
| wikidata/given-labels-RS-sr-0-212f38180404.json (Wikidata Query Service) | 2026-10-24 | 170477 | `a436d99ac8e3c78fe1cb930aa0428bfeb5be93e710e34b2f90d7d84f8b1fe385` |
| wikidata/given-labels-RS-sr-0-c0e6eb531631.json (Wikidata Query Service) | 2026-10-24 | 167453 | `a1fcd92c6a73e3192e19ddd98800d199ad78a5f6435d72606c714a9544f569b2` |
| wikidata/given-labels-RS-sr-1200-d6ae8d52be61.json (Wikidata Query Service) | 2026-10-24 | 6392 | `7bfdf6b0e448511bb6540b9d23e0fe3a96da3449e222d9e9be475e3863caf7a7` |
| wikidata/given-labels-RS-sr-400-25c8040d50d1.json (Wikidata Query Service) | 2026-10-24 | 162502 | `7d635e442d80f6a9af1dd9dd7124054fbf23c301c591f5938089950e9b5953ab` |
| wikidata/given-labels-RS-sr-400-98f14c4f3720.json (Wikidata Query Service) | 2026-10-24 | 163872 | `460bdbaa228d84f3cb18607edc09b7cc2229dcf71faf80d2199760a6786fef64` |
| wikidata/given-labels-RS-sr-800-0e7d1707deb1.json (Wikidata Query Service) | 2026-10-24 | 168811 | `3bb666adfd4d95d9a5512fe6d982c3a3f58d6414c5fc7a6f49f099840ce12df3` |
| wikidata/given-labels-RS-sr-800-44df1a4ad0db.json (Wikidata Query Service) | 2026-10-24 | 125726 | `e0cd5f3ed36426ad71c35c5647b677935402e5feaa0753af7dc40d93da3c0a06` |
| wikidata/given-labels-RU-ru-0-19d69297007e.json (Wikidata Query Service) | 2026-10-24 | 182834 | `a1caef04ad50a0fad519987697d3087781c5b37cd5bfdc0a8a02bc8ffb33d056` |
| wikidata/given-labels-RU-ru-0-74f5de2dcbe3.json (Wikidata Query Service) | 2026-10-24 | 130125 | `c5d650cfb34cec93f7bee7103810408e62f5f6c40b99fe7235d684b6ffefa20c` |
| wikidata/given-labels-RU-ru-0-bbd08d2defd3.json (Wikidata Query Service) | 2026-10-24 | 125341 | `49e568124e0dd7ac7c75cd4c0561c82f050e7f84be11a4120ddfa1f08dc906ac` |
| wikidata/given-labels-RU-ru-0-da52f4436ead.json (Wikidata Query Service) | 2026-10-24 | 182416 | `dd55c4bf7345c069c377a193220d687ecf530904736d1452b562afb15f4f114f` |
| wikidata/given-labels-RU-ru-1200-2cb47a6d1e7c.json (Wikidata Query Service) | 2026-10-24 | 91530 | `46080700a5dfddb33ecef0889267e84557f98cd892753821e5667360c0626a96` |
| wikidata/given-labels-RU-ru-1200-4ab7c266f250.json (Wikidata Query Service) | 2026-10-24 | 96552 | `2d50ae2aaf7ad4c04ee1a1c0327e090e620b06faa4d7af48eff522331e3a1c8a` |
| wikidata/given-labels-RU-ru-1200-64eb596cc37f.json (Wikidata Query Service) | 2026-10-24 | 134676 | `af580b64c003e8482630795a5bdabec02f87ef1a8dde420d49d3e003f701abaf` |
| wikidata/given-labels-RU-ru-1200-b17949002cba.json (Wikidata Query Service) | 2026-10-24 | 131680 | `8447eaafec7a20f02ca7a3bac3b2e781e936ecd4e54b7121f9122eed1cd98d98` |
| wikidata/given-labels-RU-ru-400-4ef0c5413ef1.json (Wikidata Query Service) | 2026-10-24 | 131761 | `2030fe2dcb305a82b42b67d06aff003ffd31a7be51c81b36fc5d05f1d8216d40` |
| wikidata/given-labels-RU-ru-400-93a9c43eb225.json (Wikidata Query Service) | 2026-10-24 | 181873 | `ac530e214312da39fb4c28c9f7544a369448a8560f916369117a5abcf5cba897` |
| wikidata/given-labels-RU-ru-400-a3441f31a9ee.json (Wikidata Query Service) | 2026-10-24 | 182581 | `cb5f6aaa4c8a3d22dfa9cf5f70388981a7a3b286cd58df99ebb07d66aafd4ea6` |
| wikidata/given-labels-RU-ru-400-e83fcc443260.json (Wikidata Query Service) | 2026-10-24 | 130003 | `e8f2767e393d0318dc64ea4c5c0ad4c20f781843ce9a0e8eea37f67436b06ff5` |
| wikidata/given-labels-RU-ru-800-a8611ca4c320.json (Wikidata Query Service) | 2026-10-24 | 184818 | `7fefb78e0b778d96cc6f2c4150d18d8379dcf6862dd250494533b22c2b57b8b3` |
| wikidata/given-labels-RU-ru-800-dccc76508463.json (Wikidata Query Service) | 2026-10-24 | 137027 | `4a3522386144b9ee0b561af1cc97f38d61efe730592fe3a51963f4b124fdcc0b` |
| wikidata/given-labels-RU-ru-800-defe2a662761.json (Wikidata Query Service) | 2026-10-24 | 185322 | `a897d2b8064ef962e0a9ab325a4f0e803b3561d8cd23399076e6df5dc7d44324` |
| wikidata/given-labels-RU-ru-800-eba0a102a36f.json (Wikidata Query Service) | 2026-10-24 | 130967 | `78b7e8f8492dc96cb7f9ae765704803189a1ef0d22e562aa224b2b1632b871f6` |
| wikidata/given-labels-TH-th-0-2a0f69e78fc7.json (Wikidata Query Service) | 2026-10-24 | 159272 | `f75e6f6f98893ffdeb1728615b67497ddf67de3eebb58ee5f3d28977eedbe50c` |
| wikidata/given-labels-TH-th-0-31d30d64a7aa.json (Wikidata Query Service) | 2026-10-25 | 160592 | `a8fd1f5c11ad66a3498ec7c92e8af00992a8b497db673bb5a1e77a44d30a5df2` |
| wikidata/given-labels-TH-th-0-c9b891430754.json (Wikidata Query Service) | 2026-10-24 | 92421 | `f20da4cf40f5a2eb0959f5580cb5bf23db3c8354d7568145f0289897ad8da362` |
| wikidata/given-labels-TH-th-0-da053ee55011.json (Wikidata Query Service) | 2026-10-24 | 160239 | `a1738a0aef1ad460f698294e7dd837dd8f8932d95e78f7c44713c06102bd4622` |
| wikidata/given-labels-TH-th-0-e850d9c4650c.json (Wikidata Query Service) | 2026-10-24 | 116436 | `cb6dc31f44568b900d7a90164c67907d5fa16f0e484787ddfab83f25e233ccb0` |
| wikidata/given-labels-TH-th-400-37675ad57a37.json (Wikidata Query Service) | 2026-10-24 | 138055 | `ae021f858308070c34e74abab54dcd6ca8ddc0bab8e59834216885e00d990a75` |
| wikidata/given-labels-TH-th-400-76d4afe9a18b.json (Wikidata Query Service) | 2026-10-25 | 138688 | `dc30e2148974197428b4faab0695f880043afbc21b002db6c25872ed0b6d0212` |
| wikidata/given-labels-TH-th-400-7c0298269678.json (Wikidata Query Service) | 2026-10-24 | 137846 | `b68f09c1c6e667a53ef0f2b0e2c1835d62f93768be3f189ea00a89e9ae3f7e9e` |
| wikidata/given-labels-TH-th-400-cd35b625fdb2.json (Wikidata Query Service) | 2026-10-24 | 91417 | `1c623e552f004f0a675eaf4912f3f78be4f9c6201ce62cfebd6ea497b454903b` |
| wikidata/given-labels-TH-th-400-e3316e82db16.json (Wikidata Query Service) | 2026-10-24 | 95647 | `e0a54051c12f374f964d355fc09cae6b630f6c653a3474fda0f4a2821d53cb4f` |
| wikidata/given-labels-TH-th-800-159ee1db5be1.json (Wikidata Query Service) | 2026-10-24 | 135510 | `6480d68ba38ebbf77cd1fb831a34276f97117ebb079e4cb90e074dfff9b9cd28` |
| wikidata/given-labels-TH-th-800-cf7fe2ca53c3.json (Wikidata Query Service) | 2026-10-24 | 92526 | `eaf0fedcb8ec8f6597e02ee6780b046da8d55f5ddf5a89b5f1e96b85e06455d5` |
| wikidata/given-labels-TH-th-800-eaf54ad692a0.json (Wikidata Query Service) | 2026-10-24 | 90662 | `acd2ac43498845f7c9f5896cca8530085e2274b41c9e894727f5366a148eb6a6` |
| wikidata/given-labels-TH-th-800-fb824d923e01.json (Wikidata Query Service) | 2026-10-24 | 111987 | `cf083b6ab961d42acc31401d0bdb95263ed3b8a8601120c8e8c625418f582d58` |
| wikidata/given-labels-TH-th-800-fc5b94a60994.json (Wikidata Query Service) | 2026-10-25 | 118484 | `c3242ff489a6e8180557336878aeef72d280e06a33c4d430b8ba470503cb3d98` |
| wikidata/given-labels-TR-tr-0-d33e8cfe0e90.json (Wikidata Query Service) | 2026-10-24 | 205558 | `b5e1ed7efa117cd39620f26ea74e67136297bd57783e2e21690b61225049c134` |
| wikidata/given-labels-TR-tr-0-f9cbc6ec6320.json (Wikidata Query Service) | 2026-10-24 | 205630 | `abf593b7e3f94ac5fc4c168ae353596f285f6e32fffe787ed9d30f889ae7d045` |
| wikidata/given-labels-TR-tr-1200-032bc23b0291.json (Wikidata Query Service) | 2026-10-24 | 121059 | `900b2f324740e2f26a4fe403ef364cd21091a2c5fda41b7bbbcd8b010c579fb1` |
| wikidata/given-labels-TR-tr-1200-53e018fceab4.json (Wikidata Query Service) | 2026-10-24 | 122938 | `0d5169e5573acf84a3b3d9e32110a90bc6323dbc1d7eb7dc1b5ec7a8088baa32` |
| wikidata/given-labels-TR-tr-400-888aeb8a924a.json (Wikidata Query Service) | 2026-10-24 | 205519 | `ddcf15b3a0e298cddfc3fbe747d7f01a69f11afad246f3b4f65b55ef6a317ad2` |
| wikidata/given-labels-TR-tr-400-c3881f9d714c.json (Wikidata Query Service) | 2026-10-24 | 206530 | `9994c9821dec56f05ef3805459248fbc131bb3ed3212602c7c30ef290ca80590` |
| wikidata/given-labels-TR-tr-800-072a92999bb1.json (Wikidata Query Service) | 2026-10-24 | 215812 | `583010ece4fe55003848a84f9524a8e06e67dbb9960367a9bc16e1c52fcdd7b7` |
| wikidata/given-labels-TR-tr-800-8882a2ba2616.json (Wikidata Query Service) | 2026-10-24 | 216838 | `911d0491edf0760e48990e53373e4382e232852436305674e2528498b1d02785` |
| wikidata/given-labels-TW-zh-hant-0-957d9c309a41.json (Wikidata Query Service) | 2026-10-24 | 162653 | `38ba5f66881d47ae3ff66770a99ef9a39d7dd79432501eee18febf85d7e2444a` |
| wikidata/given-labels-TW-zh-hant-0-f3ebd88bc906.json (Wikidata Query Service) | 2026-10-24 | 161256 | `fabf72cca02284acfb2a8bfffcc31396ca86ec00ed6fb309ce7000a971313366` |
| wikidata/given-labels-TW-zh-hant-1200-737d90f2c593.json (Wikidata Query Service) | 2026-10-24 | 111354 | `3d18e30da85e6a61db1fd5325fc31ad5b94909da05d265e2dd360c1f29afb19e` |
| wikidata/given-labels-TW-zh-hant-1200-c59a151f7b58.json (Wikidata Query Service) | 2026-10-24 | 111087 | `a16fa1a5575c594d533f5487421ecaac474b6228ad27ae3527c882605f66ff2f` |
| wikidata/given-labels-TW-zh-hant-400-885e07f8bb2f.json (Wikidata Query Service) | 2026-10-24 | 165635 | `6edc4cd381b372efcf79d1c521f1d5653aeaffbc6e8dda41779aff4d0b91a86e` |
| wikidata/given-labels-TW-zh-hant-400-a5d19b4604a7.json (Wikidata Query Service) | 2026-10-24 | 166821 | `5d5cbf6d2a4bf2c2c5afdc5aa97d5645fd3e3dc398e6af80b25fb6f6d153ffcf` |
| wikidata/given-labels-TW-zh-hant-800-1dbe981e6194.json (Wikidata Query Service) | 2026-10-24 | 173634 | `58a50cd9751d4a3b7f649b319c3f50b33d997cc9ed08db3520110158d1d445a1` |
| wikidata/given-labels-TW-zh-hant-800-89a43701f3d3.json (Wikidata Query Service) | 2026-10-24 | 172915 | `a2bbf71ba02f29ab7b28e93fd0dfa17a2c24ec955c259ce5d5b8c1488ab7b0c6` |
| wikidata/given-labels-UA-uk-0-47ce1cfa79eb.json (Wikidata Query Service) | 2026-10-24 | 179370 | `5466a8fcb042728f768272af2592a90eff891d97455985f3b884c785e65cb779` |
| wikidata/given-labels-UA-uk-0-e2cffa8c8a2a.json (Wikidata Query Service) | 2026-10-24 | 175576 | `d4530b3f74c2f063873e7833223914e21f149148395d3cd1ddb47baa70a36343` |
| wikidata/given-labels-UA-uk-1200-31eb4d540a79.json (Wikidata Query Service) | 2026-10-24 | 93252 | `64430c2f768117be3bb1c8b11113a4b64b54bfab1a2f4ba415703d9fbcc4e00b` |
| wikidata/given-labels-UA-uk-1200-ddb12355763d.json (Wikidata Query Service) | 2026-10-24 | 121645 | `d269a506f83cb7e7e9e2b3ca0e3e8d2cd1d9faea3879170cbd5792d4ad840c4a` |
| wikidata/given-labels-UA-uk-400-12c4ca8f507f.json (Wikidata Query Service) | 2026-10-24 | 167840 | `563265db0e513d854635280e2667c162f0fa01115052883c7a7b6036aa74f3be` |
| wikidata/given-labels-UA-uk-400-6623857c9e57.json (Wikidata Query Service) | 2026-10-24 | 167123 | `0783a76665010150c805910185c64a51dec4c950029135b96fe1dd0e4d06178f` |
| wikidata/given-labels-UA-uk-800-3ba077220cc2.json (Wikidata Query Service) | 2026-10-24 | 173308 | `cec3ca4b14a696a342ee7344e1c2cb5d816cf58f302b0ba8d7a91740fe58b30d` |
| wikidata/given-labels-UA-uk-800-f265fa8e9036.json (Wikidata Query Service) | 2026-10-24 | 175520 | `a2c3088e082d32aa19d2954507177c5009d5a81f06fc67524639293f24e07cdc` |
| wikidata/given-labels-VN-vi-0-c4a16ec7fa80.json (Wikidata Query Service) | 2026-10-24 | 177927 | `3c2727a75ef087d48480c7325cd8b8350f59180c8d1bba8b5736f39d78037f8a` |
| wikidata/given-labels-VN-vi-0-f1d8ca4e1293.json (Wikidata Query Service) | 2026-10-24 | 177302 | `23f0fe73c1bf896c7ebaeef2821234d8fc7ad065307e78af1171e89f5a216fe7` |
| wikidata/given-labels-VN-vi-400-7357eab0674b.json (Wikidata Query Service) | 2026-10-24 | 6815 | `6ffcd06db0c6f4b9696c01d20ee2aedaa0b5f304bed6d89a076f0ceaa6e58e10` |
| wikidata/given-labels-VN-vi-400-ec162669901e.json (Wikidata Query Service) | 2026-10-24 | 44253 | `28a5761fafb3ed58ce4ad8aa9db27dc1097151fd4fea5bf5fbf2410cd101071c` |

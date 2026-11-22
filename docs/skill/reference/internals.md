# internals: languages, seeds, the clock, the data and the build

Package `dev.taihei.mihon`.

## Languages and their resolution

`Mihon.languages` lists the languages with data. Each has a default country (CLDR's likely
region), whose addresses, cities and phone numbers it uses.

The 25 languages and their countries: `ar` (Egypt), `bg` (Bulgaria), `de` (Germany), `el`
(Greece), `en` (United States), `es` (Spain), `fr` (France), `he` (Israel), `hi` (India), `hu`
(Hungary), `it` (Italy), `ja` (Japan), `ko` (South Korea), `la` (Vatican City), `nl`
(Netherlands), `pl` (Poland), `pt` (Brazil), `ru` (Russia), `sr-Cyrl` (Serbia), `th` (Thailand),
`tr` (Turkey), `uk` (Ukraine), `vi` (Vietnam), `zh-Hans` (China), `zh-Hant` (Taiwan).

A language is looked up by its canonical tag, then as an alias (`zh`, `zh-CN`, `zh-SG` to
`zh-Hans`; `zh-TW`, `zh-HK`, `zh-MO` to `zh-Hant`; `sr`, `sr-RS` to `sr-Cyrl`), then with subtags
stripped from the right (RFC 4647 lookup). The first tag of that chain that is a language or an
alias decides, so an alias of a language without data refuses rather than fall back to its base
tag. A tag that names a script other than the found language's is refused, so `sr-Latn` is not
served by Cyrillic. A later minor release may serve a tag refused today, or serve it by a closer
language. A refusal is an
`IllegalArgumentException` naming the tag (printable ASCII) and the languages there are.
`mihon.language` is the resolved language.

## Randomness and seeds

A `Mihon` draws every value from the `java.util.Random` it was created with; there is no
global generator.

- `Mihon.seeded(seed)` uses `new java.util.Random(seed)`. Every draw goes through the methods
  whose algorithm `java.util.Random` specifies and Scala.js implements alike (`nextInt`,
  `nextInt(n)`, `nextLong`, `nextBoolean`, `nextDouble`), weighted draws included (one
  `nextInt(total)` and a binary search over integer weights), so a seed gives the same values on
  the JVM and on Scala.js. `SeededSpec` pins a run of seed 42 in every language and runs on both.
- The values for a seed hold for one Mihon version, language and clock. Any other version, a
  patch included, may change them; the release notes say so. Tsuji is not part of the promise:
  Mihon formats nothing through it.
- `withLanguage` views share one generator.
- `Mihon.secure()` uses a `java.security.SecureRandom`; values do not repeat.
- One instance is thread-safe, but threads drawing from it in turn make it non-reproducible.

## The clock

Both factories take a `java.time.Clock`, `Clock.systemUTC()` by default. `today` is
`LocalDate.now(clock)`, the clock's date in the clock's zone; `yesterday`, `tomorrow`,
`futureDate` and `pastDate` are relative to it. In a test, pass `Clock.fixed(...)`.

## The language data

JSON under `shared/resources/dev/taihei/mihon/data/`: `languages.json` (tags, scripts, default
countries, aliases) and per language `text.json` (separators, punctuation, quotation marks, the
capitals of the initials, words with integer weights), `names.json` (name order and separator,
female and male given names, family names with their female form, each with `latin`, `ascii`
and, in Japanese, `reading`), `countries.json` (country names) and `country-<CC>.json` (address
lines, postal code, cities, streets and phone pattern). Runtime code does no case mapping,
transliteration or locale lookup: everything a value needs is in the data, so the platforms
cannot differ.

The sources and their licences are in `docs/data-sources.md`; the scripts that build the data
are in `scripts/data/`, never run by the build.

| Platform | How the data is provided |
| --- | --- |
| **JVM** (`jvm/src`) | `ResourceLoader` reads the files from the classpath |
| **Scala.js** (`js/src`) | the build generates `EmbeddedData` from the same files (`EmbeddedData` in `build.mill`), each file a few string literals of at most 60 000 bytes |

The build checks every file against the schema the library decodes and refuses anything else in
`shared/resources` (other files, folders, links), so the library's own decoding cannot fail at run
time. `DataSpec` compares every loaded file with the one on disk on both platforms. `make budget`
(part of `make validate`) holds the data to 80 KB per language and 2.5 MB in all, the JVM jar to
1.2 MB, and the full-linked Scala.js of a fixture that uses every language to 6 MB.

`ResourceLoader`, `SampleWordCollection`, `dev.taihei.mihon.data` and `dev.taihei.mihon.sources`
are `private[mihon]`. Use `Mihon`.

## Build and layout

```
shared/src/dev/taihei/mihon/                Mihon, Person, Address, Gender, SampleImages
shared/src/dev/taihei/mihon/data/           the data model, its decoding and resolution (internal)
shared/src/dev/taihei/mihon/sources/        domain names, MIME types, file extensions (internal)
shared/resources/dev/taihei/mihon/data/     the language data
jvm/src/dev/taihei/mihon/sources/           ResourceLoader (classpath)
js/src/dev/taihei/mihon/sources/            ResourceLoader (generated EmbeddedData)
shared/test/src/dev/taihei/mihon/           MihonSpec, LanguagesSpec, SeededSpec, DataSpec (both platforms)
budget/src/                                 the size budget's Scala.js fixture, never published
scripts/data/                               the data scripts
```

- `make compile` / `make test` cover both platforms (`./mill __.compile`,
  `./mill __.test.testForked`; JS tests run on Node).
- Single platform: `./mill mihon.jvm.test` or `./mill mihon.js.test`.
- The JVM modules compile against the Java 17 API (`-java-output-version:17`); CI runs the JVM
  tests on Java 17 too.
- LICENSE, NOTICE and LICENSE-UNICODE are in `META-INF/` of every published jar that carries
  code or data: the JVM and Scala.js jars and their sources jars. The javadoc jars hold neither.
- Released from the maintainer's signed git tag by the family's release workflow
  (`.github/workflows/release.yml`), the same as every Taihei Framework library. MiMa compares
  with the newest release of the current major that has an immutable GitHub Release; 5.0.0 is
  the first under this name.

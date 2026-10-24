# Release 5.0.0

**Release Date:** 2026-10-24

Taihei Mihon 5.0.0 is the first release under the name Taihei Mihon and a major release. It is the
successor of `dev.taihei::taihei-lorem` 4.x: the artifact is now `dev.taihei::taihei-mihon` and the
package `dev.taihei.mihon`. Mihon (見本) means sample or specimen. It generates fake data (text,
people, addresses, phone numbers, dates, files, ids and sample images) in 25 languages, in the
script and conventions of each, on the JVM and on Scala.js, and from a seed when you want the same
values on every run.

The 4.x line keeps its name, `dev.taihei::taihei-lorem`, and receives security fixes for 12 months
after this release. There is no relocation from the old coordinates: the two lines are different
artifacts in different packages and can sit side by side on one classpath.

## Highlights

- **A generator bound to one language.** The global object `LoremIpsum` became the class `Mihon`,
  created by `Mihon.secure(language, clock)` or `Mihon.seeded(seed, language, clock)`. English is
  the default; Lorem Ipsum is now the text of Latin, `Language("la")`.
- **25 languages and their scripts**, each with its own words, names, name order, punctuation,
  addresses and phone numbers of its default country.
- **Reproducible data.** A seeded generator gives the same values in the same order on the JVM and
  on Scala.js, and `today` and the relative dates follow the generator's `java.time.Clock`.
- **People and addresses as values.** `person` returns a `Person` (given and family name, `Gender`,
  full name in the language's order, a romanised form, a kana reading in Japanese), `address` an
  `Address` (lines in the country's format, street, city, region, postal code, country).
- **Data with a source and a licence.** Every name, place and word comes from a source recorded in
  `docs/data-sources.md`, and the attributions travel inside the jars.

## The Mihon API

The public API is `Mihon`, `Person`, `Address`, `Gender` and `SampleImages`; everything else is
internal.

```scala
import dev.taihei.mihon.Mihon
import dev.taihei.tsuji.Language

val mihon = Mihon.secure()                    // English, values that do not repeat
mihon.name                                    // such as "Linda Robinson"
mihon.address.singleLine                      // such as "180 Cedar Lane, Houston, Texas 77233, United States"

val ja = Mihon.seeded(1L, Language("ja"))     // the same values on every run and platform
ja.person                                     // 田中さちこ, latin "Tanaka Sachiko", reading タナカ サチコ
```

- `Mihon.seeded(seed, language = Language.English, clock = Clock.systemUTC())` draws from a
  `java.util.Random` with that seed; `Mihon.secure(language, clock)` from a
  `java.security.SecureRandom`. There is no global instance.
- `language` is the language whose data the generator uses. `withLanguage(language)` gives a view
  in another language that shares the same random generator and clock. `anyLanguage` draws one of
  `Mihon.languages`.
- `person` and `person(gender)` return a `Person`; `name`, `givenName` and `familyName` draw one
  part. `email` is built from the romanised name, in ASCII, at a domain under the reserved
  `.example` top-level domain.
- `address` returns an `Address`; `street`, `city`, `postalCode`, `country` and `phone` draw one
  part, from the language's default country. Only the United States reserves numbers for fiction
  (555-0100 to 555-0199), and only there is `phone` a fictional number: elsewhere a number may be
  real, so never call or text one.
- `Person` and `Address` are values with structural equality: two equal in every member are
  equal, so one seed gives equal people and addresses. Their `toString` is the full name or the
  single line, for display.
- Text (`word`, `words`, `sentence`, `sentences`, `paragraph`, `paragraphs`, `htmlParagraphs`,
  `title`, `slug`), dates (`localDate`, `today`, `yesterday`, `tomorrow`, `futureDate`,
  `pastDate`), files and the web (`url`, `fileExtension`, `fileExtensionWithoutDot`, `mimeType`),
  ids and primitives (`id`, `character`, `password`, `bool`, `int`, `long`, `double`, `times`) and
  `SampleImages` complete it. Mihon formats no date: format a `LocalDate` with Taihei Tsuji.

### Resolution and refusal

A factory or `withLanguage` looks a language up by its tag, then by an alias, then with subtags
stripped from the right:

- `zh`, `zh-CN` and `zh-SG` are `zh-Hans`; `zh-TW`, `zh-HK` and `zh-MO` are `zh-Hant`; `sr` and
  `sr-RS` are `sr-Cyrl`;
- `en-GB` is served by `en`, `nl-BE` by `nl`, `pt-PT` by `pt` (with Brazilian addresses), and
  `zh-Hant-CN` by `zh-Hant`.

A tag that names another script than the one found is refused (`sr-Latn`, `sr-Latn-RS`,
`ru-Latn`), and so is a language without data. Both throw an `IllegalArgumentException` that names
the tag and the languages there are. For a language that comes from outside, ask first:

```scala
val mihon = Mihon.secure(Mihon.resolve(requested).getOrElse(Language.English))
```

A later minor release may serve a tag that is refused today, or serve it by a closer language
(`nl-BE` once it has its own data). Pin the Mihon version to rely on `language`.

## The 25 languages

| Tag | Language | Script | Default country |
| --- | --- | --- | --- |
| `ar` | Arabic | Arabic, right to left | Egypt |
| `bg` | Bulgarian | Cyrillic | Bulgaria |
| `de` | German | Latin | Germany |
| `el` | Greek | Greek | Greece |
| `en` | English | Latin | United States |
| `es` | Spanish | Latin | Spain |
| `fr` | French | Latin | France |
| `he` | Hebrew | Hebrew, right to left | Israel |
| `hi` | Hindi | Devanagari | India |
| `hu` | Hungarian | Latin | Hungary |
| `it` | Italian | Latin | Italy |
| `ja` | Japanese | kanji and kana | Japan |
| `ko` | Korean | Hangul | South Korea |
| `la` | Latin | Latin | Vatican City |
| `nl` | Dutch | Latin | Netherlands |
| `pl` | Polish | Latin | Poland |
| `pt` | Portuguese | Latin | Brazil |
| `ru` | Russian | Cyrillic | Russia |
| `sr-Cyrl` | Serbian | Cyrillic | Serbia |
| `th` | Thai | Thai | Thailand |
| `tr` | Turkish | Latin | Turkey |
| `uk` | Ukrainian | Cyrillic | Ukraine |
| `vi` | Vietnamese | Latin | Vietnam |
| `zh-Hans` | Chinese (Simplified) | Han | China |
| `zh-Hant` | Chinese (Traditional) | Han | Taiwan |

Each language follows its own conventions: name order (family name first in Hungarian, Japanese,
Korean, Vietnamese and Chinese), female forms of family names (Bulgarian, Greek, Polish, Russian),
quotation marks and question marks, no spaces in Japanese, Chinese and within a Thai sentence,
capitalised nouns in German, `IJ` in Dutch and `İ` in Turkish. Digits are ASCII in every language.
The words of a text are drawn by how often the language uses them, so the text has the language's
script and rhythm, but it means nothing: it is filler, as Lorem Ipsum is.

Known limits of this release: Thai has 92 female given names, below the floor of 100 the other
languages meet; Vietnamese has 26 cities, Traditional Chinese 47 and Turkish 74; Japanese given
names are in hiragana; the romanised form of Traditional Chinese is pinyin. Six languages have no
public list of offensive words to filter their text against: Vietnamese, Ukrainian, Bulgarian,
Serbian, Greek and Hebrew.

## Bundled third-party data and its licences

The language data in the jars (`dev/taihei/mihon/data/`) was derived by the scripts in
`scripts/data/` from these sources:

- **Unicode CLDR 47** (country names, quotation marks, the default country of each language) and
  the **Unicode Han Database, Unihan 16.0.0** (pinyin of Chinese family names), under the Unicode
  License v3, whose text is in `LICENSE-UNICODE`.
- **The Tatoeba Project**: word frequencies counted from its sentences, under CC BY 2.0 FR. Only
  single words, filtered, with weights from their counts are included; no sentence is reproduced.
- **Wikidata**: given names, family names, cities, regions and Latin country names, under CC0 1.0.
  Only names and places are taken, no data about any person.
- **The U.S. Census Bureau**: United States given names (1990) and surnames (2010), a work of the
  United States Government.
- **Google libaddressinput**: the address formats and postal code patterns were written for this
  library after its address metadata, under CC BY 4.0, adapted and changed.
- **Lorem Ipsum**, after Cicero, in the public domain, and classical Roman names written for this
  library.

`NOTICE` carries the attributions and `docs/data-sources.md` the versions, download dates,
SHA-256 sums and processing of every source. `LICENSE`, `NOTICE` and `LICENSE-UNICODE` are inside
the JVM and Scala.js jars and their sources jars, under `META-INF/`. The name, city and country
lists of taihei-lorem 4.x had no recorded source and are gone.

## Seeds and stability

The same seed, language, clock and Mihon version give the same values in the same order, on the
JVM and on Scala.js. Any other Mihon version, a patch included, may give other values, and its
release notes say so whenever it does. Pin the Mihon version to assert on a seed's values.

Values changed with this release: taihei-lorem 4.x had no seeded generator, and every value 5.0.0
draws comes from new data and new draws. A test that compared output of 4.x, or of a 5.0.0
snapshot, with fixed strings needs new expected values. Views from `withLanguage` share one
generator, so the order of calls across them is part of what a seed repeats. A generator shared
across threads stays thread-safe, but its values are then no longer reproducible.

## Coming from taihei-lorem 4.x

1. **Coordinates and package.** `dev.taihei::taihei-lorem` becomes `dev.taihei::taihei-mihon`,
   published as `taihei-mihon_3` (JVM) and `taihei-mihon_sjs1_3` (Scala.js). The package
   `dev.taihei.lorem` becomes `dev.taihei.mihon`. There is no relocation, and both lines can sit
   on one classpath.

   ```sh
   # every import of dev.taihei.lorem becomes dev.taihei.mihon
   grep -rlZ 'dev\.taihei\.lorem' --include='*.scala' . | xargs -0 perl -pi -e 's/dev\.taihei\.lorem/dev.taihei.mihon/g'
   ```

2. **`Language`.** `dev.taihei.i18n.Language` becomes `dev.taihei.tsuji.Language`, from Taihei
   Tsuji 5, the successor of taihei-i18n.
3. **The object `LoremIpsum` became the class `Mihon`.** Create a generator once with
   `Mihon.secure(language, clock)` or `Mihon.seeded(seed, language, clock)` and call the members on
   it; there is no global instance. The language is the second parameter, so
   `Mihon.seeded(seed, clock)` does not compile: write `Mihon.seeded(seed, clock = c)`.

   ```scala
   // 4.x
   import dev.taihei.lorem.LoremIpsum
   LoremIpsum.name

   // 5.0.0
   import dev.taihei.mihon.Mihon
   val mihon = Mihon.secure()
   mihon.name
   ```

4. **Lorem Ipsum is the text of Latin.** `Mihon.secure(Language("la"))` writes Lorem Ipsum; the
   default English generator writes English words.
5. **Renames.**
   - `firstName` and `lastName` become `givenName` and `familyName`.
   - `zipCode` becomes `postalCode`.
   - The random `language` becomes `anyLanguage`; `language` is now the generator's own resolved
     language.
   - `LoremIpsum.languages: Array[Language]`, every language Tsuji knew, becomes
     `Mihon.languages: List[Language]`, the languages with data, sorted by tag.
6. **New types.** `address: String` becomes `address: Address`, with `singleLine` as the one-line
   form. `person` returns the new `Person`, and `Gender` is new. Both `Person` and `Address` are
   values with structural equality, and `toString` is the display form.
7. **Removed.**
   - `description`: use `sentences`.
   - `sources.StateAbbreviations` and `sources.StateFullNames`: nothing used them.
   - The public `sources.*` word lists, `SampleWordCollection` and `ResourceLoader`: internal now,
     use `Mihon`.
8. **Changed behaviour.**
   - `city`, `country`, `street` and `phone` come from the generator's language and its default
     country, no longer from mixed international lists; `country` names any country in the
     language. `phone` is the calling code and a national number of that country, fictional only
     in the United States.
   - `email` is built from the romanised name, so it is ASCII in every language.
   - `title` capitalises every word only in English; other languages with case capitalise the
     first word, and scripts without case none.
   - `sentence` follows the language: a range of words, commas, now and then a quoted span, and a
     full stop, question or exclamation mark in the language's punctuation, where 4.x wrote one to
     ten words and a full stop.
   - `today`, `yesterday`, `tomorrow`, `futureDate` and `pastDate` follow the generator's clock,
     `Clock.systemUTC()` by default, so the default behaviour is the UTC date as before.
   - `id`, `password` and `character` draw from the generator, so a seeded run repeats them. `id`
     is still a version-4 UUID of the IETF variant, with `_` in place of `-`, no longer from
     `UUID.randomUUID`.
   - `SampleImages`, `url` and the file generators keep their 4.x shape.
9. **The seed promise.** The same seed, language, clock and Mihon version give the same values on
   the JVM and on Scala.js; another version may not.
10. **Languages.** The 25 tags above, resolved by tag, alias and stripped subtags; a tag with
    another script is refused, and a language without data throws an `IllegalArgumentException`.
    Use `Mihon.resolve` for a language that comes from outside.
11. **Platform.** Scala 3.9 or later, Java 17 or later. The language data adds about 3 MB to a
    Scala.js bundle, so Mihon is for tests, seed data and mock-ups, not for a production browser
    bundle.
12. **Coming from edo-lorem 3.0.0 or genius-lorem.** `com.edoframework::edo-lorem:3.0.0` and
    `fish.genius::genius-lorem` had the API of taihei-lorem 4.x in the packages
    `com.edoframework.lorem` and `genius.lorem`. Replace either package with `dev.taihei.mihon`
    (and `genius.i18n` or `com.edoframework.i18n` with `dev.taihei.tsuji`), then follow the steps
    above.
13. **Licence and data.** Apache License 2.0, as before. The new third-party data and its
    attributions are in `NOTICE` and `LICENSE-UNICODE`, inside the jars; the 4.x name and place
    lists, which had no source, are gone.

## Compatibility

- **Scala 3.9 or later** (the current LTS), Scala.js 1.22.
- **Java 17 or later** on the JVM. The JVM tests run on Java 17 as well as on the newest Java.
- **Dependencies**: ZIO 2.1.27 and zio-json 1.1.1; on Scala.js also scala-java-time 2.8.0 and
  scalajs-java-securerandom 1.0.0.
- **Taihei Tsuji 5.0.0** (`dev.taihei::taihei-tsuji`), for `Language`. Mihon uses only
  `Language` and never formats. If your code formats Dutch with Tsuji on Java 17 or 18, add
  `dev.taihei::taihei-tsuji:5.0.1` to your own dependencies: it fixes the locale for Dutch on
  those Java versions, and your build resolves the newer version.
- A major release: nothing of the taihei-lorem 4.x API is binary or source compatible (see above).

## Changelog

### Added
- The `Mihon` class with the factories `Mihon.seeded` and `Mihon.secure`, and `Mihon.languages`
  and `Mihon.resolve`; the values `Person` and `Address`, and the enum `Gender`; the members
  `person`, `person(gender)`, `givenName`, `familyName`, `postalCode`, `withLanguage` and
  `anyLanguage`.
- Language data for 25 languages, the scripts in `scripts/data/` that derive it (never run by
  the build; every download pinned by SHA-256 in `sources.lock.json`), `docs/data-sources.md`,
  the attributions in `NOTICE` and the new `LICENSE-UNICODE`, all three licence files in the
  jars.
- A build-time check of every data file, which refuses links, stray files, control and
  invisible formatting characters and malformed data, and a size budget in `make validate`:
  80 KB of data per language and 2.5 MB in all, 1.2 MB for the JVM jar, 6 MB for the full-linked
  Scala.js output of a fixture that uses every language.
- Tests that pin each language's conventions and a seeded run in every language on both
  platforms, and a CI job that runs the JVM tests on Java 17.
- `make repo-guard`: the build resolves from Maven Central only, and every `dev.taihei` pin is
  on Central.
- The release route of the family (one signed bundle, uploaded unchanged to GitHub Packages,
  read back, kept on an immutable GitHub Release, and taken to Maven Central by the release
  train), with `make release-test` against a fake registry on localhost.

### Changed
- The library is renamed from taihei-lorem to Taihei Mihon: artifacts `taihei-mihon_3` and
  `taihei-mihon_sjs1_3`, package `dev.taihei.mihon`, repository `taiheiframework/mihon`.
- `Language` comes from `dev.taihei::taihei-tsuji` 5.0.0 instead of `taihei-i18n` 4.0.2.
- Every draw goes through the generator's own random number generator, and the dates through
  its clock: there is no global generator and no wall clock.
- Scala.js compiles in the same data files the JVM reads; the hand-kept JavaScript copy is gone.
- The JVM module compiles for Java 17 (`-java-output-version:17`).
- The release machinery is hardened: the dry run that builds the signed bundle never receives
  the Central token; the release credentials file is used only when it is the maintainer's own
  regular file with mode 600; the `mill` launcher downloads over HTTPS only and keeps a download
  only when its SHA-256 matches the hash pinned for Mill 1.3.1; a release refuses stray, hidden,
  linked or filtered files in what it publishes; `make publish` serves only an embargoed security
  fix and never starts the Central upload of a version twice.

### Fixed
- The DCO check of pull requests wants the exact `Signed-off-by:` line of the author.

## Where to get it

Taihei Mihon 5.0.0 is published first to this repository's GitHub Packages registry
(`https://maven.pkg.github.com/taiheiframework/mihon`, which needs a GitHub token with
`read:packages`), and reaches Maven Central with the next release train, together with Taihei
Tsuji 5.0.1. The GitHub Release of `v5.0.0` keeps the signed bundle and `SHA256SUMS`, the SHA-256
of every file, which both registries serve unchanged. Releases are signed by the Taihei Framework
release key, fingerprint `5026 B299 6DDB 697E C463  0BCF 14A7 33E2 A507 702F`.

```scala
mvn"dev.taihei::taihei-mihon:5.0.0"       // Mill, JVM
mvn"dev.taihei::taihei-mihon::5.0.0"      // Mill, Scala.js
"dev.taihei" %% "taihei-mihon" % "5.0.0"  // sbt, JVM
"dev.taihei" %%% "taihei-mihon" % "5.0.0" // sbt, Scala.js
```

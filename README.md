# Taihei Mihon

Mihon (見本) means sample or specimen. Taihei Mihon is a **fake-data generator for Scala 3 in many
languages**: text, people, addresses, phone numbers, dates, files, ids and sample images, in the
script and conventions of the language you choose, from a seed when you want the same values on
every run. Cross-compiled for the **JVM and Scala.js**.

Part of the [Taihei Framework](https://taihei.dev), the open-source Scala 3
libraries from Innovenso. The package is `dev.taihei.mihon`. Up to 4.x the library was called
taihei-lorem: see [Coming from taihei-lorem 4.x](#coming-from-taihei-lorem-4x).

## Modules

Published on Maven Central under `dev.taihei`:

| Artifact            | Platform | Notes                                                  |
| ------------------- | -------- | ------------------------------------------------------ |
| `taihei-mihon`      | JVM      | Language data read from the classpath.                 |
| `taihei-mihon_sjs1` | Scala.js | Language data compiled in; runs in the browser or Node. |

```scala
mvn"dev.taihei::taihei-mihon:5.0.0"       // Mill, JVM
mvn"dev.taihei::taihei-mihon::5.0.0"      // Mill, Scala.js
"dev.taihei" %% "taihei-mihon" % "5.0.0"  // sbt, JVM
"dev.taihei" %%% "taihei-mihon" % "5.0.0" // sbt, Scala.js (sbt-scalajs or sbt-crossproject)
```

`shared/` holds the generator and, in `shared/resources/`, the language data; `jvm/` and `js/`
carry only the platform-specific loader of that data. Depends on
[Taihei Tsuji](https://github.com/taiheiframework/tsuji) for `Language`.

Mihon is for tests, seed data and mock-ups. Its language data adds a few megabytes to a Scala.js
bundle, so it does not belong in a production browser bundle.

5.0.0 is the first release under the name Taihei Mihon, and a major release: see
[Coming from taihei-lorem 4.x](#coming-from-taihei-lorem-4x). The 4.x line keeps its name,
`dev.taihei::taihei-lorem`, and receives security fixes for 12 months after the release of
Taihei Mihon 5.0.0.

## Stack

- Mill 1.3.1
- Scala 3.9.0 (LTS) · Scala.js 1.22.0
- ZIO 2.1.27 (`zio-json` 1.1, `zio-test`)
- `dev.taihei::taihei-tsuji` 5.0.0 · scala-java-time 2.8 and scalajs-java-securerandom (JS)

The build resolves from Coursier's defaults (the local Ivy repository, then Maven Central), then
from the family's Maven repository (`REPO_URL` in the Makefile, built from the immutable GitHub
Releases) for `dev.taihei` alone, and from nothing else; none of them needs credentials.

## Build

```sh
make compile        # ./mill __.compile           (JVM + JS)
make test           # ./mill __.test.testForked    (JVM + JS; JS runs on Node)
make fmt            # ./mill __.reformat
make fix            # ./mill __.fix                 (scalafix)
make validate       # repo-guard + release-test + data-check-test + fix + fmt + compile + test + mima + budget
make budget         # the size budget of the data, the JVM jar and the Scala.js output
make repo-guard     # Coursier's defaults (local Ivy, Central), then REPO_URL for dev.taihei
                    # alone, nothing else; every resolved dev.taihei file checked
make release-test   # the release machinery's own tests, against stand-ins on localhost
make mima           # binary compatibility with the newest release tag of the current major,
                    # which must be on Central or REPO_URL (MIMA_BASELINE=x.y.z until it is)
make publishLocal   # to ~/.ivy2/local (a SNAPSHOT only)
```

## Releasing

The version is the git tag: an exact `v`-tag on a clean tree is the release,
anything else a SNAPSHOT of the last tag.

```sh
make release-prepare TYPE=patch|minor|major EMBARGO=0|1
                                               # validate, write RELEASE_x_y_z.md
make release-finalize                          # repo-guard, commit the notes, tag locally,
                                               # check Central, REPO_URL and GitHub, build
                                               # and sign the bundle once; uploads nothing
git push origin main && git push origin vX.Y.Z # from here on the version is final
make release-github                            # publishes it: the bundle on an immutable
                                               # GitHub Release, read back and compared
make publish-repo                              # the repository's deploy, then every file
                                               # read back (verify-repo)
make release-abort                             # until the tag (and the notes commit)
                                               # is pushed: undo the preparation
make publish-dry-run                           # the signed bundle, written to out/
```

`release-prepare` refuses while the previous release is not on Maven Central yet: at most one
version of a library waits for the release train.

A security fix (`make release-prepare … EMBARGO=1`) goes the other way: `release-finalize`
publishes it to Maven Central with `make publish`. Once the Central Portal says PUBLISHED and
repo1.maven.org answers 200 for it, push `main` and the tag; once its advisory is published,
`make embargo-lift VERSION=x.y.z` records the lift, which nothing after it does without, then
`make central-bundle` (the bundle from Central's own files), `make release-github` and
`make publish-repo`.

A release is built and signed once, as one bundle. Its zip and `SHA256SUMS`, the SHA-256 of
every file, are kept on an immutable GitHub Release of the tag; that release publishes the
version. The family's Maven repository is built from those releases alone, so nothing is ever
uploaded to it, and the next release train takes the same files to Maven Central. Both serve
exactly the bytes `SHA256SUMS` lists. A security fix goes to Central first (`EMBARGO=1`), and
gets its GitHub Release, from Central's own files (`make central-bundle`), only after its
advisory is published.

`make validate` checks the build's repositories (`repo-guard`): Coursier's defaults (the local
Ivy repository, then Maven Central), then `REPO_URL` for `dev.taihei` alone, and nothing else,
for every module that resolves dependencies, published or not; every `dev.taihei` pin on one of
them (taihei-tsuji included); every file the build resolved from one of them; and every resolved
`dev.taihei` file in the `SHA256SUMS` of its version's immutable GitHub Release and signed by
the release key (`keys/release-key.asc`), or, for a version without a release, on Central with a
good signature by that key.

repo-guard asks the GitHub API without a token: when the API answers HTTP 403 or 429 (its limit
for anonymous requests), wait an hour and run it again. When it names a file other than
`dev.taihei` in the Coursier cache of `REPO_URL`, remove that repository's directory from the
cache (`~/.cache/coursier/v1/https/taiheiframework.github.io/repo/` on Linux,
`~/Library/Caches/Coursier/v1/https/taiheiframework.github.io/repo/` on macOS) and build again.

A release publishes only the commit `release-prepare` validated, and refuses when a file git
does not track is in a source or resource folder, or a stray build file is in the tree.
Credentials never go on make's command line (`make … VAR=value`): make hands such a value on to
every command it runs, also after an `unset`. They come from `~/.edo-release.env` or the
environment. From the file only `make publish` and the dry run's signing step (the key alone)
get them; do not export them in the shell you build in, where every Mill call of a build target
and of `release-prepare` would see them (repo-guard, `release-preflight` and the dry run unset
them first).

`make publish` is only for a security fix under embargo, which goes to Central
first; it refuses any other version, and a version whose Central upload it
started before. It reads the Central Portal user token and
the release key from `~/.edo-release.env` (a name from the Edo Framework days, kept because the
maintainers' workstations write it for every library; `RELEASE_ENV=…` on the
make command line points elsewhere, and the file must be yours, mode 600, not a
link) or the environment (`MILL_SONATYPE_USERNAME`,
`MILL_SONATYPE_PASSWORD`, `MILL_PGP_SECRET_BASE64`, `MILL_PGP_PASSPHRASE`).
The dry run that builds the release bundle gets the release key but never the
Central token, neither from that file nor from the shell. The `mill` launcher
downloads Mill over HTTPS only, and runs Mill only when its SHA-256 matches the
one pinned in the launcher: it checks a download before keeping it, and the
file it is about to run before every run, so a tampered copy in Mill's cache is
refused, as is a version without a pin. `release-preflight` (inside `release-finalize`), `release-github`
and `publish-repo` use the maintainer's `gh` login; nothing else in a release
needs a token.
Releases are signed by the Taihei Framework release key,
fingerprint `5026 B299 6DDB 697E C463  0BCF 14A7 33E2 A507 702F`.

## Quick start

```scala
import dev.taihei.mihon.Mihon
import dev.taihei.tsuji.Language

val mihon = Mihon.secure()      // English, values that do not repeat

mihon.name                      // a name such as "Linda Robinson"
mihon.email                     // such as "linda.robinson@brightwavehub.example"
mihon.paragraph                 // a paragraph of English words
mihon.address.singleLine        // such as "180 Cedar Lane, Houston, Texas 77233, United States"
mihon.phone                     // such as "+1 537 555-0154", a number reserved for fiction
mihon.localDate                 // a random java.time.LocalDate
mihon.id                        // a UUID-shaped identifier

val lorem = Mihon.seeded(42L, Language("la"))
lorem.paragraph                 // Lorem Ipsum
```

A generator is bound to one language, English by default. Its text is in the language's
script and punctuation, its people are named in the language's name order, and its addresses
and phone numbers are those of the language's default country. `withLanguage` gives a view in
another language that shares the same random generator and clock.

### Languages

`Mihon.languages` lists the languages with data, sorted by tag, each with its default country:

| Tag | Language | Script | Country | Conventions |
| --- | --- | --- | --- | --- |
| `ar` | Arabic | Arabic, right to left | Egypt | ، and ؟ |
| `bg` | Bulgarian | Cyrillic | Bulgaria | „ “, female forms of family names |
| `de` | German | Latin | Germany | „ “, nouns capitalised |
| `el` | Greek | Greek | Greece | ; as the question mark, « », female forms of family names |
| `en` | English | Latin | United States | title case in titles; phone numbers reserved for fiction |
| `es` | Spanish | Latin | Spain | ¿ ? and ¡ ! |
| `fr` | French | Latin | France | « », a narrow space before ? and ! |
| `he` | Hebrew | Hebrew, right to left | Israel | |
| `hi` | Hindi | Devanagari | India | । as the full stop; states in addresses |
| `hu` | Hungarian | Latin | Hungary | family name first; city first in addresses |
| `it` | Italian | Latin | Italy | « » |
| `ja` | Japanese | kanji and kana | Japan | no spaces, 。、「」, family name first, kana readings |
| `ko` | Korean | Hangul | South Korea | spaces between words, family name first without a space |
| `la` | Latin | Latin | Vatican City | Lorem Ipsum |
| `nl` | Dutch | Latin | Netherlands | ‘ ’, ij capitalised as IJ |
| `pl` | Polish | Latin | Poland | „ ”, female forms of family names |
| `pt` | Portuguese | Latin | Brazil | states in addresses |
| `ru` | Russian | Cyrillic | Russia | « », female forms of family names |
| `sr-Cyrl` | Serbian | Cyrillic | Serbia | „ ” |
| `th` | Thai | Thai | Thailand | no spaces within a sentence, no final stop |
| `tr` | Turkish | Latin | Turkey | i capitalised as İ |
| `uk` | Ukrainian | Cyrillic | Ukraine | « », the apostrophe in words |
| `vi` | Vietnamese | Latin | Vietnam | family name first |
| `zh-Hans` | Chinese (Simplified) | Han | China | no spaces, 。，, family name first |
| `zh-Hant` | Chinese (Traditional) | Han | Taiwan | no spaces, 。，「」, family name first |

The same seed in several scripts:

```scala
Mihon.seeded(1L, Language("ja")).person      // 田中さちこ, latin "Tanaka Sachiko", reading タナカ サチコ
Mihon.seeded(1L, Language("ru")).person      // Людмила Фомина, latin "Liudmila Fomina"
Mihon.seeded(1L, Language("el")).person      // Βασιλική Κοκολάκη, latin "Vasiliki Kokolaki"
Mihon.seeded(1L, Language("ar")).person      // ناديا إبراهيم, latin "Nadiyah Ibrahim"
Mihon.seeded(1L, Language("he")).person      // מירב לנקרי, latin "Meirav Lankri"
Mihon.seeded(1L, Language("hi")).person      // विद्या चटर्जी, latin "Vidya Chatterjee"
Mihon.seeded(1L, Language("zh-Hans")).person // 彭薇, latin "Peng Wei"
Mihon.seeded(1L, Language("ko")).person      // 장소희, latin "Jang So-hui"
Mihon.seeded(1L, Language("ru")).sentence    // "Люди провёл хотели отец нужна пить знакомы себя со игры."
Mihon.seeded(1L, Language("ja")).address.lines // List("日本", "〒873-4446", "京都市中央29-20-14")
Mihon.seeded(1L, Language("hi")).address.lines // List("179, मुख्य बाज़ार", "अमृतसर 273444", "पंजाब", "भारत")
```

The words of a language's text are drawn by how often they are used, so the text has the
language's script, letters and rhythm, but it means nothing: it is filler, as Lorem Ipsum is.

A factory or `withLanguage` looks a language up by its tag, then by an alias (`zh`, `zh-CN` and
`zh-SG` are `zh-Hans`; `zh-TW`, `zh-HK` and `zh-MO` are `zh-Hant`; `sr` and `sr-RS` are
`sr-Cyrl`), then with subtags stripped from the right, so `en-GB` is served by `en`, `nl-BE` by
`nl` and `pt-PT` by `pt` (with Brazilian addresses). A tag that names another script than the one
found is refused (`sr-Latn`, `ru-Latn`), and so is a language without data, with an
`IllegalArgumentException` that names the tag and the languages there are. With a language
from outside, ask first:

```scala
val mihon = Mihon.secure(Mihon.resolve(requested).getOrElse(Language.English))
mihon.language // the language whose data it uses: en for en-GB
```

A later minor release may serve a tag that is refused today, or serve it by a closer language
(`nl-BE` once it has its own data). Pin the Mihon version to rely on `language`.

### People and addresses

```scala
val p = Mihon.seeded(1L).person
p.givenName; p.familyName; p.gender  // Gender.Female or Gender.Male
p.fullName                           // in the language's name order
p.latin                              // romanised (pinyin in both Chinese); fullName in Latin script
p.reading                            // the kana reading in Japanese, None elsewhere

val a = Mihon.seeded(1L).address
a.lines                              // top to bottom, in the country's format
a.street; a.city; a.region; a.postalCode; a.country; a.countryCode
```

`Person` and `Address` are values: two equal in every member are equal, so one seed gives equal
people and addresses, and `distinct` and `Set` treat them as values; `toString` is the full
name or the single line, for display. `email` is built from the romanised name, in ASCII, at a
domain under the reserved `.example` top-level domain, so it never reaches a real host. Digits are ASCII in every language. Where a
country reserves phone numbers for fiction (the United States' 555-0100 to 555-0199), `phone`
uses them; elsewhere a number may be real, so never call or text one.

### Dates

Mihon draws dates as `java.time.LocalDate` values and formats none: format them in the
reader's language with [Taihei Tsuji](https://github.com/taiheiframework/tsuji), whose output
follows each platform's locale data.

### Seeds and the clock

A generator draws every value from its own random number generator and reads today's date from
its own clock. Create one of two kinds:

- `Mihon.secure()` draws from a `java.security.SecureRandom`: the values do not repeat from run
  to run.
- `Mihon.seeded(seed)` draws from a `java.util.Random` with that seed. The same seed, language,
  clock and Mihon version give the same values in the same order, on the JVM and on Scala.js.
  Any other Mihon version, a patch included, may give other values; the release notes say so
  whenever they do. Pin the Mihon version to assert on a seed's values.

Both take the language and a `java.time.Clock`, `Clock.systemUTC()` by default. `today`,
`yesterday`, `tomorrow`, `futureDate` and `pastDate` follow the clock:

```scala
import java.time.{Clock, Instant, ZoneOffset}

val clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC)
val fixed = Mihon.seeded(42L, clock = clock)

fixed.today       // 2026-10-24
fixed.futureDate  // the same date on every run
```

Views from `withLanguage` share one generator, so the order of calls across them is part of what
a seed repeats. One generator may be shared across threads; it stays thread-safe, but the order
in which threads draw then decides the values, so they are no longer reproducible.

### Members by category

- **Text**: `word`, `words`, `sentence`, `sentences`, `paragraph`, `paragraphs`, `htmlParagraphs`, `title`, `slug`
- **People**: `person`, `person(gender)`, `name`, `givenName`, `familyName`, `email`
- **Places and contact**: `address`, `street`, `city`, `postalCode`, `country`, `phone`
- **Dates**: `localDate`, `today`, `yesterday`, `tomorrow`, `futureDate`, `pastDate`
- **Files and the web**: `url`, `fileExtension`, `fileExtensionWithoutDot`, `mimeType`
- **Ids and primitives**: `id`, `character`, `password`, `bool`, `int(bound)` and `long(bound)` (in `[1, bound]`), `double(bound)` (in `[0, bound)`), `times(bound, f)` (one to `bound` items)
- **Languages**: `language`, `withLanguage`, `anyLanguage`; `Mihon.languages`, `Mihon.resolve`
- **Images**: `SampleImages.landscape`, `SampleImages.portrait` (base64 PNGs, constants)

## Testing

```scala
import dev.taihei.mihon.Mihon
import zio.test.*

val mihon = Mihon.seeded(42L)

assertTrue(
  mihon.email.endsWith(".example"),
  mihon.person.fullName.nonEmpty,
)
```

The suite runs on both platforms (`make test`); JS tests execute under Node. `SeededSpec` pins
a seeded run in every language, so it proves on each build that the JVM and Scala.js draw the
same values.

## Data

The language data and where it comes from (Unicode CLDR, the Tatoeba Project, Wikidata, the
U.S. Census Bureau, and formats written for this library) are described in
[docs/data-sources.md](docs/data-sources.md), with the licence of each; NOTICE carries the
attributions. The scripts that build the data are in `scripts/data/`; the build never runs them.
`make validate` checks every data file against its schema and holds the data, the JVM jar and the
Scala.js output to a size budget.

## Coming from taihei-lorem 4.x

Taihei Mihon 5.0.0 is the successor of `dev.taihei::taihei-lorem` 4.x under a new name. The
coordinates and the package change with it:

- the dependency is `dev.taihei::taihei-mihon`;
- the package is `dev.taihei.mihon`, where it was `dev.taihei.lorem`;
- `language` returns a `dev.taihei.tsuji.Language` of
  [Taihei Tsuji](https://github.com/taiheiframework/tsuji) 5, the successor of taihei-i18n.

There is no taihei-lorem 5.0.0 and no relocation from the old coordinates. The two lines are
different artifacts in different packages, so they can be on one classpath side by side.

```sh
# every import of dev.taihei.lorem becomes dev.taihei.mihon
grep -rlZ 'dev\.taihei\.lorem' --include='*.scala' . | xargs -0 perl -pi -e 's/dev\.taihei\.lorem/dev.taihei.mihon/g'
```

The object `LoremIpsum` became the class `Mihon`, bound to one language: create a generator
once and call the members on it. Lorem Ipsum is now the text of Latin.

| 4.x | 5.x |
| --- | --- |
| `LoremIpsum.name` (any generator) | `val mihon = Mihon.secure()`, then `mihon.name` |
| Lorem Ipsum text (`word`, `sentence`, `paragraph` …) | `Mihon.secure(Language("la"))`; an English generator gives English words |
| no way to repeat values | `Mihon.seeded(seed)`; the language is the second parameter, so a clock is `clock = c` |
| `today` is the UTC date by the wall clock | `today` follows the generator's clock, `Clock.systemUTC()` by default |
| `firstName`, `lastName` | `givenName`, `familyName` |
| `zipCode` | `postalCode` |
| `address: String` | `address: Address`; `address.singleLine` is a one-line form |
| `description` | removed: use `sentences` |
| `language` (a random `Language`) | `anyLanguage`, one of `Mihon.languages`; `language` is the generator's own |
| `LoremIpsum.languages: Array[Language]`, every language Tsuji knows | `Mihon.languages: List[Language]`, the languages with data, sorted by tag |
| `sentence`: one to ten words, a capital and a full stop | a range of words per language, with commas, now and then a quoted span, and a full stop, question or exclamation mark in the language's punctuation |
| `title`: every word capitalised | English alone capitalises every word; other cased languages only the first, and scripts without case none |
| `email` from the name with everything but ASCII letters removed | from the person's romanised name, so it is ASCII in every language (`wang.wei@…example`) |
| `city`, `country`, `street`, `phone` from mixed international lists | from the generator's language and its default country; `country` names any country in the language |
| `phone` (`+32` and nine digits) | the calling code and a national number of the default country |
| `dev.taihei.i18n.Language` | `dev.taihei.tsuji.Language` |
| `dev.taihei.lorem.sources.*` (word lists), `SampleWordCollection`, `ResourceLoader` | internal now: use `Mihon` |
| `sources.StateAbbreviations`, `sources.StateFullNames` | removed; nothing used them |
| `id` from `UUID.randomUUID` | a version-4 UUID drawn from the generator, so a seeded one repeats |

The name, city and country lists of 4.x had no recorded source and are gone; the names and
places of 5.x come from the sources in [docs/data-sources.md](docs/data-sources.md).
`SampleImages`, `url`, `character`, `password`, the file generators and the other date members
keep their 4.x shape; `id` and `today` changed as the table says.

## Coming from edo-lorem 3.0.0 or genius-lorem

`com.edoframework::edo-lorem:3.0.0` and `fish.genius::genius-lorem` had the API of
taihei-lorem 4.x in the packages `com.edoframework.lorem` and `genius.lorem`. Replace either
package with `dev.taihei.mihon` in your imports (and `genius.i18n` or `com.edoframework.i18n`
with `dev.taihei.tsuji`), then follow [Coming from taihei-lorem 4.x](#coming-from-taihei-lorem-4x).
genius-lorem 2.0.0 was on an earlier Scala: Taihei Mihon needs Scala 3.9 or later, `zio-json`
1.1, and is under the Apache License 2.0.

## License

Apache License 2.0: see [LICENSE](LICENSE) and [NOTICE](NOTICE). The NOTICE records
the names the library was published under and its licence history:
`fish.genius::genius-lorem` 1.0.0 to 1.1.0 were published under the GPL-3.0 and 2.0.0
under the MIT License; the copyright holder relicensed that code under the Apache
License 2.0, from 3.0.0 on.

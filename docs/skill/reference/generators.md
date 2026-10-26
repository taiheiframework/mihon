# generators: the Mihon catalog

Package `dev.taihei.mihon`. `Mihon` is a final class; every member draws a new value from the
generator's own `java.util.Random` (seeded) or `SecureRandom` (secure), in the generator's
language.

## Creating a generator

| Member | Returns |
| --- | --- |
| `Mihon.seeded(seed: Long, language: Language = Language.English, clock: Clock = Clock.fixed(Mihon.SeededInstant, ZoneOffset.UTC)): Mihon` | values that repeat for the same seed, language, clock and Mihon version, on the JVM and Scala.js; without a clock, dates are relative to 2026-01-01 |
| `Mihon.secure(language: Language = Language.English, clock: Clock = Clock.systemUTC()): Mihon` | values from a `SecureRandom`, which do not repeat |
| `Mihon.SeededInstant: Instant` | 2026-01-01T00:00:00Z, where a `seeded` generator's default clock stands; fixed within a major |
| `Mihon.languages: List[Language]` | the languages with data, sorted by tag |
| `Mihon.resolve(language: Language): Option[Language]` | the language a factory would use, or `None` |

Both factories and `withLanguage` throw `IllegalArgumentException` for a language without data.

## Language

| Member | Returns |
| --- | --- |
| `language: Language` | the resolved language of the data (`en` for `en-GB`) |
| `withLanguage(language: Language): Mihon` | a view in another language, sharing the generator and the clock |
| `anyLanguage: Language` | one of `Mihon.languages`, each equally likely |

## Text

| Member | Returns |
| --- | --- |
| `word: String` | one word of the language, frequent words more often; Lorem Ipsum for `la` |
| `words: String` | one to ten words, joined by the language's word separator |
| `sentence: String` | a sentence: capitalised where the script has case, now and then a comma or a quoted span, ending in the language's full stop, question or exclamation mark |
| `sentences: String` | one to five sentences |
| `paragraph: String` | one to six sentences |
| `paragraphs: String` | one to four paragraphs |
| `htmlParagraphs: String` | one to four paragraphs wrapped in `<p>…</p>` |
| `title: String` | one to five words; every word capitalised in English, the first only elsewhere |
| `slug: String` | one to ten words joined by `/`, with one more word appended to the last without a slash (so at most nine slashes), in the language's script |

## People

| Member | Returns |
| --- | --- |
| `person: Person` | a person of either gender |
| `person(gender: Gender): Person` | a person with a given name of that gender and the family name in the matching form |
| `name: String` | a new person's `fullName` |
| `givenName: String` | a female or a male given name |
| `familyName: String` | a family name, in its female or male form |
| `email: String` | the romanised name in ASCII, lower case, dot-separated, at a domain under the reserved `.example` TLD |

`Person` (final class, made by `Mihon`):

| Member | Returns |
| --- | --- |
| `givenName: String`, `familyName: String` | in the language's script |
| `gender: Gender` | `Gender.Female` or `Gender.Male` |
| `fullName: String` | in the language's name order and separator |
| `reading: Option[String]` | the katakana reading in Japanese, `None` elsewhere |
| `latin: String` | romanised, in the language's name order; `fullName` for Latin script |

## Places and contact

| Member | Returns |
| --- | --- |
| `address: Address` | an address in the format of the language's default country |
| `street: String` | a street and number made up from the language's names and words |
| `city: String` | a real city of the default country, larger ones more often |
| `postalCode: String` | the default country's pattern, ASCII digits |
| `country: String` | any country's name in the language |
| `phone: String` | `+`, the calling code and a national number of the default country, ASCII digits; fictional only where the country reserves a range |

`Person` and `Address` are values: equal when every member is equal (a field added in a later
minor joins equality), with `toString` the full name or the single line, for display.

`Address` (final class, made by `Mihon`): `lines: List[String]` (top to bottom),
`singleLine: String`, `street`, `postalCode`, `city`, `region: Option[String]` (US, Brazil,
India), `country` (the name in the language) and `countryCode` (ISO 3166-1 alpha-2).

## Dates (`java.time.LocalDate`)

| Member | Returns |
| --- | --- |
| `localDate` | a random date (year in [1, 3000], any month, day of month in [1, 28]) |
| `today` / `yesterday` / `tomorrow` | the clock's date, in the clock's zone, and ± 1 day |
| `futureDate` / `pastDate` | today ± one to a thousand weeks |

Mihon formats no date: format them with Taihei Tsuji.

## Files and the web

| Member | Returns |
| --- | --- |
| `url: String` | `https://name.example` |
| `fileExtension: String` | e.g. `.pdf` |
| `fileExtensionWithoutDot: String` | e.g. `pdf` |
| `mimeType: String` | e.g. `application/pdf` |

## Ids and primitives

| Member | Returns |
| --- | --- |
| `id: String` | a version-4 UUID drawn from the generator (hyphens → underscores) |
| `password: String` | 30 random characters (fake data, not a credential) |
| `character: String` | one character (letter, digit or symbol) |
| `bool: Boolean` | random boolean |
| `int(bound: Int): Int` | random int in `[1, bound]` (never 0) |
| `long(bound: Int): Long` | random long in `[1, bound]`; the bound is an `Int` |
| `double(bound: Double): Double` | random double in `[0, bound)`; refuses a bound that is not positive and finite |
| `times[A](bound: Int, f: Int => A): List[A]` | `f` applied to `1..n` for a random `n` in `[1, bound]` |

## Sample images

`SampleImages` is an object with two base64-encoded PNGs (constants, not random):

| Member | Returns |
| --- | --- |
| `SampleImages.landscape: String` | base64 landscape PNG, 399 by 310 pixels |
| `SampleImages.portrait: String` | base64 portrait PNG, 160 by 240 pixels |

## Usage

```scala
import dev.taihei.mihon.{Gender, Mihon}
import dev.taihei.tsuji.Language

val mihon = Mihon.seeded(42L)

val person  = mihon.person(Gender.Female)
val address = mihon.address              // address.lines, address.singleLine
val heading = mihon.title
val body    = mihon.htmlParagraphs       // <p>…</p><p>…</p>…

// a batch of test rows
val rows = mihon.times(10, _ => (mihon.name, mihon.localDate))

// filler text
val lorem = mihon.withLanguage(Language("la")).paragraph
```

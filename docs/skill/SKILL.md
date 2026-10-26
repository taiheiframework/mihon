---
name: taihei-mihon
description: How to use Taihei Mihon (dev.taihei::taihei-mihon, formerly taihei-lorem, edo-lorem and genius-lorem), Innovenso's cross-platform fake-data generator for Scala 3 (JVM and Scala.js) in many languages and scripts. Apply when writing or reviewing Scala that needs test or seed data (text, people, addresses, phone numbers, dates, files, ids, sample images) through dev.taihei.mihon.Mihon, or when migrating from dev.taihei.lorem.LoremIpsum. Covers the language-bound generator, resolution and refusal of languages, Person and Address, seeds and the clock, and the language data.
---

# Taihei Mihon (formerly taihei-lorem)

A fake-data generator for Scala 3, cross-compiled for the **JVM and Scala.js**, in many
languages. One class, `Mihon`, bound to one language, with a flat catalog of generators for
text, people, places, dates, files and ids; the values `Person` and `Address`; the enum
`Gender`; and `SampleImages`.

The package is `dev.taihei.mihon`. Artifacts on Maven Central: `dev.taihei::taihei-mihon`,
published as `taihei-mihon_3` (JVM) and `taihei-mihon_sjs1_3` (Scala.js), from 5.0.0, compiled
with Scala 3.9 (LTS), Apache 2.0. It depends on `dev.taihei::taihei-tsuji` 5 for `Language`.
Source: <https://github.com/taiheiframework/mihon>. Up to 4.x it was `dev.taihei::taihei-lorem`
in package `dev.taihei.lorem`, with `LoremIpsum` as an object.

## When to use this skill

Read this whenever you write or review Scala that imports `dev.taihei.mihon.*`, or code on
`dev.taihei.lorem.*` that moves to 5.x: test fixtures, seed data, mock-up content, tests of an
internationalised UI.

## The model

Create a generator with one of two factories; there is no global instance:

```scala
import dev.taihei.mihon.Mihon
import dev.taihei.tsuji.Language

val mihon = Mihon.secure()                    // English; SecureRandom, values do not repeat
val fixed = Mihon.seeded(42L)                 // java.util.Random(42): the same values on every run
val lorem = Mihon.seeded(42L, Language("la")) // Latin: Lorem Ipsum

mihon.name             // in the language's name order
mihon.address.lines    // in the default country's format
mihon.withLanguage(Language("la")).sentence // same generator and clock, other data
```

Both factories take `language` (English by default) and then a `java.time.Clock`;
`today`, `yesterday`, `tomorrow`, `futureDate` and `pastDate` follow it. `secure` uses
`Clock.systemUTC()`; `seeded` uses a clock fixed at `Mihon.SeededInstant` (2026-01-01T00:00:00Z),
so a seed's dates repeat too. Pass `clock = Clock.systemUTC()` for dates relative to the real day.

## Reference index

- **`reference/generators.md`**: the full `Mihon` catalog by category, `Person`, `Address`,
  `Gender` and `SampleImages`, with exact signatures.
- **`reference/internals.md`**: languages and their resolution, seeds and reproducibility, the
  clock, the language data and its sources, and the build layout.

## Top gotchas

- The package is **`dev.taihei.mihon`** and the class **`Mihon`**; `LoremIpsum` is gone, with no
  alias. Lorem Ipsum is the text of `Language("la")`; an English generator writes English words.
- `Mihon.seeded(seed, clock)` does not compile: the language is the second parameter, so write
  `Mihon.seeded(seed, clock = c)`.
- A language without data throws `IllegalArgumentException`; with a language from outside, use
  `Mihon.resolve(lang).getOrElse(Language.English)`. `en-GB` is served by `en`; a tag naming
  another script (`en-Cyrl`) is refused.
- A seed repeats **within one Mihon version** only, for the same seed, language and clock; any
  other version, a patch included, may change the values. Pin the version to assert on values;
  otherwise assert on shape.
- `withLanguage` views share one generator: the order of calls across views is part of the
  sequence.
- `Person` and `Address` compare by value: one seed gives equal people and addresses.
- `zh-TW`, `zh-HK` and `zh-MO` are Traditional Chinese (`zh-Hant`), never Simplified; `pt-PT` is
  served by `pt`, whose addresses are Brazilian.
- `firstName`/`lastName` are `givenName`/`familyName`, `zipCode` is `postalCode`, `address`
  returns an `Address`, `description` is removed, and the random `language` is `anyLanguage`.
- Dates are `LocalDate` values; Mihon formats none. Format them with Taihei Tsuji.
- `email` is ASCII in every language (from the romanised name); digits are ASCII everywhere.
- `phone` is fictional only where the country reserves a range (the United States); elsewhere
  a number may be real.
- `password` is fake data, not a credential: from a seeded generator it is predictable.

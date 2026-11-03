# Release 6.0.0

**Release Date:** 2026-11-03

Taihei Mihon 6.0.0 is a major release. It moves to Taihei Tsuji 6.0.0, whose `Language` is the one
Mihon's API takes, and reads its own data files with jsoniter-scala-core instead of zio-json. ZIO,
zio-streams and zio-json therefore leave Mihon's dependencies: an application that does not use
ZIO no longer gets it through Mihon. A seeded generator's dates now repeat with its seed. Mihon's
own API is otherwise unchanged.

6.0.0 is not on Maven Central, which keeps Mihon 5.0.0. It is published to the Taihei Framework's
own Maven repository.

## Highlights

- **No ZIO.** `taihei-mihon` depends on `taihei-tsuji` 6.0.0 and jsoniter-scala-core and, on
  Scala.js, scala-java-time, scala-java-time-tzdb and scalajs-java-securerandom. No ZIO module is
  on its classpath at run time.
- **Seeded dates repeat.** `Mihon.seeded` without a clock uses a clock fixed at the new
  `Mihon.SeededInstant` (2026-01-01T00:00:00Z), so `today`, `yesterday`, `tomorrow`, `futureDate`
  and `pastDate` are the same on every run.

## Artifacts

| Artifact | Platform |
| --- | --- |
| `dev.taihei:taihei-mihon_3` | JVM |
| `dev.taihei:taihei-mihon_sjs1_3` | Scala.js |

```scala
mvn"dev.taihei::taihei-mihon:6.0.0"       // Mill, JVM
mvn"dev.taihei::taihei-mihon::6.0.0"      // Mill, Scala.js
"dev.taihei" %% "taihei-mihon" % "6.0.0"  // sbt, JVM
"dev.taihei" %%% "taihei-mihon" % "6.0.0" // sbt, Scala.js (sbt-scalajs or sbt-crossproject)
```

## Dependencies

- `dev.taihei::taihei-tsuji` 6.0.0 (Apache 2.0).
- `com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core` 2.42.0 (MIT), pinned to that exact
  version, the same as Tsuji 6.0.0's. Mihon's data files are read by hand-written decoders on its
  token API; no macro is used. A build test resolves the published POMs and fails if the resolved
  jsoniter-scala-core differs from this version, or Tsuji brings another.
- No `dev.zio` artifact at run time. A build test resolves the published POMs transitively, on the
  JVM and Scala.js, and fails if one appears.

## Coming from Taihei Mihon 5.x

- **Taihei Tsuji 6.0.0.** Move Tsuji to 6.0.0 at the same time and follow its migration notes.
- **Seeded dates repeat.** The relative dates of `Mihon.seeded(seed)` are relative to
  2026-01-01, so a `futureDate` may lie before the real date. For the old behaviour, pass
  `clock = Clock.systemUTC()`. `Mihon.secure` is unchanged.
- **Thai data.** Thai has 103 female given names (92 in 5.0.0), and four of its male given names
  and one word of its text are replaced. A seed's Thai values therefore differ from 5.0.0.
- **Fewer transitive dependencies.** If your code used ZIO or zio-json without depending on them,
  add your own dependency.

## Mihon 5.0.0 with Tsuji 6.0.0

Quoted from Tsuji 6.0.0's release notes:

> Taihei Mihon 5.0.0 also runs with Tsuji 6.0.0. Every reference from Mihon 5.0.0's classes into
> `dev.taihei.tsuji` resolves in Tsuji 6.0.0's classes, checked on the JVM bytecode and on the
> Scala.js IR: Mihon 5.0.0 uses only `Language`, `Language(tag)`, `Language.English` and
> `language.tag`. So both orders of upgrading work. Mihon 5.0.0 declares zio-json itself, so it
> keeps zio-json and ZIO on the classpath; with Mihon 6.0.0, the Mihon release built on Tsuji
> 6.0.0, they are gone.

## Compatibility

A major release: MiMa has no 6.x baseline. Mihon's public API (`Mihon`, `Person`, `Address`,
`Gender`, `SampleImages`) keeps its 5.0.0 signatures apart from `seeded`'s default clock (a source
change) and the new `Mihon.SeededInstant`. It is a major because Mihon's API exposes Tsuji's
`Language`, and Tsuji moves to its next major. Scala 3.9 (LTS).

## Signature

Every jar and POM is signed by the release key `5026B2996DDB697EC4630BCF14A733E2A507702F`.

# CLAUDE.md

Guidance for Claude Code in this repository: **Taihei Mihon** (`dev.taihei::taihei-mihon`), a
cross-platform fake-data generator for Scala 3 on the JVM and Scala.js in many languages and
scripts, the successor of taihei-lorem 4.x. Part of the Taihei Framework; the family's rules come
from the parent folder's `CLAUDE.md` when this repo is checked out under `~/Projects/Taihei`.

## Commands

```bash
make fix && make fmt   # scalafix, scalafmt: rewrite the sources
make validate          # repo-guard, release and data-check tests, check scalafmt and scalafix, compile, test, MiMa and budget, JVM and JS
make budget            # size budget: data per language and in all, JVM jar, Scala.js fixture
make repo-guard        # Coursier's defaults (local Ivy, Central), then REPO_URL for dev.taihei alone;
                       # pins released; resolved dev.taihei files checked
make mima              # against the newest tag of release.majorLine that has an immutable GitHub
                       # Release (a tag without one is spent); MIMA_BASELINE=none for a major
make test              # ./mill __.test.testForked (JS tests run under Node)
make lint              # ./mill __.checkFormat, then __.fix --check: CI's lint step
TAIHEI_TEST_JAVA_HOME=<a JDK 17> make test-java-17   # the JVM tests on Java 17
make publish-dry-run   # the signed bundle, written to out/ (key from MILL_PGP_SECRET_BASE64)
```

Only the maintainer releases (ADR 0014): he signs and pushes a `v`-tag on the commit that holds
its `RELEASE_x_y_z.md`, and `.github/workflows/release.yml` calls the family's release workflow,
pinned to a full commit of `taiheiframework/release`, which runs `validate`, `test-java-17` and
`publish-dry-run` from this Makefile and waits for his approval before it signs and publishes. An
agent writes the notes through the merge gate and never creates or pushes a release tag. The
next tag of a line waits until the previous one's GitHub Release is published or its run failed:
MiMa skips a tag without an immutable release, an unfinished one included. Moving the pin is a
reviewed change. The family's Maven repository (`REPO_URL`, the same string as
`release.repoUrl` in build.mill) serves every release from its immutable GitHub Release. Maven
Central is frozen: no version of this line goes there, except a security fix of a line whose users
are on Central, from the same bytes. Credentials never go on make's command line and are not
exported in the shell you build in; never read or print `~/.edo-release.env`.

## Layout

- `shared/src/dev/taihei/mihon/`: `Mihon` (a class bound to one language, created by `seeded` or
  `secure`), `Person`, `Address`, `Gender`, `SampleImages`; the internal data model in `data/`
  and the small internal lists in `sources/`.
- `shared/resources/dev/taihei/mihon/data/`: the language data, the only copy. The JVM reads it
  from the classpath; for Scala.js the build generates `EmbeddedData` from it, after checking
  every file against its schema (`EmbeddedData` in `build.mill`).
- `scripts/data/`: the scripts that build the data (`python3 scripts/data/build.py <tag>`), never
  run by the build; downloads go to a cache outside the repo and are recorded with their SHA-256
  in `scripts/data/sources.lock.json`. Sources and licences: `docs/data-sources.md` and NOTICE.
  A new source needs the CISO's licence ruling before its data is committed.
- `jvm/src/…` and `js/src/…`: the platform `ResourceLoader`.
- `shared/test/src/…`: zio-test specs, run on both platforms. `SeededSpec` pins a seed's values
  per language, `LanguagesSpec` the conventions of each language, `DataSpec` that both platforms
  load exactly the files on disk. A change to the data or a draw updates `SeededSpec`.
- `budget/src/`: the Scala.js fixture of the size budget, never published.
- `docs/skill/`: the API reference for the library's Claude Code skill.

## Rules

- Whatever is in `shared/` compiles on Scala.js: no JVM-only API there.
- Java 17 is the JVM floor: the JVM modules compile with `-java-output-version:17`, and CI runs
  the JVM tests on Java 17 too.
- No global generator and no wall clock: every draw goes through the instance's `Random`, using
  only `nextInt`, `nextInt(n)`, `nextLong`, `nextBoolean` and `nextDouble`, so a seed gives the
  same values on both platforms.
- The public API is `Mihon`, `Person`, `Address`, `Gender` and `SampleImages`; everything else is
  `private[mihon]`. It follows semantic versioning; a break is a major release with a migration
  section in the README.
- Every dependency comes from Maven Central under an Apache-compatible licence (a dev.taihei
  version that is not on Central from `REPO_URL`); the build needs no credentials. taihei-tsuji is
  pinned in `V.taiheiTsuji` and released first.
- Data is generic (common names, real cities, words, MIME types), every piece from a source named
  in `docs/data-sources.md` under an Apache-compatible licence: nothing taken from a client or a
  person, nothing CC BY-SA, no sentence from any source.
- No runtime case mapping, transliteration or locale lookup: what a value needs is in the data.
- Work is tracked in Jira project TAIHEI; commits are `TAIHEI-12: subject` with a body that explains the
  change to a reader without access to the ticket.

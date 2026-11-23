# Contributing to Taihei Mihon

Thank you for helping. This file says how to build the library, how to send a change, and what
the sign-off on every commit means.

## Build

You need a JDK (CI uses 25) and Node.js (for the Scala.js tests). Everything resolves from Maven
Central; no credentials are needed.

```sh
make fix && make fmt   # scalafix and scalafmt rewrite the sources
make validate          # checks scalafix and scalafmt, compile, test and MiMa, on the JVM and Scala.js
```

`make validate` must pass before a change is merged. A change that breaks binary compatibility
with the latest release fails the MiMa step; such a change needs a major release and is discussed
in an issue first.

## Sending a change

1. Open an issue first for anything larger than a small fix, so we can agree on the approach.
2. Fork the repository and make your change on a branch, with tests that run on both platforms
   (`shared/test/`).
3. Open a pull request against `main`. Describe what changes and why.

Code in `shared/` compiles on the JVM and on Scala.js, so it uses no JVM-only API. Public types
and methods carry Scaladoc. Data in the word lists stays generic: no real person's details.

## Developer Certificate of Origin

Every commit carries a `Signed-off-by:` line with your name and e-mail, matching the commit's
author:

```
Signed-off-by: Jane Doe <jane@example.org>
```

With it you certify the [Developer Certificate of Origin](https://developercertificate.org): that
you wrote the change or otherwise have the right to submit it under the project's licence, the
Apache License 2.0. `git commit -s` adds the line. A check on every pull request refuses commits
without it.

## Security

Do not report a vulnerability in a public issue. Write to security@taihei.dev instead. For
anything else, open an issue or write to hello@taihei.dev.

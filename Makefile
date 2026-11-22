SHELL = /bin/bash

# The library's artifacts and the modules that publish them.
ARTIFACTS = taihei-mihon_3 taihei-mihon_sjs1_3
PUBLISHED = mihon.js.publishArtifacts mihon.jvm.publishArtifacts
BUNDLE_PREFIX = dev.taihei-taihei-mihon

# A version is released by the maintainer's signed v-tag: the caller .github/workflows/release.yml
# runs the family's release workflow, which calls validate, test-java-17 and publish-dry-run here
# (ADR 0014). Nothing in this Makefile tags, signs for real, uploads or publishes.
# Plain assignments, not ?=: an environment variable of the same name does not point repo-guard at
# another repository or key. A command-line override still does (the tests use one).
# The family's Maven repository: the one repository the build adds after Maven Central, the same
# string as release.repoUrl in build.mill (repo-guard checks that they agree). It answers 404
# for a directory, so every check asks for a file.
REPO_URL = https://taiheiframework.github.io/repo/
CENTRAL = https://repo1.maven.org/maven2
GITHUB_API = https://api.github.com
# The family's libraries: a dev.taihei artifact taihei-<name>... is released in the repository
# OWNER/<name>, on an immutable GitHub Release of its tag (repo-guard checks the files the build
# resolves against it).
OWNER = taiheiframework
FAMILY = tsuji mihon
# The release key's public key, which repo-guard checks the signatures of resolved files with.
RELEASE_KEY = 5026B2996DDB697EC4630BCF14A733E2A507702F
RELEASE_KEY_FILE = $(CURDIR)/keys/release-key.asc
# Where publish-dry-run writes the bundle (Mill empties it before each run). A named bundle is in
# its own subdirectory, <bundle name>/, in Maven layout: that tree is what the release signs.
DRY_RUN_DIR = $(CURDIR)/out/mill.javalib.SonatypeCentralPublishModule/publishAll.dest/repository
# The build files, which repo-guard scans for repositories.
BUILD_FILES = '*.mill' '*.mill.yaml' '*.mill.scala' '*.sc' mill-build .mill-version .mill-jvm-opts .mill-opts .mill-repositories .config ':(exclude)out'

# Needs URL. The HTTP status of a GET of $$URL without credentials, without following a redirect
# (a redirect is an answer that stops a check), 000 when there is no answer.
HTTP_CODE = { curl -q -s --proto '=https' --max-time 30 -o /dev/null -w '%{http_code}' "$$URL" || true; }

.PHONY: clean fix fmt lint compile prepare build test test-java-17 mima budget data-check-test repo-guard release-test validate \
        deps publishLocal publish-dry-run

# ---------------------------------------------------------------------------
# Build (cross-platform: __ covers the JVM and Scala.js modules)
# ---------------------------------------------------------------------------

clean:
	./mill clean

fix:
	./mill __.fix

fmt:
	./mill __.reformat

# The checks CI runs, after fix and fmt have rewritten the sources: when scalafmt and scalafix
# disagree on a line, the one that ran last wins locally and the other fails in CI, so validate
# checks with both instead of trusting the rewrite.
lint:
	./mill __.checkFormat
	./mill __.fix --check

compile:
	./mill __.compile

prepare: fix fmt compile

build: prepare test

# __.test.testForked resolves to both mihon.jvm.test and mihon.js.test
# (JS tests run under Node).
test:
	./mill __.test.testForked

# Binary compatibility with the baseline release (release.mimaBaseline: the newest v-tag before
# HEAD that has an immutable GitHub Release, or MIMA_BASELINE). A tag without one is spent and
# skipped. A break fails here; a major sets MIMA_BASELINE=none.
mima:
	@set -eo pipefail; BASELINE=$$(./mill show release.mimaBaseline | tr -d '\n "' | sed 's/^null$$/none/'); \
	echo "[mima] baseline: $$BASELINE"
	./mill __.mimaReportBinaryIssues

# An open repository resolves from Coursier's defaults (the local Ivy repository, then Maven
# Central), then the family's repository for dev.taihei alone, and nothing else (ADR 0004,
# section 5), so anyone can clone and build it without credentials, a pin moves only to a
# released version, and nothing but a released dev.taihei file comes from REPO_URL:
# 1. no Coursier setting in the environment adds a repository, and the build files name REPO_URL
#    once, as build.mill's release.repoUrl, add it once, after Coursier's defaults, and name no
#    other repository (release.py build-repos);
# 2. Mill's own `repositories`, which would come before Central, is empty, and every module
#    resolves from exactly Coursier's defaults followed by REPO_URL, which build.mill asks for
#    dev.taihei alone; a module that resolves dependencies without reporting its repositories
#    (build.mill's TaiheiRepositories) is refused, published or not;
# 3. every dev.taihei dependency of a published module is on Maven Central or on REPO_URL. A
#    dependency on another module of this library is the same release, and needs the same
#    version;
# 4. every file the build resolved came from Maven Central or REPO_URL, nothing but dev.taihei is
#    in the Coursier cache of REPO_URL, and every dev.taihei file, with its POM, is in the
#    SHA256SUMS of its version's immutable GitHub Release and signed by the release key, or, for
#    a version without a release (before this route, or a security fix before its advisory),
#    comes from Central with a good signature by the release key (release.py resolved). The
#    GitHub API is asked without a token.
# Each check asks for a file, never a directory, and a redirect stops it. The text scan reads
# tracked build files; a release builds a fresh checkout of its tag, and a Coursier mirror file in
# the user's home is outside this repository. CI on a clean runner, with no credentials, stays the
# proof that the build needs none.
# The resolved classpaths repo-guard reads (Mill's own tasks; a module lists those it has).
RESOLVED = resolvedMvnDeps,resolvedRunMvnDeps,scalaCompilerClasspath,scalacPluginClasspath,scalaDocClasspath
RESOLVED := $(RESOLVED),scalaDocPluginClasspath,semanticDbPluginClasspath,resolvedMimaPreviousArtifacts
RESOLVED := $(RESOLVED),scalaJSToolsClasspath,scalaJSWorkerClasspath,scalaJSLinkerClasspath,scalaJSJsEnvMvnDeps
RESOLVED := $(RESOLVED),scalafixToolClasspath,pgpWorkerClasspath,scalaCompilerBridge
RESOLVED := $(RESOLVED),resolvedSemanticDbJavaPluginMvnDeps,annotationProcessorsResolvedMvnDeps
repo-guard:
	@set -euo pipefail; \
	if [ -n "$${COURSIER_REPOSITORIES:-}$${COURSIER_MIRRORS:-}$${COURSIER_CREDENTIALS:-}" ]; then \
		echo "[repo-guard] COURSIER_REPOSITORIES, COURSIER_MIRRORS or COURSIER_CREDENTIALS is set: this build resolves from Maven Central and $(REPO_URL) only, unset them"; exit 1; fi; \
	LIST=$$(git ls-files -- $(BUILD_FILES) .github); mapfile -t FILES <<< "$$LIST"; \
	python3 -I -B scripts/release.py build-repos --url "$(REPO_URL)" "$${FILES[@]}"; \
	MODULES="$(PUBLISHED)"; MODULES="$${MODULES//.publishArtifacts/}"; \
	T=$$(mktemp -d "$(CURDIR)/.repo-guard.XXXXXX"); trap 'rm -rf "$$T"' EXIT; \
	./mill show "{$${MODULES// /,}}.repositories" > "$$T/repositories.json"; \
	python3 -I -B scripts/release.py no-repos "$$T/repositories.json"; \
	./mill show '__.resolverOrder' > "$$T/resolvers.json"; \
	./mill resolve '__.resolvedMvnDeps' > "$$T/resolving"; \
	python3 -I -B scripts/release.py resolvers --url "$(REPO_URL)" --resolving "$$T/resolving" --modules "$$MODULES" "$$T/resolvers.json"; \
	./mill show "{$${MODULES// /,}}.pom" > "$$T/poms.json"; \
	python3 -I -B -c 'import json, sys; [print(v.split(":", 3)[3]) for v in json.load(open(sys.argv[1])).values()]' "$$T/poms.json" > "$$T/poms"; \
	mapfile -t POMS < "$$T/poms"; \
	python3 -I -B scripts/release.py pins --own "$(ARTIFACTS)" "$${POMS[@]}" | sort -u > "$$T/pins"; \
	while read -r A V; do \
		URL="$(CENTRAL)/dev/taihei/$$A/$$V/$$A-$$V.pom"; CODE=$$($(HTTP_CODE)); \
		case "$$CODE" in \
			200) echo "[repo-guard] dev.taihei:$$A:$$V is on Maven Central"; continue;; \
			404) ;; \
			*) echo "[repo-guard] cannot tell whether dev.taihei:$$A:$$V is on Maven Central (HTTP $$CODE)"; exit 1;; \
		esac; \
		URL="$(REPO_URL)dev/taihei/$$A/$$V/$$A-$$V.pom"; CODE=$$($(HTTP_CODE)); \
		case "$$CODE" in \
			200) echo "[repo-guard] dev.taihei:$$A:$$V is on $(REPO_URL)";; \
			404) echo "[repo-guard] dev.taihei:$$A:$$V is neither on Maven Central nor on $(REPO_URL): a pin moves only to a released version"; exit 1;; \
			*) echo "[repo-guard] cannot tell whether dev.taihei:$$A:$$V is on $(REPO_URL) (HTTP $$CODE)"; exit 1;; \
		esac; \
	done < "$$T/pins"; \
	./mill show '__.{$(RESOLVED)}' > "$$T/resolved.json"; \
	python3 -I -B scripts/release.py resolved --workspace "$(CURDIR)" --url "$(REPO_URL)" --central "$(CENTRAL)" --api "$(GITHUB_API)" \
		--owner "$(OWNER)" --family "$(FAMILY)" --key "$(RELEASE_KEY_FILE)" --fingerprint "$(RELEASE_KEY)" "$$T/resolved.json" | sed 's/^/[repo-guard] /'; \
	echo "[repo-guard] Maven Central, then $(REPO_URL), and nothing else"

# The tests of scripts/release.py, repo-guard and publish-dry-run, against stand-ins for Maven
# Central, the family's repository, the GitHub API and Mill on localhost. No real registry, no
# token and no signing key is involved. Then the mill launcher's pinned SHA-256: a tampered or
# unpinned Mill, downloaded or already cached, is refused.
release-test:
	@bash scripts/test-release.sh
	@bash scripts/test-mill-launcher

# The size budget (TAIHEI-40): data per language and in all, the JVM jar and the full-linked
# Scala.js of a fixture that uses every language. Over budget fails.
budget:
	./mill mihon.budget

# The build's data check refuses what it must (links, stray files, malformed data, over budget).
data-check-test:
	@bash scripts/test-data-check.sh

validate: repo-guard release-test data-check-test fix fmt lint compile test mima budget

# The JVM tests on Java 17, the oldest Java the library supports (scalac already compiles against
# its API). Mill runs on its own JVM, so TAIHEI_TEST_JAVA_HOME, a JDK 17's home, makes the test
# module fork on it; the target refuses to run them on anything else.
test-java-17:
	@set -euo pipefail; \
	J="$${TAIHEI_TEST_JAVA_HOME:-}"; \
	if [ -z "$$J" ]; then echo "[test-java-17] set TAIHEI_TEST_JAVA_HOME to the home of a JDK 17"; exit 1; fi; \
	JV=$$("$$J/bin/java" -version 2>&1) || { echo "[test-java-17] $$J/bin/java does not run"; exit 1; }; \
	if ! grep -q 'version "17\.' <<< "$$JV"; then echo "[test-java-17] $$J is not a JDK 17:"; echo "$$JV"; exit 1; fi; \
	SHOWN=$$(./mill show mihon.jvm.test.javaHome); \
	if ! grep -qF "$$J" <<< "$$SHOWN"; then echo "[test-java-17] the test module does not fork on $$J"; exit 1; fi; \
	./mill mihon.jvm.test.testForked

deps:
	./mill mill.scalalib.Dependency/showUpdates

# ---------------------------------------------------------------------------
# Publishing (namespace dev.taihei): the dry run
# ---------------------------------------------------------------------------

# A release number in ~/.ivy2/local shadows the published artifact for every build on the machine
# (Coursier reads it first). So a publishLocal at a release tag on a clean tree is refused; a
# SNAPSHOT publishLocal stays.
publishLocal:
	@if [ -z "$$(git status --porcelain | grep -v '^??')" ] && git describe --exact-match --tags --match 'v[0-9]*' HEAD >/dev/null 2>&1; then \
	  echo "[publishLocal] refused: HEAD is release tag $$(git describe --exact-match --tags HEAD); release it instead"; \
	  exit 1; \
	fi
	./mill __.publishLocal

# The bundle, signed, written to out/ instead of uploaded: what the release workflow signs again
# and publishes. Mill names a bundle only for a release version, so a SNAPSHOT dry run writes it
# unnamed. The signing key comes from MILL_PGP_SECRET_BASE64 and MILL_PGP_PASSPHRASE in the
# environment (the workflow's build job hands it a throwaway key). Only MILL_TESTS_PUBLISH_DRY_RUN,
# a test hook of Mill's, keeps this from uploading: Mill, which wants credentials before it looks
# at the hook, gets a placeholder that is no token, and the older names it also reads are unset,
# so a Mill that no longer honours the hook fails to authenticate instead of publishing.
publish-dry-run:
	@set -e; \
	unset SONATYPE_USERNAME SONATYPE_PASSWORD; \
	VERSION=$$(./mill --no-daemon show release.gitVersion | tr -d '"'); \
	case "$$VERSION" in *-SNAPSHOT) BUNDLE=();; *) BUNDLE=(--bundleName "$(BUNDLE_PREFIX)-$$VERSION");; esac; \
	MILL_TESTS_PUBLISH_DRY_RUN=1 MILL_SONATYPE_USERNAME=dry-run-no-upload MILL_SONATYPE_PASSWORD=dry-run-no-upload \
		./mill --no-daemon mill.javalib.SonatypeCentralPublishModule/ "$${BUNDLE[@]}"; \
	echo "[publish-dry-run] bundle in $(DRY_RUN_DIR)"

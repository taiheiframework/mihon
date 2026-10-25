#!/usr/bin/env bash
# Proves that the build's data check (EmbeddedData.check in build.mill) accepts the data as it is
# and refuses each kind of file it must: a link, a file or folder that is no language's, a missing
# file, a malformed or unknown field, a weight that is not positive, a name without an ASCII form,
# an unknown placeholder, and a language over its size budget. Each case runs on a copy of
# shared/resources in a temporary folder; nothing in the repository changes.
set -euo pipefail
cd "$(dirname "$0")/.."
T=$(mktemp -d "${TMPDIR:-/tmp}/mihon-data-check.XXXXXX")
trap 'rm -rf "$T"' EXIT
D=dev/taihei/mihon/data
FAILED=0

fresh() { rm -rf "$T/r"; cp -R shared/resources "$T/r"; }
check() { ./mill --ticker false mihon.checkData --folder "$T/r" > "$T/out" 2>&1; }
edit() { python3 -I -B -c "import json,sys; p=sys.argv[1]; d=json.load(open(p)); exec(sys.argv[2]); open(p,'w').write(json.dumps(d, ensure_ascii=False))" "$T/r/$D/$1" "$2"; }

accepts() {
  fresh; if check; then echo "[ok] accepts the data as it is"; else echo "[FAIL] refuses the data as it is"; cat "$T/out"; FAILED=1; fi
}
refuses() { # name, expected message, setup...
  local name="$1" expected="$2"; shift 2
  fresh; "$@"
  if check; then echo "[FAIL] accepts $name"; FAILED=1
  elif grep -qF -- "$expected" "$T/out"; then echo "[ok] refuses $name"
  else echo "[FAIL] refuses $name, but not with: $expected"; tail -5 "$T/out"; FAILED=1; fi
}

accepts
refuses "a symlink to a file outside" "a link, or a folder above it" ln -s /etc/hostname "$T/r/$D/en/x.json"
refuses "a symlinked data file" "a link" bash -c "mv '$T/r/$D/en/text.json' '$T/text.json' && ln -s '$T/text.json' '$T/r/$D/en/text.json'"
refuses "a symlinked language folder" "a link, or a folder above it" bash -c "mv '$T/r/$D/la' '$T/la' && ln -s '$T/la' '$T/r/$D/la'"
refuses "a symlinked data folder" "a link, or a folder above it" bash -c "mv '$T/r/$D' '$T/data' && ln -s '$T/data' '$T/r/$D'"
refuses "a symlinked folder above the data" "a link, or a folder above it" bash -c "mv '$T/r/dev/taihei' '$T/taihei' && ln -s '$T/taihei' '$T/r/dev/taihei'"
refuses "a file outside the data folder" "only dev/taihei/mihon/data/ belongs" touch "$T/r/stray.txt"
refuses "an extra file in a language" "not a data file of a listed language" touch "$T/r/$D/en/notes.json"
refuses "a folder of no language" "not the folder of a listed language" mkdir "$T/r/$D/xx"
refuses "a missing file" "missing" rm "$T/r/$D/la/names.json"
refuses "an unknown field" "unknown extra" edit en/text.json "d['extra'] = 1"
refuses "a missing field" "missing comma" edit en/text.json "del d['comma']"
refuses "a weight of zero" "weight: not an integer" edit en/text.json "d['words'][0][1] = 0"
refuses "weights beyond Int.MaxValue" "add up beyond" edit en/text.json "d['words'][0][1] = 2147483647; d['words'][1][1] = 2147483647"
refuses "more than 2000 words" "at most 2000" edit en/text.json "d['words'] = [['w%d' % i, 1] for i in range(2001)]"
refuses "an initial without its capital" "no capital for the initial" edit en/text.json "d['capitals'] = [c for c in d['capitals'] if c[0] != 't']"
refuses "a control character" "a control character" edit en/names.json "d['male'][0]['name'] = 'A\u0007'"
refuses "a name with a bidi override" "an invisible formatting character" edit en/names.json "d['male'][0]['name'] = 'Ja\u202emes'"
refuses "a city with a zero-width space" "an invisible formatting character" edit en/country-US.json "d['cities'][0]['name'] = 'New\u200bYork'"
refuses "a name without an ASCII form" "no ASCII form" edit en/names.json "d['male'][0]['name'] = 'Zoë'"
refuses "an unknown street placeholder" "unknown placeholders" edit en/country-US.json "d['streets'][0] = '{secret}'"
refuses "an unknown address field" "unknown placeholders" edit en/country-US.json "d['lines'][0] = '{password}'"
refuses "a city in no listed region" "not one of regions" edit en/country-US.json "d['cities'][0]['region'] = 'Atlantis'"
refuses "city weights beyond Int.MaxValue" "add up beyond" edit en/country-US.json "d['cities'][0]['weight'] = 2147483647; d['cities'][1]['weight'] = 2147483647"
refuses "a phone pattern without a calling code" "phone: not" edit en/country-US.json "d['phone'] = '555-0100'"
refuses "an alias of no language" "the target is not a language" edit languages.json "d['aliases'] = [['xx', 'y']]"
refuses "an alias equal to a language's tag" "a language's own tag" edit languages.json "d['aliases'].append(['en', 'la'])"
refuses "an alias listed twice" "an alias listed twice" edit languages.json "d['aliases'].append(d['aliases'][0])"
refuses "unsorted countries" "not sorted" edit en/countries.json "d.reverse()"
refuses "a language over its budget" "more than the budget" edit en/text.json "d['words'] = [['w%d' % i + 'x' * 40, 1] for i in range(2000)]"

exit $FAILED

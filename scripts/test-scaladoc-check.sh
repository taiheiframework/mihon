#!/usr/bin/env bash
# Proves that scripts/scaladoc-check.py accepts the sources as they are and refuses a public
# declaration without a Scaladoc, naming its file and line: one taken out of a copy of Mihon.scala,
# and each kind of declaration in small fixtures. Private, protected, local and exempt
# declarations pass. Every case runs in a temporary folder; nothing in the repository changes.
set -euo pipefail
cd "$(dirname "$0")/.."
T=$(mktemp -d "${TMPDIR:-/tmp}/mihon-scaladoc-check.XXXXXX")
trap 'rm -rf "$T"' EXIT
FAILED=0

check() { python3 -I -B scripts/scaladoc-check.py "$@" > "$T/out" 2>&1; }

accepts() { # name, paths...
  local name="$1"; shift
  if check "$@"; then echo "[ok] accepts $name"; else echo "[FAIL] refuses $name"; cat "$T/out"; FAILED=1; fi
}
refuses() { # name, expected message, paths...
  local name="$1" expected="$2"; shift 2
  if check "$@"; then echo "[FAIL] accepts $name"; cat "$T/out"; FAILED=1
  elif grep -qF -- "$expected" "$T/out"; then echo "[ok] refuses $name"
  else echo "[FAIL] refuses $name, but not with: $expected"; cat "$T/out"; FAILED=1; fi
}
fixture() { mkdir -p "$T/$1"; cat > "$T/$1/A.scala"; }

accepts "the sources as they are" shared/src jvm/src js/src
check shared/src jvm/src js/src || true
grep -qE '^\[scaladoc\] [1-9][0-9]+ public declarations' "$T/out" \
  || { echo "[FAIL] the sources have too few public declarations to prove anything:"; cat "$T/out"; FAILED=1; }

# Mihon.bool without its one-line Scaladoc: the message names the file and the line of the def.
mkdir "$T/mihon"
grep -vF '/** True or false, each equally likely. */' shared/src/dev/taihei/mihon/Mihon.scala > "$T/mihon/Mihon.scala"
LINE=$(grep -nF 'def bool: Boolean' "$T/mihon/Mihon.scala" | cut -d: -f1)
refuses "Mihon.bool without its Scaladoc" "$T/mihon/Mihon.scala:$LINE: public def bool has no Scaladoc" "$T/mihon"

mkdir "$T/empty"
refuses "no Scala file" "no Scala file" "$T/empty"

fixture top <<'EOF'
package p

class Plain
EOF
refuses "an undocumented top-level class" "A.scala:3: public class Plain" "$T/top/A.scala"

fixture member <<'EOF'
package p

/** Documented. */
object O:

  /** Documented. */
  def documented: Int = 1

  def size: Int = 2

  /** Documented. */
  object Inner:
    lazy val deep: Int = 3
EOF
refuses "an undocumented member" "A.scala:9: public def size" "$T/member/A.scala"
refuses "an undocumented member of a nested object" "A.scala:13: public val deep" "$T/member/A.scala"

fixture enum <<'EOF'
package p

/** Documented. */
enum Colour:

  /** Documented. */
  case Red

  case Green
EOF
refuses "an undocumented enum case" "A.scala:9: public case Green" "$T/enum/A.scala"

fixture blank <<'EOF'
package p

/** Detached by a blank line. */

trait T
EOF
refuses "a Scaladoc detached by a blank line" "A.scala:5: public trait T" "$T/blank/A.scala"

fixture plain-comment <<'EOF'
package p

/* Not a Scaladoc. */
type Id = String
EOF
refuses "a block comment that is not a Scaladoc" "A.scala:4: public type Id" "$T/plain-comment/A.scala"

fixture override <<'EOF'
package p

/** Documented. */
final class C:
  override def size: Int = 1
EOF
refuses "an override of another member" "A.scala:5: public def size" "$T/override/A.scala"

fixture fine <<'EOF'
package p

/**
 * Documented, with an example that holds a declaration:
 *
 * {{{
 * def example = 1
 * }}}
 */
final class C private[p] (
  x: Int,
  y: Int):

  /** Documented, an annotation between. */
  @deprecated("use another", "1.0")
  def old: Int = x

  /** A body after a continuation line keeps its local values local. */
  def sum(
    a: Int,
  ): Int =
    val local = a + y
    local

  private[p] def internal: Int = x
  protected def forSubclasses: Int = x
  private val hidden = """
def notADeclaration = 1
"""

  /** Documented. */
  override def equals(other: Any): Boolean = other.isInstanceOf[C]
  override def hashCode: Int = x
  override def toString: String = "C"

  /** Documented. */
  def matched(n: Int): String = n match
    case 1 => "one"
    case _ => "more"

end C

private[p] object Internal:
  def undocumented: Int = 1
  object Nested:
    val alsoUndocumented: Int = 2
EOF
accepts "private, protected, local and exempt declarations" "$T/fine/A.scala"

exit $FAILED

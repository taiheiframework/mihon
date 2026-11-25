"""Fails on a public declaration without a Scaladoc in the given Scala sources.

  python3 -I -B scaladoc-check.py <file or folder>...

The library promises a Scaladoc on every public type and member. This reads the sources line by
line, relying on the layout scalafmt keeps: a declaration's body is indented below it. A
declaration (def, val, var, class, object, trait, enum, type, given, an enum case) is checked when
it is a member of a type or a top-level definition, not a local one inside a method or a value,
and public: neither it nor anything around it is private or protected. Its Scaladoc, a /** */
comment, ends on the line right above it; annotations may stand between. An override of equals,
hashCode or toString inherits the contract of Any and needs none. Constructor parameters are
documented with @param and are not checked.

Prints one "<file>:<line>: ..." per missing Scaladoc and exits 1 when there is one, or when no
Scala file is found (a check over nothing proves nothing).
"""

import os
import re
import sys

MODIFIERS = {
    "final", "sealed", "abstract", "override", "implicit", "lazy", "inline", "transparent",
    "opaque", "open", "infix", "case",
}
HIDDEN = re.compile(r"^(private|protected)(\[\w+\])?$")
TYPES = {"class", "object", "trait", "enum"}
KEYWORDS = TYPES | {"def", "val", "var", "type", "given", "extension"}
EXEMPT = {"equals", "hashCode", "toString"}


def declaration(text, in_enum):
    """The (keyword, name, modifiers) a code line declares, or None."""
    tokens = text.split()
    mods = []
    i = 0
    while i < len(tokens) and (tokens[i] in MODIFIERS or HIDDEN.match(tokens[i])):
        mods.append(tokens[i])
        i += 1
    keyword = tokens[i] if i < len(tokens) else ""
    if keyword in KEYWORDS:
        rest = tokens[i + 1] if i + 1 < len(tokens) else ""
    elif in_enum and mods and mods[-1] == "case" and keyword:
        # An enum case: "case Female", "case Red, Green" or "case Rgb(r: Int)".
        keyword, rest = "case", keyword
        mods = mods[:-1]
    else:
        return None
    name = re.split(r"[(\[:=,{]", rest, maxsplit=1)[0] or "(anonymous)"
    return keyword, name, mods


def check(path):
    """The missing Scaladocs of one file, and how many public declarations it has."""
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")
    missing = []
    public_count = 0
    # Each open scope: (indent, kind, public); kind is "members", "enum" or "local".
    scopes = []
    in_comment = comment_is_doc = in_string = False
    doc = False  # a Scaladoc ends right above the current line
    for number, raw in enumerate(lines, 1):
        text = raw.strip()
        if in_string:
            in_string = text.count('"""') % 2 == 0
            doc = False
            continue
        if in_comment:
            if "*/" in text:
                in_comment = False
                doc = comment_is_doc and text.endswith("*/")
            continue
        if not text:
            doc = False
            continue
        if text.startswith("/*"):
            is_doc = text.startswith("/**") and not text.startswith("/**/")
            if "*/" in text[2:]:
                doc = is_doc and text.endswith("*/")
            else:
                in_comment, comment_is_doc = True, is_doc
            continue
        if text.startswith("//"):
            doc = False
            continue
        if text.startswith("@"):
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        # The scopes this line would close. Only a declaration closes them: a continuation such
        # as "): Mihon =" stands at its definition's indent while the body below still belongs
        # to that definition.
        outer = [s for s in scopes if s[0] < indent]
        kind, public = outer[-1][1:] if outer else ("members", True)
        found = declaration(text, kind == "enum") if kind != "local" else None
        if found:
            scopes = outer
            keyword, name, mods = found
            visible = public and not any(HIDDEN.match(m) for m in mods)
            exempt = "override" in mods and name in EXEMPT
            if visible and keyword != "extension" and not exempt:
                public_count += 1
                if not doc:
                    missing.append(f"{path}:{number}: public {keyword} {name} has no Scaladoc")
            if keyword == "enum":
                opens = "enum"
            elif keyword in TYPES or keyword == "extension":
                opens = "members"
            elif keyword == "given" and text.endswith(" with"):
                opens = "members"
            else:
                opens = "local"
            if keyword != "case":
                scopes.append((indent, opens, visible))
        in_string = text.count('"""') % 2 == 1
        doc = False
    return missing, public_count


def scala_files(arg):
    if os.path.isfile(arg):
        return [arg]
    found = []
    for folder, dirs, files in os.walk(arg):
        dirs.sort()
        found += [os.path.join(folder, f) for f in sorted(files) if f.endswith(".scala")]
    return found


def main(args):
    files = [f for arg in args for f in scala_files(arg)]
    if not files:
        print(f"[scaladoc] no Scala file in {' '.join(args)}")
        return 1
    missing = []
    total = 0
    for path in files:
        m, n = check(path)
        missing += m
        total += n
    for line in missing:
        print(f"[scaladoc] {line}")
    if missing:
        print(f"[scaladoc] {len(missing)} of {total} public declarations have no Scaladoc")
        return 1
    print(f"[scaladoc] {total} public declarations in {len(files)} files, each with a Scaladoc")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

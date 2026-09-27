#!/bin/sh
# init.sh — render this KMP template into a real project.
#
#   sh scripts/init.sh <name> [--group <maven.group>] [--org <github-org>] \
#                             [--display-name <Display Name>]
#
# Or, via mise:
#   mise run init <name> [--group g] [--org o] [--display-name 'Name']
#
# What it does, in order:
#   1. Validate args.
#   2. Rename the module directories (src -> <name>, src-testing -> <name>-testing)
#      and the Kotlin package dirs (com/happycodelucky/src -> com/<group>/<name>).
#   3. Rename the convention-plugin files (template.* -> <name>.*).
#   4. Replace tokens + the group/org defaults across the tree.
#   5. Strip the [tasks.init] block from mise.toml.
#   6. Delete the template-only files listed in scripts/template-manifest.txt.
#   7. Remove itself (scripts/) and TEMPLATE.md.
#   8. Print next steps.
#
# POSIX sh. No `sed -i` (BSD vs GNU differ) — rewrites go through a temp file.
# Idempotent guard: aborts if scripts/template-manifest.txt is gone (already
# rendered).

set -eu

# --- 0. Must run from the template root -------------------------------------
if [ ! -f scripts/template-manifest.txt ]; then
    echo "error: scripts/template-manifest.txt not found." >&2
    echo "       Run this from the template root, and only on an un-rendered template" >&2
    echo "       (it is removed after the first render)." >&2
    exit 1
fi

# --- 1. Parse args ----------------------------------------------------------
NAME=""
GROUP="com.happycodelucky"
ORG="happycodelucky"
DISPLAY=""

while [ $# -gt 0 ]; do
    case "$1" in
        --group)        GROUP="${2:-}"; shift 2 ;;
        --group=*)      GROUP="${1#--group=}"; shift ;;
        --org)          ORG="${2:-}"; shift 2 ;;
        --org=*)        ORG="${1#--org=}"; shift ;;
        --display-name) DISPLAY="${2:-}"; shift 2 ;;
        --display-name=*) DISPLAY="${1#--display-name=}"; shift ;;
        -h|--help)
            grep '^#' "$0" | sed 's/^# \{0,1\}//'
            exit 0 ;;
        --*)
            echo "error: unknown flag '$1'" >&2; exit 1 ;;
        *)
            if [ -z "$NAME" ]; then NAME="$1"; shift
            else echo "error: unexpected argument '$1'" >&2; exit 1; fi ;;
    esac
done

if [ -z "$NAME" ]; then
    echo "error: <name> is required." >&2
    echo "usage: sh scripts/init.sh <name> [--group g] [--org o] [--display-name 'Name']" >&2
    exit 1
fi

# --- 2. Validate ------------------------------------------------------------
# Kebab-case: lowercase, starts with a letter, hyphens allowed, no trailing/
# leading/double hyphen.
if ! printf '%s' "$NAME" | grep -Eq '^[a-z][a-z0-9]*(-[a-z0-9]+)*$'; then
    echo "error: <name> must be kebab-case (lowercase, letter-first, single hyphens)." >&2
    echo "       Got: '$NAME'" >&2
    exit 1
fi
if ! printf '%s' "$GROUP" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z0-9_]+)*$'; then
    echo "error: --group must be a reverse-DNS-ish group id. Got: '$GROUP'" >&2
    exit 1
fi
if ! printf '%s' "$ORG" | grep -Eq '^[A-Za-z0-9][A-Za-z0-9-]*$'; then
    echo "error: --org must be a GitHub org/user handle. Got: '$ORG'" >&2
    exit 1
fi

# The framework/XCFramework module name is ALWAYS derived from <name> and must be
# an identifier (no spaces) — it matches the convention plugin's frameworkBaseName
# derivation (my-app -> MyAppKit). The `Kit` suffix keeps the Swift module name
# distinct from the library's public types: a `MyApp` type inside a `MyApp` module
# makes SKIE rename the type in Swift (`MyApp_`) — LESSONS D-002. The display name
# is free text (may contain spaces) and defaults to the plain name (MyApp).
PASCAL=$(printf '%s' "$NAME" | awk -F- '{ s=""; for (i=1;i<=NF;i++){ s = s toupper(substr($i,1,1)) substr($i,2) } print s }')
FRAMEWORK="${PASCAL}Kit"
if [ -z "$DISPLAY" ]; then
    DISPLAY="$PASCAL"
fi

# Collision guard.
if [ -e "$NAME" ] || [ -e "$NAME-testing" ]; then
    echo "error: '$NAME' or '$NAME-testing' already exists in this directory." >&2
    exit 1
fi

GROUP_PATH=$(printf '%s' "$GROUP" | tr '.' '/')
UPPER_NAME=$(printf '%s' "$NAME" | tr '[:lower:]-' '[:upper:]_')

# Kotlin packages can't contain hyphens. The convention plugin derives the
# namespace as `<group>.` + name.replace("-", "."), so a hyphenated <name>
# becomes a dotted package (my-cool-lib -> my.cool.lib). PKG_NAME is that dotted
# leaf; PKG_PATH is its directory form. For a single-word name these equal NAME.
PKG_NAME=$(printf '%s' "$NAME" | tr '-' '.')
PKG_PATH=$(printf '%s' "$NAME" | tr '-' '/')

echo "Rendering template:"
echo "  name         : $NAME"
echo "  display name : $DISPLAY"
echo "  framework    : $FRAMEWORK"
echo "  maven group  : $GROUP.$NAME"
echo "  github org   : $ORG"
echo ""

# --- 3. Rename module directories + Kotlin package dirs ---------------------
# The committed ABI dumps under <module>/api/ are named after the old module
# (src.klib.api) and carry the old library-unique-name. They're a regenerable
# baseline, so drop them; the rendered project recreates them with `mise run
# api:dump` (the "next steps" note below reminds the user).
rm -rf src/api src-testing/api
mv src "$NAME"
mv src-testing "$NAME-testing"

# Move the Kotlin package tree com/happycodelucky/src -> com/<group-path>/<name>.
# Done by relocating the leaf and pruning empty parents, for each module.
move_pkg() {
    # $1 = module dir, $2 = leaf suffix under the package root (e.g. "" or "/testing")
    module_dir="$1"
    # Find every source set's kotlin root that contains com/happycodelucky/src.
    find "$module_dir" -type d -path '*/kotlin/com/happycodelucky/src' 2>/dev/null | while IFS= read -r leaf; do
        kotlin_root="${leaf%/com/happycodelucky/src}"
        dest="$kotlin_root/$GROUP_PATH/$PKG_PATH"
        mkdir -p "$(dirname "$dest")"
        mv "$leaf" "$dest"
        # Prune now-empty com/happycodelucky (and com) if nothing else uses them.
        rmdir "$kotlin_root/com/happycodelucky" 2>/dev/null || true
        rmdir "$kotlin_root/com" 2>/dev/null || true
    done
}
move_pkg "$NAME"
move_pkg "$NAME-testing"
# The Android sample lives under apps/android/src/main/java/com/happycodelucky/src/...
find apps -type d -path '*/com/happycodelucky/src' 2>/dev/null | while IFS= read -r leaf; do
    java_root="${leaf%/com/happycodelucky/src}"
    dest="$java_root/$GROUP_PATH/$PKG_PATH"
    mkdir -p "$(dirname "$dest")"
    mv "$leaf" "$dest"
    rmdir "$java_root/com/happycodelucky" 2>/dev/null || true
    rmdir "$java_root/com" 2>/dev/null || true
done

# --- 4. Rename convention-plugin files --------------------------------------
PLUGIN_DIR="gradle/plugins/src/main/kotlin"
mv "$PLUGIN_DIR/template.kmp-library.gradle.kts" "$PLUGIN_DIR/$NAME.kmp-library.gradle.kts"
mv "$PLUGIN_DIR/template.publish.gradle.kts" "$PLUGIN_DIR/$NAME.publish.gradle.kts"

# --- 5. Token + literal replacement -----------------------------------------
# Replace across all tracked text files. Exclude .git, build dirs, the scripts/
# dir (deleted anyway; never self-edit a running script), and symlinks
# (AGENTS.md -> CLAUDE.md must not be followed/edited twice).
#
# Order matters: the dotted group (com.happycodelucky) is replaced before the
# bare org (happycodelucky), so the org pass never touches the group prefix.
replace_in_file() {
    file="$1"
    tmp="$file.__init_tmp__"
    # The Kotlin package is `com.happycodelucky.src[.testing]`. The trailing
    # `.src` segment is the module name, which becomes `.<name>` — replace these
    # compound forms BEFORE the generic group rule, so `com.happycodelucky.src`
    # -> `<group>.<name>` (not `<group>.src`). A namespace-only dot variant (e.g.
    # the convention plugin's `"com.happycodelucky." + name`) is covered by the
    # later generic group rule.
    # Replacement order is load-bearing:
    #   - Kotlin package compounds (com.happycodelucky.src[.testing|.example|.cli])
    #     before the generic group rule, so the trailing `.src` module segment
    #     becomes `.<name>`, not `.src`.
    #   - `build:src` is a mise TASK name (CLAUDE.md / spec keeps it literal). It
    #     is the only `:src` we must NOT touch, so we hide it behind a sentinel,
    #     rename every other `:src`/`:src-testing` Gradle path, then restore it.
    #   - dotted group (com.happycodelucky) before bare org (happycodelucky).
    sed \
        -e "s/__PROJECT_NAME__/$NAME/g" \
        -e "s/__DISPLAY_NAME__/$DISPLAY/g" \
        -e "s/__FRAMEWORK__/$FRAMEWORK/g" \
        -e "s/template\.kmp-library/$NAME.kmp-library/g" \
        -e "s/template\.publish/$NAME.publish/g" \
        -e "s/LIBRARY_VERSION/${UPPER_NAME}_VERSION/g" \
        -e "s/com\.happycodelucky\.src\.testing/$GROUP.$PKG_NAME.testing/g" \
        -e "s/com\.happycodelucky\.src\.example/$GROUP.$PKG_NAME.example/g" \
        -e "s/com\.happycodelucky\.src\.cli/$GROUP.$PKG_NAME.cli/g" \
        -e "s/com\.happycodelucky\.src/$GROUP.$PKG_NAME/g" \
        -e "s/com\.happycodelucky/$GROUP/g" \
        -e "s/happycodelucky/$ORG/g" \
        -e "s/build:src/build@@SRCTASK@@/g" \
        -e "s/:src-testing/:$NAME-testing/g" \
        -e "s/:src/:$NAME/g" \
        -e "s/build@@SRCTASK@@/build:src/g" \
        -e "s/assembleSrcKitXCFramework/assemble${FRAMEWORK}XCFramework/g" \
        -e "s#src-testing/build#$NAME-testing/build#g" \
        -e "s#src-testing/api#$NAME-testing/api#g" \
        -e "s#src/build#$NAME/build#g" \
        -e "s#src/api#$NAME/api#g" \
        -e "s/if-no-files-found: warn/if-no-files-found: error/g" \
        "$file" > "$tmp"
    # Only overwrite if something changed (keeps mtimes stable otherwise).
    if cmp -s "$file" "$tmp"; then rm -f "$tmp"; else mv "$tmp" "$file"; fi
}

# Walk regular files only, skipping binary/build/scratch dirs and symlinks.
# scripts/ is NOT pruned: scripts/release.sh and scripts/changeset.py survive the
# render and carry tokens (__FRAMEWORK__, __PROJECT_NAME__) that must be replaced.
# We skip only this running script (editing a script mid-execution is unsafe) and
# template-manifest.txt (deleted below).
find . \
    -type d \( -name .git -o -name build -o -name .gradle -o -name .kotlin -o -name '*.xcodeproj' \) -prune -o \
    -type f ! -name '*.jar' ! -name '*.png' ! -name '*.jpg' ! -name '*.zip' ! -name '*.keystore' -print | \
while IFS= read -r f; do
    # Skip symlinks (AGENTS.md). -type f above already excludes them on macOS/Linux,
    # but guard explicitly for portability.
    [ -L "$f" ] && continue
    case "$f" in
        ./scripts/init.sh|./scripts/template-manifest.txt) continue ;;
    esac
    replace_in_file "$f"
done

# --- 6. Strip the [tasks.init] block from mise.toml -------------------------
# Remove from the "# --- template rendering" banner (or the [tasks.init] header)
# through end-of-file. The init task is the last task in the file by design.
awk '
    /^# --- template rendering/ { skip=1 }
    /^\[tasks\.init\]/ { skip=1 }
    skip { next }
    { print }
' mise.toml > mise.toml.__init_tmp__ && mv mise.toml.__init_tmp__ mise.toml
# Trim any trailing blank lines left behind.
awk 'NF{p=NR} {a[NR]=$0} END{for(i=1;i<=p;i++) print a[i]}' mise.toml > mise.toml.__init_tmp__ && mv mise.toml.__init_tmp__ mise.toml

# --- 7. Delete template-only files + self-remove ----------------------------
# Read the manifest (ignoring blank lines and # comments) and delete each path.
while IFS= read -r entry; do
    case "$entry" in
        ''|\#*) continue ;;
    esac
    rm -rf "$entry"
done < scripts/template-manifest.txt

# Remove scripts/ only if empty. After render it still contains release.sh and
# changeset.py (the rendered project's release tooling), so this is normally a
# no-op — init.sh + template-manifest.txt are gone, the release scripts stay.
rmdir scripts 2>/dev/null || true

# --- 8. Next steps ----------------------------------------------------------
cat <<EOF

Done. '$NAME' is ready.

Next steps:
  rm -rf .git && git init && git add -A && git commit -m "Initial commit"
  cp local.properties.example local.properties   # set sdk.dir
  mise install
  mise run api:dump      # generate the public-API baseline for this project
  mise run check         # ktlint + detekt + tests + API check, every target

Then edit:
  CLAUDE.md §1 (Scope) — describe what $DISPLAY does
  $NAME/src/commonMain/kotlin/$GROUP_PATH/$PKG_PATH/Greeter.kt — your real API

Note: commit the generated api/ dumps — they're the reference the API check
compares against. After an intentional public-API change, re-run api:dump and
commit the diff.
EOF

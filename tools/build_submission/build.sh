#!/usr/bin/env bash
# Build the submission artifacts into build/submission/<PART>/ for the given part.
# Usage:   build.sh --part p1 | --part p2
# Run from WSL: needs pandoc and pdflatex. Intermediate .tex.md files are
# written next to the PDFs; build/ is gitignored in full.
set -euo pipefail
cd "$(dirname "$0")/../.."

PART=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --part) PART="$2"; shift 2 ;;
        *) echo "Unknown arg: $1" >&2; exit 2 ;;
    esac
done

shopt -s nullglob
case "$PART" in
    p1|P1)
        PART_TAG="P1"
        ENTRIES=( docs/ai-journal/entries/0[1-6]_*.md )
        ;;
    p2|P2)
        PART_TAG="P2"
        ENTRIES=( docs/ai-journal/entries/0[7-9]_*.md
                  docs/ai-journal/entries/1[0-9]_*.md
                  docs/ai-journal/entries/2[0-9]_*.md )
        ;;
    *) echo "Usage: $0 --part p1|p2" >&2; exit 2 ;;
esac

OUT=build/submission/${PART_TAG}
ID=220104004930
mkdir -p "$OUT"

# 1. Concatenate this part's journal entries (sorted by filename) into a
#    single D4 source.
D4_SRC="$OUT/D4.md"
: > "$D4_SRC"
IFS=$'\n' SORTED_ENTRIES=( $(printf '%s\n' "${ENTRIES[@]}" | sort) )
unset IFS
for f in "${SORTED_ENTRIES[@]}"; do
    [ -s "$D4_SRC" ] && { echo; echo "---"; echo; } >> "$D4_SRC"
    cat "$f" >> "$D4_SRC"
    echo >> "$D4_SRC"
done
echo "  D4 source: ${#SORTED_ENTRIES[@]} entries → $D4_SRC"

# 2. Render each markdown source to its named submission PDF.
render() {
    local src="$1" dst="$2"
    local tmp="$OUT/$(basename "$src").tex.md"
    python3 tools/build_submission/preprocess.py "$src" "$tmp"
    pandoc "$tmp" -o "$OUT/$dst" \
        --pdf-engine=pdflatex \
        -V geometry:margin=1in \
        -H tools/build_submission/header.tex \
        --toc=false
    echo "  $OUT/$dst"
}

render docs/design/D1.md "${ID}_D1_${PART_TAG}.pdf"
render docs/design/D3.md "${ID}_D3_${PART_TAG}.pdf"
render "$D4_SRC"         "${ID}_D4_${PART_TAG}.pdf"
render docs/design/D5.md "${ID}_D5_${PART_TAG}.pdf"

# 3. Package the D2 source zip (src/, samples/, README.md, compile.bat).
ZIP="$OUT/${ID}_D2_${PART_TAG}.zip"
rm -f "$ZIP"
zip -qr "$ZIP" src samples README.md compile.bat
echo "  $ZIP"

echo "Done ($PART_TAG)."

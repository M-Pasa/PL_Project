#!/usr/bin/env bash
# Build the five Part 1 submission artifacts into build/submission/.
# Run from WSL: needs pandoc and pdflatex. Intermediate .tex.md files
# are written next to the PDFs; build/ is gitignored in full.
set -euo pipefail
cd "$(dirname "$0")/../.."

OUT=build/submission
ID=220104004930
mkdir -p "$OUT"

# 1. Concatenate the six journal entries into a single D4 source.
D4_SRC="$OUT/D4.md"
: > "$D4_SRC"
for f in docs/ai-journal/entries/0[1-6]_*.md; do
    [ -s "$D4_SRC" ] && { echo; echo "---"; echo; } >> "$D4_SRC"
    cat "$f" >> "$D4_SRC"
    echo >> "$D4_SRC"
done

# 2. Render each markdown source to its named submission PDF.
render() {
    local src="$1" dst="$2"
    local tmp="$OUT/$(basename "$src").tex.md"
    python3 tools/build_submission/preprocess.py "$src" "$tmp"
    pandoc "$tmp" -o "$OUT/$dst" \
        --pdf-engine=pdflatex \
        -V geometry:margin=1in \
        --toc=false
    echo "  $OUT/$dst"
}

render docs/design/D1.md "${ID}_D1_P1.pdf"
render docs/design/D3.md "${ID}_D3_P1.pdf"
render "$D4_SRC"         "${ID}_D4_P1.pdf"
render docs/design/D5.md "${ID}_D5_P1.pdf"

# 3. Package the D2 source zip (src/, README.md, compile.bat).
ZIP="$OUT/${ID}_D2_P1.zip"
rm -f "$ZIP"
( cd . && zip -qr "$ZIP" src README.md compile.bat )
echo "  $ZIP"

echo "Done."

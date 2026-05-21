#!/usr/bin/env python3
"""Replace Unicode glyphs that bare pdflatex can't render with safe equivalents.
   Turkish letters and section signs work natively via inputenc[utf8]."""
import sys, pathlib, re

# Each replacement is a (char, plain_text, in_code_text) tuple.
# We only replace inside a regular text run; code spans/blocks get the
# code-safe form so the math escape doesn't break verbatim rendering.
REPLACEMENTS = {
    "→": ("$\\rightarrow$", "->"),
    "←": ("$\\leftarrow$", "<-"),
    "↔": ("$\\leftrightarrow$", "<->"),
    "⇒": ("$\\Rightarrow$", "=>"),
    "≡": ("$\\equiv$", "=="),
    "≥": ("$\\ge$", ">="),
    "≤": ("$\\le$", "<="),
    "≠": ("$\\ne$", "!="),
    "≈": ("$\\approx$", "~="),
    "·": ("$\\cdot$", "*"),
    "×": ("$\\times$", "*"),
    "±": ("$\\pm$", "+/-"),
    "²": ("$^{2}$", "^2"),
    "−": ("$-$", "-"),
    "Δ": ("$\\Delta$", "Delta"),
    "Σ": ("$\\Sigma$", "Sum"),
    "…": ("\\ldots ", "..."),
    "↦": ("$\\mapsto$", "|->"),
    "⇓": ("$\\Downarrow$", "||"),
    "⇝": ("$\\rightsquigarrow$", "~>"),
    "∅": ("$\\emptyset$", "{}"),
    "∈": ("$\\in$", "in"),
    "∉": ("$\\notin$", "not in"),
    "∧": ("$\\wedge$", "&&"),
    "∪": ("$\\cup$", "U"),
    "⊕": ("$\\oplus$", "(+)"),
    "⊢": ("$\\vdash$", "|-"),
    "⊥": ("$\\bot$", "_|_"),
    "₀": ("$_{0}$", "_0"),
    "₁": ("$_{1}$", "_1"),
    "₂": ("$_{2}$", "_2"),
    "ₙ": ("$_{n}$", "_n"),
    "ᵢ": ("$_{i}$", "_i"),
    "ⱼ": ("$_{j}$", "_j"),
    "─": ("-", "-"),
    "└": ("+", "+"),
    "├": ("+", "+"),
    "✅": ("[Y]", "[Y]"),
    "✓": ("[Y]", "[Y]"),
    "❌": ("[X]", "[X]"),
    "�": ("?", "?"),
    # smart dashes — pdflatex handles via inputenc but be explicit
    "–": ("--", "--"),
    "—": ("---", "---"),
}

def transform(text: str) -> str:
    out = []
    i = 0
    in_code_block = False
    while i < len(text):
        # toggle on triple backtick at line start
        if text.startswith("```", i) and (i == 0 or text[i-1] == "\n"):
            in_code_block = not in_code_block
            out.append("```"); i += 3; continue
        ch = text[i]
        if ch in REPLACEMENTS:
            text_form, code_form = REPLACEMENTS[ch]
            if in_code_block:
                out.append(code_form)
            else:
                # check if inside an inline `code` span on this line
                line_start = text.rfind("\n", 0, i) + 1
                segment = text[line_start:i]
                ticks = segment.count("`")
                use = code_form if ticks % 2 == 1 else text_form
                # pandoc rejects $...$ math when the closing $ is
                # immediately followed by a digit. Insert {} before the
                # digit so pandoc still finds a math span and the
                # visible output is unchanged (LaTeX renders {} empty).
                nxt = text[i+1] if i+1 < len(text) else ""
                if use.endswith("$") and nxt.isdigit():
                    out.append(use); out.append("{}"); i += 1; continue
                out.append(use)
            i += 1
        else:
            out.append(ch); i += 1
    return "".join(out)

if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    pathlib.Path(dst).write_text(
        transform(pathlib.Path(src).read_text(encoding="utf-8")),
        encoding="utf-8")
    print(f"wrote {dst}")

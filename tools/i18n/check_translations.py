#!/usr/bin/env python3
"""Automated quality checks for the 10 app languages.

Hard errors (exit code 1): missing/empty strings, placeholder mismatches, wrong plural categories,
text written in the wrong script. Warnings: identical-to-English text and unusually long text.
Usage: python3 tools/i18n/check_translations.py [--warnings]
"""
import re
import sys

from i18n_common import LANGUAGES, PLURAL_CATEGORIES, load, modules, placeholders

SCRIPTS = {
    "zh": r"[\u4e00-\u9fff]", "hi": r"[\u0900-\u097f]", "bn": r"[\u0980-\u09ff]",
    "ar": r"[\u0600-\u06ff]", "ur": r"[\u0600-\u06ff]", "ru": r"[\u0400-\u04ff]",
}
# Words that are legitimately the same as English in some languages.
SAME_OK = re.compile(
    r"^(onio|cvv|total|subtotal|ok|usd|[A-Z]{3}|4★.*|mm/aa|mm/yy|tops|smartphones|sports|tablets|minutes"
    r"|total \(%1\$d item\)|%1\$d item|%1\$d min|%1\$d%% off)$",
    re.IGNORECASE,
)


def main(show_warnings):
    errors, warnings, checked = [], [], 0
    for module in modules():
        english = load(module, "en")
        for lang in LANGUAGES:
            if lang == "en":
                continue
            translated = load(module, lang)
            for (key, quantity), en in english.items():
                if not en.translatable:
                    continue
                where = f"{module} [{lang}] {key}{'/' + quantity if quantity else ''}"
                got = translated.get((key, quantity))
                if got is None:
                    # Plural categories are checked separately below; a missing plain string is an error.
                    if not quantity:
                        errors.append(f"{where}: missing")
                    continue
                checked += 1
                text = got.text.strip()
                if not text:
                    errors.append(f"{where}: empty")
                    continue
                if got.text != got.text.strip():
                    warnings.append(f"{where}: leading/trailing whitespace")
                # Plural forms of one key may legitimately differ; compare against the same quantity or "other".
                reference = english.get((key, quantity)) or english.get((key, "other"))
                if placeholders(got.text) != placeholders(reference.text) and not (
                    quantity in ("zero", "one", "two") and lang == "ar"
                ):
                    errors.append(f"{where}: placeholders {placeholders(got.text)} != {placeholders(reference.text)}")
                if lang in SCRIPTS and not SAME_OK.match(text):
                    letters = re.sub(r"[%\d$sdf\s\W]", "", got.text)
                    if letters and not re.search(SCRIPTS[lang], got.text):
                        errors.append(f"{where}: no {lang} script in {got.text!r}")
                if lang not in SCRIPTS and text == en.text.strip() and not SAME_OK.match(text):
                    warnings.append(f"{where}: identical to English ({text!r})")
                if len(text) > 2.6 * max(len(en.text), 12):
                    warnings.append(f"{where}: {len(text)} chars vs {len(en.text)} in English (may overflow)")
            # Every plural must define exactly the categories the language needs.
            plural_keys = {k for (k, q) in english if q}
            for key in plural_keys:
                have = {q for (k, q) in translated if k == key and q}
                need = PLURAL_CATEGORIES[lang]
                if have != need:
                    errors.append(f"{module} [{lang}] {key}: plural categories {sorted(have)} != {sorted(need)}")
    print(f"checked {checked} translations in {len(modules())} modules")
    for line in errors:
        print("ERROR  ", line)
    if show_warnings:
        for line in warnings:
            print("warning", line)
    else:
        print(f"{len(warnings)} warnings (run with --warnings to list them)")
    print(f"{len(errors)} errors")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main("--warnings" in sys.argv))

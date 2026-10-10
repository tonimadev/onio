#!/usr/bin/env python3
"""Exports every translatable string to docs/translations/<lang>.csv for native reviewers.

Columns: module, key, quantity, english, translation. Reviewers edit only the `translation` column
(keep placeholders like %1$s untouched) and send the file back; apply it with import_translations.py.
Usage: python3 tools/i18n/export_translations.py
"""
import csv
import os

from i18n_common import LANGUAGES, ROOT, load, modules

OUT = os.path.join(ROOT, "docs", "translations")


def main():
    os.makedirs(OUT, exist_ok=True)
    for lang in LANGUAGES:
        if lang == "en":
            continue
        rows = []
        for module in modules():
            english, translated = load(module, "en"), load(module, lang)
            for (key, quantity), en in english.items():
                if en.translatable:
                    got = translated.get((key, quantity))
                    rows.append([module, key, quantity, en.text, got.text if got else ""])
        with open(os.path.join(OUT, f"{lang}.csv"), "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(["module", "key", "quantity", "english", "translation"])
            writer.writerows(rows)
        print(f"{lang}: {len(rows)} rows")


if __name__ == "__main__":
    main()

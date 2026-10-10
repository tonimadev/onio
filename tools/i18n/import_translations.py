#!/usr/bin/env python3
"""Applies a reviewed CSV (see export_translations.py) to the strings.xml files.

Only rows whose `translation` differs from what is in the repo are written, and each module file keeps
its element order. Run check_translations.py afterwards.
Usage: python3 tools/i18n/import_translations.py <lang> path/to/reviewed.csv
"""
import csv
import re
import sys

from i18n_common import LANGUAGES, escape, load, strings_path


def apply(lang, csv_path):
    changes = {}
    with open(csv_path, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            current = load(row["module"], lang).get((row["key"], row["quantity"]))
            new = row["translation"]
            if current is not None and new.strip() and new != current.text:
                changes.setdefault(row["module"], {})[(row["key"], row["quantity"])] = new
    total = 0
    for module, items in changes.items():
        path = strings_path(module, lang)
        xml = open(path, encoding="utf-8").read()
        for (key, quantity), new in items.items():
            if quantity:
                pattern = re.compile(
                    r'(<plurals name="%s">.*?<item quantity="%s">)(.*?)(</item>)' % (re.escape(key), quantity), re.S)
            else:
                pattern = re.compile(r'(<string name="%s"[^>]*>)(.*?)(</string>)' % re.escape(key), re.S)
            xml, n = pattern.subn(lambda m: m.group(1) + escape(new) + m.group(3), xml, count=1)
            total += n
        open(path, "w", encoding="utf-8").write(xml)
    print(f"{lang}: updated {total} strings in {len(changes)} modules")


if __name__ == "__main__":
    if len(sys.argv) != 3 or sys.argv[1] not in LANGUAGES:
        sys.exit(__doc__)
    apply(sys.argv[1], sys.argv[2])

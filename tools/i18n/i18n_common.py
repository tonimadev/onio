"""Shared helpers for the translation tools: locate and parse every module's strings.xml."""
import glob
import os
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

# language code -> values folder suffix ("" is the English default)
LANGUAGES = {
    "en": "", "zh": "-zh", "hi": "-hi", "es": "-es", "fr": "-fr",
    "ar": "-ar", "bn": "-bn", "pt": "-pt", "ru": "-ru", "ur": "-ur",
}

# CLDR plural categories each language needs in <plurals>.
PLURAL_CATEGORIES = {
    "en": {"one", "other"}, "zh": {"other"}, "hi": {"one", "other"}, "bn": {"one", "other"},
    "ur": {"one", "other"}, "es": {"one", "many", "other"}, "fr": {"one", "many", "other"},
    "pt": {"one", "many", "other"}, "ru": {"one", "few", "many", "other"},
    "ar": {"zero", "one", "two", "few", "many", "other"},
}


@dataclass(frozen=True)
class Entry:
    module: str
    key: str
    quantity: str  # "" for plain strings, CLDR category for plurals
    text: str
    translatable: bool = True


def modules():
    """Directories (relative to the repo root) that own a res/values/strings.xml."""
    found = glob.glob(os.path.join(ROOT, "**", "src", "main", "res", "values", "strings.xml"), recursive=True)
    return sorted(os.path.relpath(p, ROOT).split("/src/main/")[0] for p in found if "/build/" not in p)


def strings_path(module, lang):
    return os.path.join(ROOT, module, "src", "main", "res", f"values{LANGUAGES[lang]}", "strings.xml")


def unescape(text):
    """Android string escapes -> the text a user sees."""
    return re.sub(r"\\(['\"@?\\])", r"\1", text or "")


def escape(text):
    """Visible text -> Android resource XML text."""
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    return ("\\" + text) if text.startswith(("@", "?")) else text


def load(module, lang):
    """Returns {(key, quantity): Entry} for one module/language ({} if the file is missing)."""
    path = strings_path(module, lang)
    if not os.path.exists(path):
        return {}
    out = {}
    for node in ET.parse(path).getroot():
        key = node.get("name")
        translatable = node.get("translatable") != "false"
        if node.tag == "string":
            out[(key, "")] = Entry(module, key, "", unescape(node.text), translatable)
        elif node.tag == "plurals":
            for item in node:
                q = item.get("quantity")
                out[(key, q)] = Entry(module, key, q, unescape(item.text), translatable)
    return out


PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdf%]")


def placeholders(text):
    return sorted(PLACEHOLDER.findall(text))

# Translation review

Onio ships in 10 languages: English (source), Chinese (`zh`), Hindi (`hi`), Spanish (`es`), French (`fr`),
Arabic (`ar`), Bengali (`bn`), Portuguese-BR (`pt`), Russian (`ru`) and Urdu (`ur`).

**Status:** every translation was written by an AI assistant and has had an automated check plus a
read-through of the main sentences in `es`, `fr`, `pt` and `ru`. **No language has been reviewed by a native
speaker yet**, so reviewers are very welcome, especially for `zh`, `hi`, `ar`, `bn` and `ur`.

## How to review a language

1. Open `<lang>.csv` (columns: `module`, `key`, `quantity`, `english`, `translation`).
2. Edit **only** the `translation` column. Keep placeholders such as `%1$s`, `%1$d` and `%%` exactly as they are.
3. `quantity` is a plural form (`one`, `few`, `many`, `other`, ...). Rows with the same `key` are the same
   sentence for different numbers: make each one grammatically correct for that count.
4. Tone: friendly and a little playful, never pushy. The app helps people who shop compulsively, so avoid
   promotional or shaming wording.
5. Send the file back (a pull request or an issue attachment).

## How a maintainer applies it

```sh
python3 tools/i18n/import_translations.py <lang> path/to/reviewed.csv   # writes the strings.xml files
python3 tools/i18n/check_translations.py --warnings                     # placeholders, plurals, script, length
python3 tools/i18n/export_translations.py                               # refresh these CSVs
```

The `strings.xml` files under `*/src/main/res/values-<lang>/` are the source of truth; these CSVs are
generated from them. Lint treats a missing translation as a build error.

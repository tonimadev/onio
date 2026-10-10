# Play Store listing (fastlane `supply`)

```
fastlane/metadata/android/<locale>/
  title.txt  short_description.txt  full_description.txt
  images/featureGraphic.png   # 1024x500, no alpha
  images/icon.png             # 512x512
```

Locales: `en-US zh-CN hi-IN es-419 fr-FR ar bn-BD pt-BR ru-RU ur` (the 10 app languages).

- Edit texts/taglines in `tools/store-assets/generate_store_assets.py`, then `fastlane store_assets`.
- `fastlane check_credentials` then `fastlane listing` (needs `SUPPLY_JSON_KEY`).
- Play policy: keep artwork free of prices, "free", "sale", rankings and calls to action.
- Phone screenshots (`images/phoneScreenshots/*.png`) are not generated yet.

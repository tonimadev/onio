package com.ipirangatech.onio.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Localized category name for an API slug, falling back to [fallback] (the API's English name). */
fun categoryLabel(context: Context, slug: String, fallback: String): String {
    val id = context.resources.getIdentifier("category_" + slug.replace('-', '_'), "string", context.packageName)
    return if (id != 0) context.getString(id) else fallback
}

@Composable
fun categoryLabel(slug: String, fallback: String): String = categoryLabel(LocalContext.current, slug, fallback)

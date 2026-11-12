package app.wortlinge.data

import android.content.Context
import app.wortlinge.core.content.WordCatalog

class ContentRepository(private val context: Context) {
    fun load(): WordCatalog =
        context.assets.open("words.json").bufferedReader(Charsets.UTF_8).use { WordCatalog.parse(it.readText()) }
}

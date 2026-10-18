package eu.kanade.tachiyomi.source

import androidx.preference.PreferenceScreen

interface Source {
    val name: String
    val lang: String
}

interface SourceFactory {
    fun createSources(): List<Source>
}

interface ConfigurableSource {
    fun setupPreferenceScreen(screen: PreferenceScreen): PreferenceScreen
}

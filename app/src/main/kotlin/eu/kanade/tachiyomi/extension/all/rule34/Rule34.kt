package eu.kanade.tachiyomi.extension.all.rule34

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class Rule34 : HttpSource(), ConfigurableSource {

    override val name = "Rule34"
    override val baseUrl = "https://rule34.xxx"
    private val apiUrl = "https://api.rule34.xxx"
    override val lang = "all"
    override val supportsLatest = true

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    private val apiKey: String
        get() = preferences.getString("api_key", "") ?: ""

    private val userId: String
        get() = preferences.getString("user_id", "") ?: ""

    private fun checkCredentials() {
        if (apiKey.isBlank() || userId.isBlank()) {
            throw Exception("Please enter your Rule34 User ID and API Key in the extension settings.")
        }
    }

    override fun headersBuilder() = super.headersBuilder().add("Referer", baseUrl)

    // Popular Manga
    override fun popularMangaRequest(page: Int): Request {
        checkCredentials()
        val offset = (page - 1) * 40
        return GET("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&tags=sort:score&pid=$offset&limit=40&api_key=$apiKey&user_id=$userId", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = parsePosts(response)

    // Latest Manga
    override fun latestUpdatesRequest(page: Int): Request {
        checkCredentials()
        val offset = (page - 1) * 40
        return GET("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&tags=&pid=$offset&limit=40&api_key=$apiKey&user_id=$userId", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = parsePosts(response)

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        checkCredentials()
        val offset = (page - 1) * 40
        val tags = query.trim().replace(" ", "+")
        return GET("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&tags=$tags&pid=$offset&limit=40&api_key=$apiKey&user_id=$userId", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = parsePosts(response)

    private fun parsePosts(response: Response): MangasPage {
        val jsonArray = JSONArray(response.body.string())
        val mangas = mutableListOf<SManga>()
        
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val manga = SManga.create().apply {
                url = json.getInt("id").toString()
                title = "Post ${json.getInt("id")}"
                thumbnail_url = json.optString("preview_url", json.optString("sample_url"))
                description = "Tags: ${json.optString("tags", "")}"
            }
            mangas.add(manga)
        }
        
        return MangasPage(mangas, jsonArray.length() == 40)
    }

    // Manga Details
    override fun mangaDetailsRequest(manga: SManga): Request {
        checkCredentials()
        return GET("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&id=${manga.url}&api_key=$apiKey&user_id=$userId", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val jsonArray = JSONArray(response.body.string())
        if (jsonArray.length() == 0) throw Exception("Post not found")
        val json = jsonArray.getJSONObject(0)
        
        return SManga.create().apply {
            title = "Post ${json.getInt("id")}"
            thumbnail_url = json.optString("preview_url", json.optString("sample_url"))
            description = "Tags: ${json.optString("tags", "")}\nScore: ${json.optInt("score", 0)}"
            genre = json.optString("tags", "").replace(" ", ", ")
            status = SManga.COMPLETED
        }
    }

    // Chapters (Each post is treated as a single-chapter manga)
    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val jsonArray = JSONArray(response.body.string())
        if (jsonArray.length() == 0) return emptyList()
        val json = jsonArray.getJSONObject(0)
        
        return listOf(
            SChapter.create().apply {
                url = json.getInt("id").toString()
                name = "Post ${json.getInt("id")}"
                date_upload = parseDate(json.optString("created_at", ""))
            }
        )
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            try { dateStr.toLong() * 1000 } catch (e: Exception) { 0L }
        }
    }

    // Pages
    override fun pageListRequest(chapter: SChapter): Request {
        checkCredentials()
        return GET("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&id=${chapter.url}&api_key=$apiKey&user_id=$userId", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val jsonArray = JSONArray(response.body.string())
        if (jsonArray.length() == 0) return emptyList()
        val json = jsonArray.getJSONObject(0)
        
        val fileUrl = json.getString("file_url")
        return listOf(Page(0, "", fileUrl))
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used.")

    // Preferences for API Credentials
    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val userIdPref = EditTextPreference(screen.context).apply {
            key = "user_id"
            title = "User ID"
            summary = "Your Rule34 User ID. Required for API access."
            setDefaultValue("")
        }
        
        val apiKeyPref = EditTextPreference(screen.context).apply {
            key = "api_key"
            title = "API Key"
            summary = "Your Rule34 API Key. Required for API access."
            setDefaultValue("")
        }
        
        screen.addPreference(userIdPref)
        screen.addPreference(apiKeyPref)
    }
}

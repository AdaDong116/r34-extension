package eu.kanade.tachiyomi.extension.all.rule34

import android.content.SharedPreferences
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

class Rule34Factory : SourceFactory {
    override fun createSources(): List<Source> = listOf(Rule34())
}

class Rule34 : HttpSource(), ConfigurableSource {

    override val name = "Rule34"
    override val baseUrl = "https://rule34.xxx"
    override val lang = "all"
    override val supportsLatest = true

    private val apiUrl = "https://api.rule34.xxx"

    private var prefs: SharedPreferences? = null

    private val userId: String get() = prefs?.getString("user_id", "") ?: ""
    private val apiKey: String get() = prefs?.getString("api_key", "") ?: ""

    private val authParams: String
        get() {
            val sb = StringBuilder()
            if (userId.isNotBlank()) sb.append("&user_id=$userId")
            if (apiKey.isNotBlank()) sb.append("&api_key=$apiKey")
            if (sb.isEmpty()) throw Exception("Open Rule34 extension settings and enter your User ID / API Key.")
            return sb.toString()
        }

    override fun headersBuilder(): Headers.Builder = super.headersBuilder().add("Referer", baseUrl)

    private fun apiGet(url: String): Request = Request.Builder().url(url).headers(headers).build()

    override fun popularMangaRequest(page: Int): Request {
        val pid = (page - 1) * 40
        return apiGet("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&tags=sort:score&pid=$pid&limit=40$authParams")
    }

    override fun popularMangaParse(response: Response): MangasPage = parsePosts(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val pid = (page - 1) * 40
        return apiGet("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&tags=&pid=$pid&limit=40$authParams")
    }

    override fun latestUpdatesParse(response: Response): MangasPage = parsePosts(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val pid = (page - 1) * 40
        val tags = query.trim().replace(" ", "+")
        return apiGet("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&tags=$tags&pid=$pid&limit=40$authParams")
    }

    override fun searchMangaParse(response: Response): MangasPage = parsePosts(response)

    private fun parsePosts(response: Response): MangasPage {
        val jsonArray = JSONArray(response.body!!.string())
        val mangas = mutableListOf<SManga>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            mangas.add(SManga.create().apply {
                url = json.getInt("id").toString()
                title = "Post ${json.getInt("id")}"
                thumbnail_url = json.optString("preview_url", json.optString("sample_url"))
                description = "Tags: ${json.optString("tags", "")}"
            })
        }
        return MangasPage(mangas, jsonArray.length() == 40)
    }

    override fun mangaDetailsRequest(manga: SManga): Request =
        apiGet("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&id=${manga.url}$authParams")

    override fun mangaDetailsParse(response: Response): SManga {
        val jsonArray = JSONArray(response.body!!.string())
        if (jsonArray.length() == 0) throw Exception("Post not found")
        val json = jsonArray.getJSONObject(0)
        return SManga.create().apply {
            title = "Post ${json.getInt("id")}"
            thumbnail_url = json.optString("preview_url", json.optString("sample_url"))
            description = "Tags: ${json.optString("tags", "")}\nScore: ${json.optInt("score", 0)}"
            genre = json.optString("tags", "").replace(" ", ", ")
            status = SManga.COMPLETED
            initialized = true
        }
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val jsonArray = JSONArray(response.body!!.string())
        if (jsonArray.length() == 0) return emptyList()
        val json = jsonArray.getJSONObject(0)
        return listOf(SChapter.create().apply {
            url = json.getInt("id").toString()
            name = "Post ${json.getInt("id")}"
            date_upload = parseDate(json.optString("created_at", ""))
        })
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            try { dateStr.toLong() * 1000 } catch (e: Exception) { 0L }
        }
    }

    override fun pageListRequest(chapter: SChapter): Request =
        apiGet("$apiUrl/index.php?page=dapi&s=post&q=index&json=1&id=${chapter.url}$authParams")

    override fun pageListParse(response: Response): List<Page> {
        val jsonArray = JSONArray(response.body!!.string())
        if (jsonArray.length() == 0) return emptyList()
        val json = jsonArray.getJSONObject(0)
        return listOf(Page(0, "", json.getString("file_url")))
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used.")

    override fun setupPreferenceScreen(screen: PreferenceScreen): PreferenceScreen {
        val p = screen.context.getSharedPreferences("rule34_prefs", 0)
        prefs = p
        screen.addPreference(EditTextPreference(screen.context).apply {
            key = "user_id"
            title = "User ID"
            summary = "Your Rule34 User ID (required)"
            setDefaultValue("")
            setOnPreferenceChangeListener { _, v -> p.edit().putString("user_id", v as String).commit(); true }
        })
        screen.addPreference(EditTextPreference(screen.context).apply {
            key = "api_key"
            title = "API Key"
            summary = "Your Rule34 API Key (optional)"
            setDefaultValue("")
            setOnPreferenceChangeListener { _, v -> p.edit().putString("api_key", v as String).commit(); true }
        })
        return screen
    }
}

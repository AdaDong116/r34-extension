package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

abstract class HttpSource : Source {
    abstract val baseUrl: String
    abstract val supportsLatest: Boolean
    open fun headersBuilder(): Headers.Builder = Headers.Builder()
    open val headers: Headers get() = headersBuilder().build()
    abstract fun popularMangaRequest(page: Int): Request
    abstract fun popularMangaParse(response: Response): MangasPage
    abstract fun latestUpdatesRequest(page: Int): Request
    abstract fun latestUpdatesParse(response: Response): MangasPage
    abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    abstract fun searchMangaParse(response: Response): MangasPage
    open fun mangaDetailsRequest(manga: SManga): Request = throw UnsupportedOperationException()
    abstract fun mangaDetailsParse(response: Response): SManga
    open fun chapterListRequest(manga: SManga): Request = throw UnsupportedOperationException()
    abstract fun chapterListParse(response: Response): List<SChapter>
    open fun pageListRequest(chapter: SChapter): Request = throw UnsupportedOperationException()
    abstract fun pageListParse(response: Response): List<Page>
    abstract fun imageUrlParse(response: Response): String
}

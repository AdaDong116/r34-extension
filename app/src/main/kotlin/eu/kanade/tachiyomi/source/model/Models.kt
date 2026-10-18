package eu.kanade.tachiyomi.source.model

interface SManga {
    var title: String
    var url: String
    var thumbnail_url: String?
    var description: String?
    var author: String?
    var artist: String?
    var genre: String?
    var status: Int
    var initialized: Boolean
    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6
        fun create(): SManga = SMangaImpl()
    }
}

class SMangaImpl : SManga {
    override lateinit var title: String
    override lateinit var url: String
    override var thumbnail_url: String? = null
    override var description: String? = null
    override var author: String? = null
    override var artist: String? = null
    override var genre: String? = null
    override var status: Int = 0
    override var initialized: Boolean = false
}

interface SChapter {
    var url: String
    var name: String
    var date_upload: Long
    var chapter_number: Float
    var scanlator: String?
    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

class SChapterImpl : SChapter {
    override lateinit var url: String
    override lateinit var name: String
    override var date_upload: Long = 0
    override var chapter_number: Float = -1f
    override var scanlator: String? = null
}

open class Page(val index: Int, val url: String = "", @Volatile var imageUrl: String? = null)

class MangasPage(val mangas: List<SManga>, val hasNextPage: Boolean)

abstract class Filter<T>(val name: String)

class FilterList(vararg filters: Filter<*>)

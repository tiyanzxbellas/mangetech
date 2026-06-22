package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaItem(
    @Json(name = "title") val title: String,
    @Json(name = "url") val url: String,
    @Json(name = "img") val img: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "views") val views: String?,
    @Json(name = "score") val score: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "colorized") val colorized: String?,
    @Json(name = "data") val data: MangaLatestData?
) {
    // Helper to extract numeric ID from url like "https://kmkindo.click/?page=manga&id=200719"
    fun extractId(): String {
        return extractIdFromUrl(url)
    }
}

@JsonClass(generateAdapter = true)
data class MangaLatestData(
    @Json(name = "url") val url: String?,
    @Json(name = "chapter") val chapter: String?,
    @Json(name = "time") val time: String?
) {
    fun extractId(): String? {
        return url?.let { extractIdFromUrl(it) }
    }
}

@JsonClass(generateAdapter = true)
data class MangaDetail(
    @Json(name = "title") val title: String,
    @Json(name = "cover") val cover: String?,
    @Json(name = "img") val img: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "released") val released: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "score") val score: String?,
    @Json(name = "synopsis") val synopsis: String?,
    @Json(name = "author") val author: List<NameLink>?,
    @Json(name = "genre") val genre: List<NameLink>?,
    @Json(name = "demographic") val demographic: List<NameLink>?,
    @Json(name = "theme") val theme: List<NameLink>?,
    @Json(name = "content") val content: List<NameLink>?,
    @Json(name = "data") val data: List<ChapterItem>?
)

@JsonClass(generateAdapter = true)
data class NameLink(
    @Json(name = "name") val name: String,
    @Json(name = "link") val link: String?
) {
    // Extract genre, demographic, or theme val.
    // e.g., "https://kmkindo.click/?page=term_result&term=genres&val=comedy" -> "comedy"
    fun extractQueryValue(): String? {
        return link?.let { url ->
            val uri = android.net.Uri.parse(url)
            uri.getQueryParameter("val")
        }
    }
}

@JsonClass(generateAdapter = true)
data class ChapterItem(
    @Json(name = "chapter") val chapter: String,
    @Json(name = "url") val url: String,
    @Json(name = "download") val download: String?
) {
    // Extract chapter ID from "https://kmkindo.click/?page=chapter&id=425726"
    fun extractId(): String {
        return extractIdFromUrl(url)
    }
}

@JsonClass(generateAdapter = true)
data class ChapterDetail(
    @Json(name = "title") val title: String,
    @Json(name = "prev") val prev: String?,
    @Json(name = "next") val next: String?,
    @Json(name = "thumb") val thumb: String?,
    @Json(name = "chapter") val chapter: String?,
    @Json(name = "image") val image: List<String>?
)

fun extractIdFromUrl(url: String): String {
    return try {
        val uri = android.net.Uri.parse(url)
        val id = uri.getQueryParameter("id")
        id ?: url.split("&").firstOrNull { it.startsWith("id=") }?.substringAfter("id=") ?: ""
    } catch (e: Exception) {
        ""
    }
}

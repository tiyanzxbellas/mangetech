package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface KomikIndoApi {
    @GET("/")
    suspend fun getHomepage(
        @Query("page") page: String, // "latest", "rekomendasi", "terpopuler"
        @Query("paged") paged: String? = null,
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ResponseBody

    @GET("/")
    suspend fun searchManga(
        @Query("page") page: String = "search",
        @Query("search") query: String,
        @Query("paged") paged: String = "1",
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ResponseBody

    @GET("/")
    suspend fun getTermResult(
        @Query("page") page: String = "term_result",
        @Query("term") term: String, // "genres", "demographic", "theme", "content"
        @Query("val") value: String,
        @Query("paged") paged: String = "1",
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ResponseBody

    @GET("/")
    suspend fun getTypeList(
        @Query("page") page: String = "type",
        @Query("type") type: String, // "manga", "manhua", "manhwa"
        @Query("paged") paged: String = "1",
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ResponseBody

    @GET("/")
    suspend fun getColorizedList(
        @Query("page") page: String = "colorized",
        @Query("colorized") colorized: String, // "colorized" for 1, "bnw" for 0
        @Query("paged") paged: String = "1",
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ResponseBody

    @GET("/")
    suspend fun getMangaDetail(
        @Query("page") page: String = "manga",
        @Query("id") id: String,
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ResponseBody

    @GET("/")
    suspend fun getChapterDetail(
        @Query("page") page: String = "chapter",
        @Query("id") id: String,
        @Header("User-Agent") userAgent: String = "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)"
    ): ChapterDetail
}

object RetrofitClient {
    private const val BASE_URL = "https://kmkindo.click/"

    val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: KomikIndoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(KomikIndoApi::class.java)
    }

    fun parseMangaList(responseBody: String): List<MangaItem> {
        val trimmed = responseBody.trim()
        return if (trimmed.startsWith("[")) {
            try {
                moshi.adapter(Array<MangaItem>::class.java).fromJson(trimmed)?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun parseMangaDetail(responseBody: String): List<MangaDetail> {
        val trimmed = responseBody.trim()
        return if (trimmed.startsWith("[")) {
            try {
                moshi.adapter(Array<MangaDetail>::class.java).fromJson(trimmed)?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}

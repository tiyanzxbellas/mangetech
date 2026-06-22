package com.example

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun fetchApiTest() {
    val client = OkHttpClient()
    val urls = listOf(
        "https://kmkindo.click/?page=latest&paged=1",
        "https://kmkindo.click/?page=search&search=osaraku+kanojo&paged=1",
        "https://kmkindo.click/?page=manga&id=200719"
    )
    for (url in urls) {
        println("=== Fetching $url ===")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 10; Redmi Note 4 Build/QQ3A.200905.001)")
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            println("Response Length: ${body?.length}")
            println("Subset: " + body?.take(600))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
  }
}

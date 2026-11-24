package com.zirakusa.tienda

import com.zirakusa.tienda.data.AllowedHost
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A URL outside [AllowedHost] (asked for, or a redirect): nothing is sent there. */
class BlockedHost(url: String) : IOException("host not allowed: ${url.substringBefore('?').take(80)}")
class TooBig : IOException("file too big")
class NotFound : IOException("not found")

/**
 * Tienda's only door to the internet: OkHttp with every request, also each redirect hop, checked against [AllowedHost]
 * before it leaves. No cookies, no cache, no analytics.
 */
class Net(private val client: OkHttpClient = build()) {
    companion object {
        private val guard = Interceptor { chain ->
            val url = chain.request().url.toString()
            if (!AllowedHost.allowed(url)) throw BlockedHost(url)
            chain.proceed(chain.request())
        }

        fun build(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followSslRedirects(false)
            .addInterceptor(guard)
            .addNetworkInterceptor(guard)
            .build()
    }

    private fun call(url: String): Response {
        if (!AllowedHost.allowed(url)) throw BlockedHost(url)
        val response = client.newCall(Request.Builder().url(url).header("User-Agent", "Tienda").build()).execute()
        if (response.code == 404) { response.close(); throw NotFound() }
        if (!response.isSuccessful) { response.close(); throw IOException("HTTP ${response.code}") }
        return response
    }

    /** A small text file (the catalog), at most [maxBytes]. */
    fun text(url: String, maxBytes: Int): String = call(url).use { r ->
        val body = r.body
        if (body.contentLength() > maxBytes) throw TooBig()
        val out = java.io.ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > maxBytes) throw TooBig()
            }
        }
        out.toString(Charsets.UTF_8.name())
    }

    /** Saves [url] to [target], at most [maxBytes]; [progress] gets 0..1 (or -1 when the size is unknown). */
    fun download(url: String, target: File, maxBytes: Long, progress: (Float) -> Unit) {
        call(url).use { r ->
            val total = r.body.contentLength()
            if (total > maxBytes) throw TooBig()
            r.body.byteStream().use { input ->
                target.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        done += n
                        if (done > maxBytes) throw TooBig()
                        out.write(buffer, 0, n)
                        progress(if (total > 0) done.toFloat() / total else -1f)
                    }
                }
            }
        }
    }
}

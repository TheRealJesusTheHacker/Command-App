package net.sentinelsafeguard.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** One HTTP call, shaped so unit tests can substitute a fake transport. */
data class HttpRequest(
    val method: String,
    val path: String,
    val body: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

data class HttpResponse(
    val status: Int,
    val body: String,
)

interface HttpTransport {
    suspend fun execute(request: HttpRequest): HttpResponse

    /** Binary download (the update APK), same headers as [execute]. */
    suspend fun download(request: HttpRequest, sink: (ByteArray) -> Unit): HttpResponse {
        throw UnsupportedOperationException("This transport cannot download files.")
    }
}

class OkHttpTransport(
    private val baseUrl: String,
    // TLS for the Command host uses the platform trust store with NO
    // certificate pinning — a deliberate decision, not an omission:
    // the certificate on command.sentinelsafeguard.net is presented
    // through Cloudflare's layer, outside the owner's control, and a
    // stale pin would brick this client and the update download
    // below at the same moment. The mail client pins instead (its
    // server is the owner's own); see CertificatePins/HARDENING.md.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) : HttpTransport {

    private fun buildRequest(request: HttpRequest): Request {
        val builder = Request.Builder().url(baseUrl.trimEnd('/') + request.path)
        request.headers.forEach { (k, v) -> builder.header(k, v) }
        when (request.method.uppercase()) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            "POST" -> builder.post((request.body ?: "").toRequestBody(JSON))
            "PATCH" -> builder.patch((request.body ?: "").toRequestBody(JSON))
            else -> builder.method(request.method, request.body?.toRequestBody(JSON))
        }
        return builder.build()
    }

    override suspend fun execute(request: HttpRequest): HttpResponse =
        withContext(Dispatchers.IO) {
            client.newCall(buildRequest(request)).execute().use { resp ->
                HttpResponse(resp.code, resp.body?.string() ?: "")
            }
        }

    override suspend fun download(request: HttpRequest, sink: (ByteArray) -> Unit): HttpResponse =
        withContext(Dispatchers.IO) {
            client.newCall(buildRequest(request)).execute().use { resp ->
                resp.body?.byteStream()?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        sink(buffer.copyOf(read))
                    }
                }
                HttpResponse(resp.code, "")
            }
        }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

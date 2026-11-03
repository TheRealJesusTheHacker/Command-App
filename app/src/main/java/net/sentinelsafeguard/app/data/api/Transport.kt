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
    /** Raw-byte body (Phase 3 photo / voice uploads): when set it
     *  rides instead of [body], with the Content-Type taken from
     *  the Content-Type header (default application/octet-stream). */
    val bodyBytes: ByteArray? = null,
) {
    // ByteArray in a data class: identity equality for that field is
    // deliberate — requests are values to execute, never compared.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpRequest) return false
        return method == other.method && path == other.path &&
            body == other.body && headers == other.headers &&
            (bodyBytes?.contentEquals(other.bodyBytes ?: ByteArray(0)) ?: (other.bodyBytes == null))
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + (bodyBytes?.contentHashCode() ?: 0)
        return result
    }
}

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
        val raw = request.bodyBytes
        when (request.method.uppercase()) {
            "GET" -> builder.get()
            "DELETE" -> if (raw != null) {
                builder.delete(raw.toRequestBody(OCTET_STREAM))
            } else {
                builder.delete()
            }
            "POST" -> builder.post(
                raw?.toRequestBody(OCTET_STREAM)
                    ?: (request.body ?: "").toRequestBody(JSON)
            )
            "PATCH" -> builder.patch(
                raw?.toRequestBody(OCTET_STREAM)
                    ?: (request.body ?: "").toRequestBody(JSON)
            )
            else -> builder.method(
                request.method,
                raw?.toRequestBody(OCTET_STREAM)
                    ?: request.body?.toRequestBody(JSON),
            )
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
        val OCTET_STREAM = "application/octet-stream".toMediaType()
    }
}

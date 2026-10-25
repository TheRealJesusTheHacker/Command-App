package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport

/** Records every request and answers from a script the test supplies. */
class FakeTransport(
    var handler: (HttpRequest) -> HttpResponse = { HttpResponse(200, "{}") },
) : HttpTransport {
    val requests = mutableListOf<HttpRequest>()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        requests += request
        return handler(request)
    }

    override suspend fun download(request: HttpRequest, sink: (ByteArray) -> Unit): HttpResponse {
        requests += request
        sink("fake-apk-bytes".toByteArray())
        return HttpResponse(200, "")
    }
}

package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * hunt4 pagination completeness (owner catch: "I saw all clients
 * in pipeline but only 50 on that one screenshot"). The 120-row
 * fixtures are REAL envelopes captured from the byte-exact
 * production server code against a scratch DB seeded with 120
 * clients and 120 tickets (see RealBodies.CLIENTS_120).
 *
 * These tests emulate servers that cap the page below the app's
 * requested limit and prove the API layer walks offset pages until
 * the envelope total is loaded — the list can never again stop
 * silently at page 1.
 */
class PagingTest {

    private fun api(transport: FakeTransport) =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun queryOf(path: String): Map<String, String> =
        path.substringAfter("?", "")
            .split("&")
            .filter { it.contains("=") }
            .associate { it.substringBefore("=") to it.substringAfter("=") }

    /**
     * Serve [fullBody] (a real limit=200 envelope) the way the
     * production server does: filter by [filterField]/[filterParam]
     * and q=[searchField], then slice by limit/offset with the page
     * size capped at [cap] — totals always reflect the full
     * filtered set.
     */
    private fun pagingTransport(
        fullBody: String,
        key: String,
        cap: Int,
        filterParam: String? = null,
        filterField: String? = null,
        searchField: String? = null,
    ): FakeTransport {
        val root = Json.parseToJsonElement(fullBody).jsonObject
        val all = root.getValue(key).jsonArray
        return FakeTransport { request ->
            val params = queryOf(request.path)
            var rows: List<kotlinx.serialization.json.JsonElement> = all
            if (filterParam != null && filterField != null) {
                params[filterParam]?.let { wanted ->
                    rows = rows.filter {
                        it.jsonObject[filterField]?.jsonPrimitive?.content == wanted
                    }
                }
            }
            if (searchField != null) {
                params["q"]?.let { q ->
                    rows = rows.filter {
                        it.jsonObject[searchField]?.jsonPrimitive?.content
                            ?.contains(q, ignoreCase = true) == true
                    }
                }
            }
            val offset = params["offset"]?.toIntOrNull() ?: 0
            val limit = minOf(params["limit"]?.toIntOrNull() ?: 50, cap)
            val slice = rows.drop(offset).take(limit)
            val body = buildJsonObject {
                put("ok", true)
                put("total", rows.size)
                put("limit", limit)
                put("offset", offset)
                put(key, JsonArray(slice))
            }.toString()
            HttpResponse(200, body)
        }
    }

    private fun expectedIds(fullBody: String, key: String): List<Int> =
        Json.parseToJsonElement(fullBody).jsonObject.getValue(key).jsonArray
            .map { it.jsonObject.getValue("id").jsonPrimitive.content.toInt() }

    @Test
    fun clientsWalkEveryPageWhenTheServerCapsThePageSize() = runBlocking {
        val transport = pagingTransport(
            RealBodies.CLIENTS_120, "clients", cap = 50, searchField = "legalName",
        )
        val page = api(transport).clients()
        assertEquals(120, page.total)
        assertEquals(120, page.items.size)
        assertEquals(
            expectedIds(RealBodies.CLIENTS_120, "clients"),
            page.items.map { it.id },
        )
        // Three page requests, walking offsets 0 / 50 / 100.
        assertEquals(3, transport.requests.size)
        assertTrue(transport.requests[0].path.contains("offset=0"))
        assertTrue(transport.requests[1].path.contains("offset=50"))
        assertTrue(transport.requests[2].path.contains("offset=100"))
    }

    @Test
    fun clientsLoadInOneRequestWhenTheServerHonorsTheBigPage() = runBlocking {
        val transport = pagingTransport(
            RealBodies.CLIENTS_120, "clients", cap = 200, searchField = "legalName",
        )
        val page = api(transport).clients()
        assertEquals(120, page.total)
        assertEquals(120, page.items.size)
        assertEquals(1, transport.requests.size)
    }

    @Test
    fun clientSearchFindsTheRecordLivingOnPageThree() = runBlocking {
        val transport = pagingTransport(
            RealBodies.CLIENTS_120, "clients", cap = 50, searchField = "legalName",
        )
        val page = api(transport).clients("Zzfindme")
        assertEquals(1, page.total)
        assertEquals(1, page.items.size)
        assertEquals("Pagecheck Client 119 Zzfindme", page.items.single().legalName)
        assertTrue(transport.requests.single().path.contains("q=Zzfindme"))
    }

    @Test
    fun ticketsWalkEveryPageToo() = runBlocking {
        val transport = pagingTransport(
            RealBodies.TICKETS_120, "tickets", cap = 50,
            filterParam = "status", filterField = "status",
        )
        val page = api(transport).tickets()
        assertEquals(120, page.total)
        assertEquals(120, page.items.size)
        assertEquals(
            expectedIds(RealBodies.TICKETS_120, "tickets"),
            page.items.map { it.id },
        )
        assertEquals(3, transport.requests.size)
    }

    @Test
    fun ticketStatusFilterReportsItsOwnTrueTotal() = runBlocking {
        val transport = pagingTransport(
            RealBodies.TICKETS_120, "tickets", cap = 200,
            filterParam = "status", filterField = "status",
        )
        val page = api(transport).tickets(status = "New")
        assertEquals(70, page.total)
        assertEquals(70, page.items.size)
        assertTrue(page.items.all { it.status == "New" })
    }
}

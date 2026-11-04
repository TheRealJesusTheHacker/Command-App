package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream

/**
 * Phase 5 (hunt9) endpoint methods: Employees, Assets (incl.
 * flags / renewals / QR / labels), and Forms (records + the
 * form-files vault with its binary upload/download twins).
 *
 * Every method rides the deployed Phase 5 backend (Addendum 18;
 * production server.py sha256 6cd150bd…), whose payloads are the
 * web handlers' own — so these ride the open-JSON verbs on
 * [SentinelApi] exactly like Phases 3–4. Refusals surface the
 * server's own message via [SentinelApi.callJson] (the STAFF
 * employees 403, the OWNER-only gates, the PDF/25 MB wordings).
 *
 * Request bodies mirror phase5-work/rehearsal_capture.py field
 * for field.
 */

private fun JsonObject.toBody5(): String = toString()

// ---- Employees ----

suspend fun SentinelApi.employees(
    q: String = "",
    role: String = "All",
): JsonObject {
    val parts = mutableListOf("limit=200")
    if (q.isNotBlank()) parts += "q=${encodeQuery(q)}"
    if (role != "All") parts += "role=${encodeQuery(role)}"
    return callJson("GET", "/api/app/employees?" + parts.joinToString("&"))
}

suspend fun SentinelApi.employeesWorkload(): JsonObject =
    callJson("GET", "/api/app/employees/workload")

suspend fun SentinelApi.employeeDetail(id: Int): JsonObject =
    callJson("GET", "/api/app/employees/$id")

suspend fun SentinelApi.createEmployee(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/employees", body.toBody5())

suspend fun SentinelApi.editEmployee(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/employees/$id", body.toBody5())

suspend fun SentinelApi.setEmployeeActive(id: Int, active: Boolean): JsonObject =
    callJson(
        "POST", "/api/app/employees/$id/active",
        buildJsonObject { put("active", active) }.toBody5(),
    )

suspend fun SentinelApi.deleteEmployee(id: Int): JsonObject =
    deleteJson("/api/app/employees/$id")

suspend fun SentinelApi.revokeEmployeeSession(id: Int, sessionId: String): JsonObject =
    callJson("POST", "/api/app/employees/$id/sessions/$sessionId/revoke", "{}")

suspend fun SentinelApi.revokeAllEmployeeSessions(id: Int): JsonObject =
    callJson("POST", "/api/app/employees/$id/revoke-all-sessions", "{}")

suspend fun SentinelApi.setEmployeeModNote(id: Int, note: String): JsonObject =
    callJson(
        "POST", "/api/app/employees/$id/mod-note",
        buildJsonObject { put("note", note) }.toBody5(),
    )

suspend fun SentinelApi.setEmployeeSuspend(id: Int, until: Long): JsonObject =
    callJson(
        "POST", "/api/app/employees/$id/suspend",
        buildJsonObject { put("until", until) }.toBody5(),
    )

suspend fun SentinelApi.setEmployeeMustReset(id: Int, required: Boolean): JsonObject =
    callJson(
        "POST", "/api/app/employees/$id/must-reset",
        buildJsonObject { put("required", required) }.toBody5(),
    )

suspend fun SentinelApi.setEmployeeRole(id: Int, role: String): JsonObject =
    callJson(
        "POST", "/api/app/employees/$id/role",
        buildJsonObject { put("role", role) }.toBody5(),
    )

suspend fun SentinelApi.setEmployeeClient(id: Int, clientId: Int?): JsonObject =
    callJson(
        "POST", "/api/app/employees/$id/client",
        buildJsonObject {
            if (clientId == null) put("clientId", kotlinx.serialization.json.JsonNull)
            else put("clientId", clientId)
        }.toBody5(),
    )

suspend fun SentinelApi.createEmployeeInvite(role: String): JsonObject =
    callJson(
        "POST", "/api/app/employees/invites",
        buildJsonObject { put("role", role) }.toBody5(),
    )

suspend fun SentinelApi.revokeEmployeeInvite(id: Int): JsonObject =
    callJson("POST", "/api/app/employees/invites/$id/revoke", "{}")

// ---- Assets ----

suspend fun SentinelApi.assets(
    q: String = "",
    type: String? = null,
    status: String? = null,
    scope: String? = null,
    alert: String = "All",
    sort: String = "name",
    archived: Boolean = false,
): JsonObject {
    val parts = mutableListOf("limit=200")
    if (q.isNotBlank()) parts += "q=${encodeQuery(q)}"
    if (!type.isNullOrBlank()) parts += "type=${encodeQuery(type)}"
    if (!status.isNullOrBlank()) parts += "status=${encodeQuery(status)}"
    if (!scope.isNullOrBlank()) parts += "scope=${encodeQuery(scope)}"
    if (alert != "All") parts += "alert=${encodeQuery(alert)}"
    if (sort != "name") parts += "sort=${encodeQuery(sort)}"
    if (archived) parts += "archived=only"
    return callJson("GET", "/api/app/assets?" + parts.joinToString("&"))
}

suspend fun SentinelApi.assetDetail(id: Int): JsonObject =
    callJson("GET", "/api/app/assets/$id")

suspend fun SentinelApi.createAsset(item: JsonObject): JsonObject =
    callJson("POST", "/api/app/assets", buildJsonObject { put("item", item) }.toBody5())

suspend fun SentinelApi.editAsset(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/assets/$id", body.toBody5())

suspend fun SentinelApi.setAssetStatus(id: Int, status: String): JsonObject =
    callJson(
        "POST", "/api/app/assets/$id/status",
        buildJsonObject { put("status", status) }.toBody5(),
    )

suspend fun SentinelApi.archiveAsset(id: Int): JsonObject =
    callJson("POST", "/api/app/assets/$id/archive", "{}")

suspend fun SentinelApi.restoreAsset(id: Int): JsonObject =
    callJson("POST", "/api/app/assets/$id/restore", "{}")

suspend fun SentinelApi.deleteAsset(id: Int): JsonObject =
    deleteJson("/api/app/assets/$id")

suspend fun SentinelApi.ensureAssetQr(id: Int): JsonObject =
    callJson("POST", "/api/app/assets/$id/qr/ensure", "{}")

suspend fun SentinelApi.regenerateAssetQr(id: Int): JsonObject =
    callJson("POST", "/api/app/assets/$id/qr/regenerate", "{}")

suspend fun SentinelApi.assetLabels(): JsonObject =
    callJson("GET", "/api/app/assets/labels")

suspend fun SentinelApi.assetUrlLabels(): JsonObject =
    callJson("GET", "/api/app/assets/url-labels")

suspend fun SentinelApi.assetRenewals(): JsonObject =
    callJson("GET", "/api/app/assets/renewals")

suspend fun SentinelApi.resolveAssetQr(code: String): JsonObject =
    callJson("GET", "/api/app/assets/resolve?code=${encodeQuery(code)}")

/** hunt17 (final contract census): the client's vault files
 *  (expansion item 36) — the client profile carried no files
 *  surface before this pass. Downloads ride the form-files
 *  binary route the Forms vault already uses. */
suspend fun SentinelApi.clientFiles(clientId: Int): JsonObject =
    callJson("GET", "/api/app/clients/$clientId/files")

// ---- Forms: records ----

suspend fun SentinelApi.forms(q: String = "", clientId: Int? = null): JsonObject {
    val parts = mutableListOf("limit=200")
    if (q.isNotBlank()) parts += "q=${encodeQuery(q)}"
    if (clientId != null) parts += "clientId=$clientId"
    return callJson("GET", "/api/app/forms?" + parts.joinToString("&"))
}

suspend fun SentinelApi.formDetail(id: Int): JsonObject =
    callJson("GET", "/api/app/forms/$id")

suspend fun SentinelApi.createForm(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/forms", body.toBody5())

suspend fun SentinelApi.editForm(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/forms/$id", body.toBody5())

suspend fun SentinelApi.deleteForm(id: Int): JsonObject =
    deleteJson("/api/app/forms/$id")

// ---- Forms: form-files vault (binary twins) ----

suspend fun SentinelApi.formFiles(q: String = ""): JsonObject {
    val suffix = if (q.isBlank()) "" else "?q=${encodeQuery(q)}"
    return callJson("GET", "/api/app/form-files$suffix")
}

suspend fun SentinelApi.updateFormFile(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/form-files/$id", body.toBody5())

suspend fun SentinelApi.deleteFormFile(id: Int): JsonObject =
    deleteJson("/api/app/form-files/$id")

/** Binary twin of web POST /forms/upload: raw bytes + the web's
 *  X-File-Name / X-Form-Name / X-Form-Note headers (URL-encoded,
 *  exactly as web sends them). The server enforces the PDF rule
 *  and the 25 MB cap and answers in its own words. */
suspend fun SentinelApi.uploadFormFile(
    bytes: ByteArray,
    fileName: String,
    formName: String,
    formNote: String,
): JsonObject = uploadBytes(
    "/api/app/form-files/upload", bytes,
    mapOf(
        "Content-Type" to "application/octet-stream",
        "X-File-Name" to java.net.URLEncoder.encode(fileName, "UTF-8"),
        "X-Form-Name" to java.net.URLEncoder.encode(formName, "UTF-8"),
        "X-Form-Note" to java.net.URLEncoder.encode(formNote, "UTF-8"),
    ),
)

/** Binary twin of web GET /forms/download?id=: the file's bytes. */
suspend fun SentinelApi.downloadFormFile(id: Int): ByteArray {
    val out = ByteArrayOutputStream()
    downloadBytes("/api/app/form-files/$id/download") { chunk -> out.write(chunk) }
    return out.toByteArray()
}

package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.Ticket
import net.sentinelsafeguard.app.ui.MirrorVocab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure mirror logic: board order, money, directory mapping. */
class MirrorLogicTest {

    private fun ticket(
        id: Int,
        status: String = "Open",
        priority: String = "Normal",
        due: String? = null,
        created: Long = 0,
    ) = Ticket(
        id = id, status = status, priority = priority,
        dueDate = due, createdAt = created, title = "t$id",
    )

    @Test
    fun boardSortsPriorityThenDueThenNewest() {
        val sorted = MirrorVocab.sortBoardCards(
            listOf(
                ticket(1, priority = "Low", created = 300),
                ticket(2, priority = "Critical", due = "2026-11-05", created = 100),
                ticket(3, priority = "Critical", due = "2026-11-03", created = 50),
                ticket(4, priority = "Critical", created = 200),
                ticket(5, priority = "Normal", created = 400),
            )
        )
        assertEquals(listOf(3, 2, 4, 5, 1), sorted.map { it.id })
    }

    @Test
    fun boardGroupsCanonicalStagesPlusLegacyColumns() {
        val groups = MirrorVocab.groupBoard(
            listOf(
                ticket(1, status = "Open"),
                ticket(2, status = "Merged-ish legacy"),
                ticket(3, status = "New"),
            )
        )
        assertEquals(
            MirrorVocab.TICKET_STAGES + "Merged-ish legacy",
            groups.map { it.first },
        )
        assertEquals(1, groups.first { it.first == "Open" }.second.size)
    }

    @Test
    fun steppersStopAtTheEnds() {
        assertEquals("Open", MirrorVocab.stepStage("New", +1))
        assertEquals("On Hold", MirrorVocab.stepStage("In Progress", +1))
        assertNull(MirrorVocab.stepStage("New", -1))
        assertNull(MirrorVocab.stepStage("Closed", +1))
        assertNull(MirrorVocab.stepStage("Legacy", +1))
    }

    @Test
    fun moneyFormatsLikeWeb() {
        assertEquals("$350", MirrorVocab.formatCents(35000))
        assertEquals("$117,000", MirrorVocab.formatCents(11700000))
        assertEquals("$1,234.56", MirrorVocab.formatCents(123456))
        assertEquals("$2,500", MirrorVocab.formatDollars(2500.0))
        assertEquals("", MirrorVocab.formatDollars(null))
    }

    @Test
    fun directoryParamsMapToB3Filters() {
        val params = MirrorVocab.ticketDirectoryParams(
            q = "camera", stage = "Open", priority = null,
            overdueOnly = true, unassignedOnly = false,
            noCaseOnly = true, adOnly = false, sortLabel = "Due soonest",
        )
        assertEquals("camera", params["q"])
        assertEquals("Open", params["status"])
        assertEquals("1", params["overdue"])
        assertEquals("1", params["noCase"])
        assertEquals("due", params["sort"])
        assertTrue("priority" !in params)
        assertTrue("unassigned" !in params)
    }

    @Test
    fun clientAggregatesMatchWebStatCards() {
        val clients = listOf(
            Client(id = 1, legalName = "A", stage = "Lead", value = 100.0, source = "Google", tags = "tier-a"),
            Client(id = 2, legalName = "B", stage = "Active Client", value = 200.0, source = "Google"),
            Client(id = 3, legalName = "C", stage = "Lost", value = 999.0, source = "Academy"),
        )
        assertEquals(1, MirrorVocab.inPipelineCount(clients))
        assertEquals(300.0, MirrorVocab.pipelineValue(clients), 0.001)
        assertEquals(listOf("Google" to 2, "Academy" to 1), MirrorVocab.topSources(clients))
        assertEquals("tier-a", MirrorVocab.tierOf(clients[0]))
        assertEquals("", MirrorVocab.tierOf(clients[1]))
    }
}

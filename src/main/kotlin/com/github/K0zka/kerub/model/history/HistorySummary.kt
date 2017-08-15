package com.github.K0zka.kerub.model.history

import com.github.K0zka.kerub.model.Range
import java.io.Serializable
import java.util.UUID

data class HistorySummary(
		override val id: UUID = UUID.randomUUID(),
		override val entityKey: Any,
		override val appVersion: String?,
		val time: Range<Long>,
		val changes: List<PropertyChange>
) : HistoryEntry, Serializable
package com.github.kerubistan.kerub.model

import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonView
import com.github.kerubistan.kerub.model.views.Detailed
import com.github.kerubistan.kerub.model.views.Simple
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.search.annotations.DocumentId
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import java.util.UUID

/**
 * A host represents a server, a physical computation resource, that can be used by virtual resources.
 */
@Indexed
@JsonTypeName("host")
data class Host constructor(
		@JsonView(Simple::class)
		@DocumentId
		@JsonProperty("id")
		override
		val id: UUID = UUID.randomUUID(),
		@Field
		@JsonProperty("address")
		val address: String,
		@JsonView(Detailed::class)
		@Field
		@JsonProperty("publickey")
		val publicKey: String,
		@JsonView(Simple::class)
		@Field
		@JsonProperty("dedicated")
		val dedicated: Boolean,
		@Field
		@JsonProperty("capabilities")
		@JsonView(Detailed::class)
		val capabilities: HostCapabilities? = null,
		val recycling: Boolean = false,
		val dead: Boolean = false
)
	: Entity<UUID> {
	override fun toString(): String = "Host(id=$id,addr=$address)"
}

package com.github.kerubistan.kerub.model.messages

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("ping")
data class PingMessage @JsonCreator constructor(val sent: Long?) : Message

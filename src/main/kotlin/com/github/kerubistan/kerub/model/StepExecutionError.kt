package com.github.kerubistan.kerub.model

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("err")
data class StepExecutionError(
		override val executionStep: ExecutionStep,
		val timestamp: Long = System.currentTimeMillis(),
		val error: String
) : StepExecutionResult
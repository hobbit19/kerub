package com.github.kerubistan.kerub.model

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("skip")
class StepExecutionSkip(override val executionStep: ExecutionStep) : StepExecutionResult
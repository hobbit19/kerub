package com.github.K0zka.kerub.utils.junix.benchmarks.fio

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class FioBenchmarkResults @JsonCreator constructor(
		@JsonProperty("jobs") val jobs: List<FioJob>
)

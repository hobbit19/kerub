package com.github.kerubistan.kerub.planner.steps.vstorage.fs.rebase

import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.steps.AbstractOperationalStepFactory

object FallocateImageFactory : AbstractOperationalStepFactory<FallocateImage>() {
	override fun produce(state: OperationalState): List<FallocateImage> {
		//pending: for this the aggregated history of the disk should be used
		// but that is not yet in operational state
		TODO()
	}
}
package com.github.kerubistan.kerub.planner.steps.vm.resume

import com.github.kerubistan.kerub.model.VirtualMachineStatus
import com.github.kerubistan.kerub.model.collection.VirtualMachineDataCollection
import com.github.kerubistan.kerub.model.dynamic.VirtualMachineDynamic
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.steps.vm.base.AbstractForEachVmStepFactory

object ResumeVirtualMachineFactory : AbstractForEachVmStepFactory<ResumeVirtualMachine>() {
	override fun filter(vmDyn: VirtualMachineDynamic): Boolean
			= vmDyn.status == VirtualMachineStatus.Paused

	override fun create(vmData: VirtualMachineDataCollection, state: OperationalState): ResumeVirtualMachine
			= ResumeVirtualMachine(
			vm = vmData.stat,
			host = getHost(requireNotNull(vmData.dynamic).hostId, state).stat
	)
}
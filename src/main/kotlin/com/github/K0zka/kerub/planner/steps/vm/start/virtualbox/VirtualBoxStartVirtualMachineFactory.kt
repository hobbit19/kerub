package com.github.K0zka.kerub.planner.steps.vm.start.virtualbox

import com.github.K0zka.kerub.planner.OperationalState
import com.github.K0zka.kerub.planner.steps.vm.match
import com.github.K0zka.kerub.planner.steps.vm.start.AbstractStartVmFactory
import com.github.K0zka.kerub.planner.steps.vm.storageAllocationMap
import com.github.K0zka.kerub.utils.junix.virt.vbox.VBoxManage
import com.github.K0zka.kerub.utils.times

object VirtualBoxStartVirtualMachineFactory : AbstractStartVmFactory<VirtualBoxStartVirtualMachine>() {
	override fun produce(state: OperationalState): List<VirtualBoxStartVirtualMachine> =
			(getVmsToStart(state) * getWorkingHosts(state) { hostData ->
				VBoxManage.available(hostData.stat.capabilities)
						&& isHwVirtualizationSupported(hostData.stat)
			}.toList()).filter {
				val vm = it.first
				val host = it.second.stat
				match(
						host = host,
						vm = vm,
						dyn = it.second.dynamic,
						vStorage = storageAllocationMap(state, vm.virtualStorageLinks)
				)
			}.map { VirtualBoxStartVirtualMachine(vm = it.first, host = it.second.stat) }

}
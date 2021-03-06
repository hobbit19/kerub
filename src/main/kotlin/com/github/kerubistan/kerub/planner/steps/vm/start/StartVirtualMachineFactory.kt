package com.github.kerubistan.kerub.planner.steps.vm.start

import com.github.kerubistan.kerub.planner.steps.StepFactoryCollection
import com.github.kerubistan.kerub.planner.steps.vm.start.kvm.KvmStartVirtualMachineFactory
import com.github.kerubistan.kerub.planner.steps.vm.start.virtualbox.VirtualBoxStartVirtualMachineFactory

object StartVirtualMachineFactory : StepFactoryCollection(listOf(
		KvmStartVirtualMachineFactory,
		VirtualBoxStartVirtualMachineFactory
		//xen, bhyve, etc
))

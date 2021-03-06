package com.github.kerubistan.kerub.planner.steps.vm.stop

import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.VirtualMachine
import com.github.kerubistan.kerub.model.VirtualMachineStatus
import com.github.kerubistan.kerub.model.dynamic.VirtualMachineDynamic
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.utils.now
import com.github.kerubistan.kerub.utils.toSize
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class StopVirtualMachineFactoryTest {
	@Test
	fun produceWithBlankState() {
		val state = OperationalState.fromLists()
		val steps = StopVirtualMachineFactory.produce(state)
		Assert.assertTrue(steps.isEmpty())
	}

	@Test
	fun produceWithSingleRunningVm() {
		val vm = VirtualMachine(
				 name = "test-vm",
		         id = UUID.randomUUID(),
		         nrOfCpus = 1
		                       )
		val host = Host(
				id = UUID.randomUUID(),
		        address = "host-1.example.com",
		        dedicated = true,
		        publicKey = "test"
		               )
		val vmDyn = VirtualMachineDynamic(
				id = vm.id,
		        hostId = host.id,
		        status = VirtualMachineStatus.Up,
		        memoryUsed = "1 GB".toSize(),
		        lastUpdated = now()
		                                 )
		val state = OperationalState.fromLists(vms = listOf(vm), hosts = listOf(host), vmDyns = listOf(vmDyn))
		val steps = StopVirtualMachineFactory.produce(state)
		Assert.assertTrue(steps.isNotEmpty())
	}

}
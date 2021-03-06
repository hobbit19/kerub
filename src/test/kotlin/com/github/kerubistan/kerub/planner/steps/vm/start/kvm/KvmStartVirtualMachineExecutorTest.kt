package com.github.kerubistan.kerub.planner.steps.vm.start.kvm

import com.github.kerubistan.kerub.data.dynamic.VirtualMachineDynamicDao
import com.github.kerubistan.kerub.host.FireWall
import com.github.kerubistan.kerub.host.HostManager
import com.github.kerubistan.kerub.hypervisor.Hypervisor
import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.Range
import com.github.kerubistan.kerub.model.VirtualMachine
import com.github.kerubistan.kerub.model.VirtualMachineStatus
import com.github.kerubistan.kerub.model.display.RemoteConsoleProtocol
import com.github.kerubistan.kerub.model.dynamic.VirtualMachineDynamic
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.mockito.Mockito
import java.math.BigInteger
import java.util.UUID

class KvmStartVirtualMachineExecutorTest {
	val hostManager: HostManager = mock()

	val vmDynDao: VirtualMachineDynamicDao = mock()

	val hypervisor: Hypervisor = mock()

	val firewall: FireWall = mock()

	@Test
	fun execute() {
		val host = Host(
				address = "127.0.0.1",
				publicKey = "",
				dedicated = false
		)

		val vm = VirtualMachine(
				name = "",
				id = UUID.randomUUID(),
				nrOfCpus = 1,
				memory = Range(BigInteger("128"), BigInteger("256"))
		)

		val step = KvmStartVirtualMachine(
				host = host,
				vm = vm
		)

		whenever(hostManager.getHypervisor(any())).thenReturn(hypervisor)
		whenever(hypervisor.getDisplay(Mockito.any(VirtualMachine::class.java) ?: vm)).thenReturn(RemoteConsoleProtocol.spice to 5900)
		whenever(hostManager.getFireWall(Mockito.any(Host::class.java) ?: host)).thenReturn(firewall)

		KvmStartVirtualMachineExecutor(hostManager, vmDynDao).execute(step)

		verify(hypervisor).startVm(
				//TODO: eq is failing with VM in kotlin-mockito (test framework bug)
				Mockito.eq(vm) ?: vm,
				any()
		)
		verify(firewall).open(eq(5900), eq("tcp"))
		verify(vmDynDao).update(Mockito.any(VirtualMachineDynamic::class.java) ?: VirtualMachineDynamic(
				id = step.vm.id,
				status = VirtualMachineStatus.Up,
				memoryUsed = BigInteger.ZERO,
				hostId = step.host.id
		))
	}

}
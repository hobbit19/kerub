package com.github.kerubistan.kerub.services.impl

import com.github.kerubistan.kerub.data.VirtualMachineDao
import com.github.kerubistan.kerub.data.dynamic.VirtualMachineDynamicDao
import com.github.kerubistan.kerub.model.VirtualMachine
import com.github.kerubistan.kerub.model.VirtualMachineStatus
import com.github.kerubistan.kerub.model.dynamic.DisplaySettings
import com.github.kerubistan.kerub.model.dynamic.VirtualMachineDynamic
import com.github.kerubistan.kerub.utils.now
import com.github.kerubistan.kerub.utils.toSize
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.hamcrest.CoreMatchers
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.UUID

class VirtualMachineDynamicServiceImplTest {

	val dao: VirtualMachineDynamicDao = mock()
	val vmDao : VirtualMachineDao = mock()

	val vm = VirtualMachine(
			id = UUID.randomUUID(),
			name = "vm-1",
			nrOfCpus = 1
	)

	val vmDyn = VirtualMachineDynamic(
			id = vm.id,
			status = VirtualMachineStatus.Up,
			displaySetting = DisplaySettings(
					hostAddr = "host1.example.com",
					port = 5900,
					password = "wilmaletmein",
					ca = ""
			),
			lastUpdated = now(),
			hostId = UUID.randomUUID(),
			memoryUsed = "512 MB".toSize()
	)

	@Test
	fun spiceConnection() {
		whenever(dao[vm.id]).thenReturn(vmDyn)
		whenever(vmDao[vm.id]).thenReturn(vm)

		val result = VirtualMachineDynamicServiceImpl(dao, vmDao).spiceConnection(vm.id)
		assertThat(result, CoreMatchers.containsString(vmDyn.displaySetting?.hostAddr))
		assertThat(result, CoreMatchers.containsString(vmDyn.displaySetting?.port?.toString()))
		assertThat(result, CoreMatchers.containsString(vmDyn.displaySetting?.password))

		verify(dao)[vm.id]
		verify(vmDao)[vm.id]
	}
}
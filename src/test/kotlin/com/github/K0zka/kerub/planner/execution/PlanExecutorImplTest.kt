package com.github.K0zka.kerub.planner.execution

import com.github.K0zka.kerub.data.config.HostConfigurationDao
import com.github.K0zka.kerub.data.dynamic.HostDynamicDao
import com.github.K0zka.kerub.data.dynamic.VirtualMachineDynamicDao
import com.github.K0zka.kerub.data.dynamic.VirtualStorageDeviceDynamicDao
import com.github.K0zka.kerub.host.HostCommandExecutor
import com.github.K0zka.kerub.host.HostManager
import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.model.VirtualStorageDevice
import com.github.K0zka.kerub.model.io.VirtualDiskFormat
import com.github.K0zka.kerub.planner.OperationalState
import com.github.K0zka.kerub.planner.Plan
import com.github.K0zka.kerub.planner.steps.vstorage.fs.create.CreateImage
import com.github.K0zka.kerub.utils.toSize
import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import java.util.UUID

@RunWith(MockitoJUnitRunner::class) class PlanExecutorImplTest {
	@Mock
	var executor: HostCommandExecutor? = null

	@Mock
	val hostConfigDao = mock<HostConfigurationDao>()

	@Mock
	var hostManager: HostManager? = null

	@Mock
	var hostDynamicDao: HostDynamicDao? = null

	@Mock
	var vmDynamicDao: VirtualMachineDynamicDao? = null

	@Mock
	var virtualStorageDeviceDynamicDao: VirtualStorageDeviceDynamicDao? = null

	@Test
	fun execute() {
		val host = Host(
				id = UUID.randomUUID(),
		        address = "127.0.0.1",
		        dedicated = true,
		        publicKey = ""
		               )
		val plan = Plan(
				state = OperationalState.fromLists(
						vms = listOf(),
						hosts = listOf(host),
						hostDyns = listOf(),
						vmDyns = listOf()
				                        ),
				steps = listOf(CreateImage(
						host = host,
						disk = VirtualStorageDevice(
								size = "100 MB".toSize(),
								name = "foo"
						                             ),
						path = "/var/",
						format = VirtualDiskFormat.qcow2
				                            )
				                )
		               )
		PlanExecutorImpl(
				executor!!,
				hostManager!!,
				hostDynamicDao!!,
				vmDynamicDao!!,
				virtualStorageDeviceDynamicDao!!,
				hostConfigDao
		).execute(plan, {})

		//Mockito.verify(executor)!!.execute(Matchers.eq(host) ?: host, Matchers.any() ?: Mockito.mock() )
	}
}
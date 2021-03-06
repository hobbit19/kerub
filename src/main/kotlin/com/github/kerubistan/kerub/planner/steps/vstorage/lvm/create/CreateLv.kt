package com.github.kerubistan.kerub.planner.steps.vstorage.lvm.create

import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.VirtualStorageDevice
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageDeviceDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageLvmAllocation
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.costs.Cost
import com.github.kerubistan.kerub.planner.costs.IOCost
import com.github.kerubistan.kerub.planner.steps.vstorage.AbstractCreateVirtualStorage
import com.github.kerubistan.kerub.planner.steps.vstorage.lvm.base.updateHostDynLvmWithAllocation
import com.github.kerubistan.kerub.utils.update

class CreateLv(
		override val host: Host,
		val volumeGroupName: String,
		override val disk: VirtualStorageDevice
) : AbstractCreateVirtualStorage {

	override fun getCost(): List<Cost> {
		return listOf(
				IOCost(2048, host)
		)
	}

	override fun take(state: OperationalState): OperationalState = state.copy(
			vStorage = state.vStorage.update(disk.id) {
				it.copy(dynamic = VirtualStorageDeviceDynamic(
						id = disk.id,
						allocations = listOf(VirtualStorageLvmAllocation(
								hostId = host.id,
								actualSize = disk.size,
								path = ""
						))
				))
			},
			hosts = state.hosts.update(host.id) {
				it.copy(dynamic = updateHostDynLvmWithAllocation(state, host, volumeGroupName, disk.size))
			}
	)

}
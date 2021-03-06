package com.github.kerubistan.kerub.planner.steps.vstorage.gvinum.create

import com.github.kerubistan.kerub.data.dynamic.HostDynamicDao
import com.github.kerubistan.kerub.data.dynamic.VirtualStorageDeviceDynamicDao
import com.github.kerubistan.kerub.host.HostCommandExecutor
import com.github.kerubistan.kerub.model.GvinumStorageCapability
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageDeviceDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageGvinumAllocation
import com.github.kerubistan.kerub.model.dynamic.gvinum.ConcatenatedGvinumConfiguration
import com.github.kerubistan.kerub.model.dynamic.gvinum.SimpleGvinumConfiguration
import com.github.kerubistan.kerub.planner.execution.AbstractStepExecutor
import com.github.kerubistan.kerub.planner.steps.replace
import com.github.kerubistan.kerub.utils.junix.storagemanager.gvinum.GVinum

class CreateGvinumVolumeExecutor(
		private val hostCommandExecutor: HostCommandExecutor,
		private val virtualDiskDynDao: VirtualStorageDeviceDynamicDao,
		private val hostDynamicDao: HostDynamicDao
) : AbstractStepExecutor<CreateGvinumVolume, Unit>() {
	override fun update(step: CreateGvinumVolume, updates: Unit) {
		virtualDiskDynDao.add(
				VirtualStorageDeviceDynamic(
						id = step.disk.id,
						allocations = listOf(VirtualStorageGvinumAllocation(
								hostId = step.host.id,
								actualSize = step.disk.size,
								configuration = step.config
						))
				)
		)
		when (step.config) {
			is ConcatenatedGvinumConfiguration -> {

			}
			is SimpleGvinumConfiguration -> {
				hostDynamicDao.update(step.host.id) {
					dyn ->
					dyn.copy(
							storageStatus = dyn.storageStatus.replace({
								it.id == step.config.diskId
							}, {
								require(it.freeCapacity > step.disk.size) {
									"Free capacity is ${it.freeCapacity}, required ${step.disk.size}"
								}
								it.copy(
										freeCapacity = it.freeCapacity - step.disk.size
								)
							})
					)
				}
			}
			else -> TODO("Can not update the status with gvinum configuration type ${step.config}")
		}
	}

	override fun perform(step: CreateGvinumVolume) {
		hostCommandExecutor.execute(step.host) {
			session ->
			when (step.config) {
				is ConcatenatedGvinumConfiguration -> {
					GVinum.createConcatenatedVolume(
							session = session,
							volName = step.disk.id.toString(),
							disks = step.config.disks
					)
				}
				is SimpleGvinumConfiguration -> {
					val storage = step.host.capabilities?.storageCapabilities?.firstOrNull {
						it is GvinumStorageCapability && it.id == step.config.diskId
					} as GvinumStorageCapability
					GVinum.createSimpleVolume(session = session, volName = step.disk.id.toString(), disk = storage.name, size = step.disk.size)
				}
				else -> {
					TODO("gvinum configuration not implemented by executor: ${step.config}")
				}
			}
		}
	}

}
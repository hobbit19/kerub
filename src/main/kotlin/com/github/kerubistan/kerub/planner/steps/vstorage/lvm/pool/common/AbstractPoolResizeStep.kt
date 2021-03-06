package com.github.kerubistan.kerub.planner.steps.vstorage.lvm.pool.common

import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.config.LvmPoolConfiguration
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.steps.AbstractOperationalStep
import com.github.kerubistan.kerub.planner.steps.vstorage.lvm.base.updateHostDynLvmWithAllocation
import com.github.kerubistan.kerub.utils.update
import java.math.BigInteger

abstract class AbstractPoolResizeStep : AbstractOperationalStep {
	abstract val host: Host
	abstract val vgName: String
	abstract val pool: String
	override fun take(state: OperationalState): OperationalState = state.copy(
			hosts = state.hosts.update(host.id) { hostData ->
				hostData.copy(
						config = requireNotNull(hostData.config).let { hostConfig ->
							hostConfig.copy(
									storageConfiguration = hostConfig.storageConfiguration.map {
										if (it is LvmPoolConfiguration && it.poolName == pool) {
											it.copy(size = it.size + sizeChange())
										} else it
									}
							)
						},
						dynamic = updateHostDynLvmWithAllocation(state, host, vgName, sizeChange())
				)
			}
	)

	internal abstract fun sizeChange(): BigInteger

}
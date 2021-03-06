package com.github.kerubistan.kerub.planner.steps.host.startup

import com.github.kerubistan.kerub.model.dynamic.HostStatus
import com.github.kerubistan.kerub.model.lom.WakeOnLanInfo
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.steps.AbstractOperationalStepFactory
import com.github.kerubistan.kerub.planner.steps.factoryFeature
import com.github.kerubistan.kerub.utils.join

/**
 * Generates steps to wake up non-running hosts.
 */
object WakeHostFactory : AbstractOperationalStepFactory<AbstractWakeHost>() {
	override fun produce(state: OperationalState): List<AbstractWakeHost> =
			factoryFeature(state.controllerConfig.powerManagementEnabled) {
				state.hosts.values.filter {
					it.stat.capabilities?.powerManagment?.isNotEmpty() ?: false
							&& (it.dynamic == null || it.dynamic.status == HostStatus.Down)

				}.map {
					(stat) ->
					stat.capabilities?.powerManagment?.map {
						lom ->
						when (lom) {
							is WakeOnLanInfo -> {
								if (state.controllerConfig.wakeOnLanEnabled) {
									WolWakeHost(stat)
								} else null
							}
							else -> null
						}
					} ?: listOf()
				}.join().filterNotNull()

			}
}
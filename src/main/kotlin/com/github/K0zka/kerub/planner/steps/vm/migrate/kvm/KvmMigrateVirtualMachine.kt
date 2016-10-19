package com.github.K0zka.kerub.planner.steps.vm.migrate.kvm

import com.github.K0zka.kerub.model.Constrained
import com.github.K0zka.kerub.model.Expectation
import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.model.VirtualMachine
import com.github.K0zka.kerub.model.expectations.NoMigrationExpectation
import com.github.K0zka.kerub.model.expectations.NotSameHostExpectation
import com.github.K0zka.kerub.planner.OperationalState
import com.github.K0zka.kerub.planner.costs.ComputationCost
import com.github.K0zka.kerub.planner.costs.Cost
import com.github.K0zka.kerub.planner.costs.NetworkCost
import com.github.K0zka.kerub.planner.costs.Risk
import com.github.K0zka.kerub.planner.reservations.Reservation
import com.github.K0zka.kerub.planner.reservations.UseHostReservation
import com.github.K0zka.kerub.planner.reservations.VmReservation
import com.github.K0zka.kerub.planner.steps.AbstractOperationalStep
import com.github.K0zka.kerub.utils.update
import java.math.BigInteger
import java.util.ArrayList
import java.util.HashMap

data class KvmMigrateVirtualMachine(
		val vm: VirtualMachine,
		val source: Host,
		val target: Host) : AbstractOperationalStep {

	override fun reservations(): List<Reservation<*>>
			= listOf(VmReservation(vm),
			UseHostReservation(target),
			UseHostReservation(source)
	)

	override fun violations(state: OperationalState): Map<Constrained<Expectation>, List<Expectation>> {
		/**
		 * TODO
		 */
		val ret = HashMap<Constrained<Expectation>, List<Expectation>>()
		val vmViolations = ArrayList<Expectation>()
		for (expectation in vm.expectations) {
			when (expectation) {
				is NoMigrationExpectation ->
					vmViolations.add(expectation)
				is NotSameHostExpectation ->
					if (state.vmsOnHost(target.id).any { expectation.otherVmId == it.id }) {
						vmViolations.add(expectation)
					}
			//TODO: and so on
			}
		}
		return ret
	}

	override fun take(state: OperationalState): OperationalState {
		val vmDyn = requireNotNull(state.vms[vm.id]?.dynamic)
		val targetHostDyn = requireNotNull(state.hosts[target.id]?.dynamic)
		val sourceHostDyn = requireNotNull(state.hosts[source.id]?.dynamic)
		return state.copy(
				vms = state.vms.update(vm.id) {
					it.copy(dynamic = vmDyn.copy(
							hostId = target.id
					)
					)
				},
				hosts = state.hosts
						.update(target.id) {
							it.copy(
									dynamic = targetHostDyn.copy(
											memFree = sourceHostDyn.memFree!! + vmDyn.memoryUsed,
											memUsed = sourceHostDyn.memUsed ?: BigInteger.ZERO - vmDyn.memoryUsed
									)
							)
						}
						.update(source.id) {
							it.copy(
									dynamic = sourceHostDyn.copy(
											memFree = sourceHostDyn.memFree!! + vmDyn.memoryUsed,
											memUsed = sourceHostDyn.memUsed ?: BigInteger.ZERO - vmDyn.memoryUsed
									)
							)
						}
		)
	}

	override fun getCost(): List<Cost> = listOf(
			/*
			 * TODO
			 * This calculates cost based on the max, which is pessimistic
			 * rather than realistic.
			 */
			NetworkCost(
					hosts = listOf(source, target),
					bytes = vm.memory.max.toLong()
			),
			ComputationCost(
					host = target,
					cycles = vm.memory.max.toLong()
			),
			ComputationCost(
					host = source,
					cycles = vm.memory.max.toLong()
			)

	) + if (vm.expectations.any { it is NoMigrationExpectation }) {
		listOf(Risk(1000, comment = "broken no-migration rule"))
	} else {
		listOf<Cost>()
	}
}
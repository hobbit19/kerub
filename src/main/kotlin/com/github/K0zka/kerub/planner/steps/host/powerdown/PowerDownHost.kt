package com.github.K0zka.kerub.planner.steps.host.powerdown

import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.planner.OperationalState
import com.github.K0zka.kerub.planner.costs.Cost
import com.github.K0zka.kerub.planner.reservations.FullHostReservation
import com.github.K0zka.kerub.planner.reservations.Reservation
import com.github.K0zka.kerub.planner.steps.AbstractOperationalStep

data class PowerDownHost(val host: Host) : AbstractOperationalStep {
	override fun getCost(): List<Cost> {
		return listOf()
	}

	override fun reservations(): List<Reservation<Host>>
			= listOf(FullHostReservation(host))

	override fun take(state: OperationalState): OperationalState {
		return state.copy(
				hostDyns = state.hostDyns.filterNot { it.key == host.id }
		)
	}
}
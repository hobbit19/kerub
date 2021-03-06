package com.github.kerubistan.kerub.planner.steps

import com.github.kerubistan.kerub.model.Constrained
import com.github.kerubistan.kerub.model.ExecutionStep
import com.github.kerubistan.kerub.model.Expectation
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.Plan
import com.github.kerubistan.kerub.planner.costs.Cost
import com.github.kerubistan.kerub.planner.reservations.Reservation
import com.github.k0zka.finder4j.backtrack.Step

interface AbstractOperationalStep : Step<Plan>, ExecutionStep {

	/**
	 * Take an operational state transformation step
	 */
	fun take(state: OperationalState): OperationalState

	/**
	 *
	 */
	override fun take(state: Plan): Plan =
			Plan(
					state = take(state.state),
					steps = state.steps + this
			)

	/**
	 * Get the list of costs expected at executing this step.
	 * Default implementation returns an empty list, meaning negligible
	 * costs.
	 */
	fun getCost(): List<Cost> = listOf()

	/**
	 * Returns a map of violated resources.
	 */
	fun violations(state: OperationalState)
			: Map<Constrained<Expectation>, List<Expectation>>
			= mapOf()

	/**
	 * List both the physical and virtual resources reserved for the execution of the step.
	 *
	 * This method does not have a default implementation since I have frequently made mistakes
	 * by leaving the default implementation. Actually the default empty list rarely makes sense.
	 */
	fun reservations(): List<Reservation<*>>
}
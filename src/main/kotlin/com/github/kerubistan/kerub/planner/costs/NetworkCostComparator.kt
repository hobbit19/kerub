package com.github.kerubistan.kerub.planner.costs

import java.util.Comparator

object NetworkCostComparator : Comparator<NetworkCost> {

	fun cost(networkCost: NetworkCost) = networkCost.bytes * (networkCost.hosts.size + 1)

	override fun compare(first: NetworkCost, second: NetworkCost): Int =
			(cost(first) - cost(second)).toInt()
}
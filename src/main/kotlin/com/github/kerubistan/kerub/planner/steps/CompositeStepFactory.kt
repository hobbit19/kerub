package com.github.kerubistan.kerub.planner.steps

import com.github.k0zka.finder4j.backtrack.StepFactory
import com.github.kerubistan.kerub.model.expectations.NotSameStorageExpectation
import com.github.kerubistan.kerub.model.expectations.StorageAvailabilityExpectation
import com.github.kerubistan.kerub.model.expectations.VirtualMachineAvailabilityExpectation
import com.github.kerubistan.kerub.planner.Plan
import com.github.kerubistan.kerub.planner.PlanViolationDetector
import com.github.kerubistan.kerub.planner.issues.problems.CompositeProblemDetectorImpl
import com.github.kerubistan.kerub.planner.issues.problems.ProblemDetector
import com.github.kerubistan.kerub.planner.issues.problems.hosts.RecyclingHost
import com.github.kerubistan.kerub.planner.issues.problems.hosts.UnusedService
import com.github.kerubistan.kerub.planner.issues.problems.vms.VmOnRecyclingHost
import com.github.kerubistan.kerub.planner.steps.host.powerdown.PowerDownHostFactory
import com.github.kerubistan.kerub.planner.steps.host.recycle.RecycleHostFactory
import com.github.kerubistan.kerub.planner.steps.host.startup.WakeHostFactory
import com.github.kerubistan.kerub.planner.steps.vm.migrate.MigrateVirtualMachineFactory
import com.github.kerubistan.kerub.planner.steps.vm.migrate.kvm.KvmMigrateVirtualMachineFactory
import com.github.kerubistan.kerub.planner.steps.vm.start.StartVirtualMachineFactory
import com.github.kerubistan.kerub.planner.steps.vm.stop.StopVirtualMachineFactory
import com.github.kerubistan.kerub.planner.steps.vstorage.CreateDiskFactory
import com.github.kerubistan.kerub.planner.steps.vstorage.migrate.MigrateVirtualStorageDeviceFactory
import com.github.kerubistan.kerub.planner.steps.vstorage.share.iscsi.IscsiShareFactory
import com.github.kerubistan.kerub.utils.getLogger
import com.github.kerubistan.kerub.utils.join
import kotlin.reflect.KClass

class CompositeStepFactory(
		private val planViolationDetector: PlanViolationDetector,
		private val problemDetector: ProblemDetector<*> = CompositeProblemDetectorImpl
) :
		StepFactory<AbstractOperationalStep, Plan> {

	private val logger = getLogger(CompositeStepFactory::class)

	private val defaultFactories = setOf(MigrateVirtualMachineFactory, MigrateVirtualStorageDeviceFactory,
										 PowerDownHostFactory, StartVirtualMachineFactory, StopVirtualMachineFactory,
										 RecycleHostFactory)

	private val factories = mapOf<KClass<*>, Set<AbstractOperationalStepFactory<*>>>(
			VirtualMachineAvailabilityExpectation::class
					to setOf(StartVirtualMachineFactory, CreateDiskFactory, StopVirtualMachineFactory,
							 KvmMigrateVirtualMachineFactory, WakeHostFactory, IscsiShareFactory),
			NotSameStorageExpectation::class to setOf(MigrateVirtualStorageDeviceFactory, WakeHostFactory,
													  MigrateVirtualMachineFactory),
			StorageAvailabilityExpectation::class to setOf(CreateDiskFactory, WakeHostFactory,
														   MigrateVirtualMachineFactory)
	)

	private val problems = mapOf(
			RecyclingHost::class to setOf(MigrateVirtualMachineFactory, PowerDownHostFactory, RecycleHostFactory),
			UnusedService::class to setOf(),
			VmOnRecyclingHost::class to setOf()
	)

	override fun produce(state: Plan): List<AbstractOperationalStep> {
		val unsatisfiedExpectations = planViolationDetector.listViolations(state)
		logger.debug("unsatisfied expectations: {}", unsatisfiedExpectations)
		val stepFactories = unsatisfiedExpectations.values.join()
				.map {
					factories[it.javaClass.kotlin] ?: defaultFactories
				}.join().toSet()


		val planProblems = problemDetector.detect(state)
		logger.debug("problems {}", planProblems)
		val problemStepFactories = planProblems
				.map { it.javaClass.kotlin }
				.map { problems[it] ?: defaultFactories }.join().distinct()

		val steps = sort(list = (stepFactories + problemStepFactories).map { it.produce(state.state) }.join(), state = state)
		logger.debug("steps generated: {}", steps)

		return steps
	}

	internal fun sort(list: List<AbstractOperationalStep>,
					  detector: ProblemDetector<*> = CompositeProblemDetectorImpl,
					  state: Plan): List<AbstractOperationalStep> =
			list.sortedWith(StepBenefitComparator(planViolationDetector, state).reversed()
									.thenComparing(StepProblemsComparator(plan = state, detector = detector).reversed())
									.thenComparing(StepCostComparator).reversed())

}
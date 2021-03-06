package com.github.kerubistan.kerub.stories.planner

import com.github.k0zka.finder4j.backtrack.BacktrackService
import com.github.k0zka.finder4j.backtrack.BacktrackServiceImpl
import com.github.kerubistan.kerub.model.ExpectationLevel
import com.github.kerubistan.kerub.model.FsStorageCapability
import com.github.kerubistan.kerub.model.GvinumStorageCapability
import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.HostCapabilities
import com.github.kerubistan.kerub.model.LvmStorageCapability
import com.github.kerubistan.kerub.model.OperatingSystem
import com.github.kerubistan.kerub.model.Range
import com.github.kerubistan.kerub.model.SoftwarePackage
import com.github.kerubistan.kerub.model.Version
import com.github.kerubistan.kerub.model.VirtualMachine
import com.github.kerubistan.kerub.model.VirtualMachineStatus
import com.github.kerubistan.kerub.model.VirtualStorageDevice
import com.github.kerubistan.kerub.model.VirtualStorageLink
import com.github.kerubistan.kerub.model.config.HostConfiguration
import com.github.kerubistan.kerub.model.controller.config.ControllerConfig
import com.github.kerubistan.kerub.model.dynamic.HostDynamic
import com.github.kerubistan.kerub.model.dynamic.HostStatus
import com.github.kerubistan.kerub.model.dynamic.StorageDeviceDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualMachineDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageDeviceDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageFsAllocation
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageLvmAllocation
import com.github.kerubistan.kerub.model.dynamic.gvinum.ConcatenatedGvinumConfiguration
import com.github.kerubistan.kerub.model.expectations.CacheSizeExpectation
import com.github.kerubistan.kerub.model.expectations.ChassisManufacturerExpectation
import com.github.kerubistan.kerub.model.expectations.ClockFrequencyExpectation
import com.github.kerubistan.kerub.model.expectations.CoreDedicationExpectation
import com.github.kerubistan.kerub.model.expectations.CpuArchitectureExpectation
import com.github.kerubistan.kerub.model.expectations.MemoryClockFrequencyExpectation
import com.github.kerubistan.kerub.model.expectations.NoMigrationExpectation
import com.github.kerubistan.kerub.model.expectations.NotSameHostExpectation
import com.github.kerubistan.kerub.model.expectations.NotSameStorageExpectation
import com.github.kerubistan.kerub.model.expectations.StorageAvailabilityExpectation
import com.github.kerubistan.kerub.model.expectations.VirtualMachineAvailabilityExpectation
import com.github.kerubistan.kerub.model.hardware.CacheInformation
import com.github.kerubistan.kerub.model.hardware.ChassisInformation
import com.github.kerubistan.kerub.model.hardware.MemoryInformation
import com.github.kerubistan.kerub.model.hardware.ProcessorInformation
import com.github.kerubistan.kerub.model.io.BusType
import com.github.kerubistan.kerub.model.io.VirtualDiskFormat
import com.github.kerubistan.kerub.model.lom.IpmiInfo
import com.github.kerubistan.kerub.model.lom.WakeOnLanInfo
import com.github.kerubistan.kerub.model.messages.EntityUpdateMessage
import com.github.kerubistan.kerub.model.services.IscsiService
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.OperationalStateBuilder
import com.github.kerubistan.kerub.planner.Plan
import com.github.kerubistan.kerub.planner.PlanExecutor
import com.github.kerubistan.kerub.planner.PlanViolationDetectorImpl
import com.github.kerubistan.kerub.planner.Planner
import com.github.kerubistan.kerub.planner.PlannerImpl
import com.github.kerubistan.kerub.planner.steps.host.powerdown.PowerDownHost
import com.github.kerubistan.kerub.planner.steps.host.recycle.RecycleHost
import com.github.kerubistan.kerub.planner.steps.host.startup.AbstractWakeHost
import com.github.kerubistan.kerub.planner.steps.replace
import com.github.kerubistan.kerub.planner.steps.vm.migrate.kvm.KvmMigrateVirtualMachine
import com.github.kerubistan.kerub.planner.steps.vm.start.kvm.KvmStartVirtualMachine
import com.github.kerubistan.kerub.planner.steps.vm.start.virtualbox.VirtualBoxStartVirtualMachine
import com.github.kerubistan.kerub.planner.steps.vstorage.fs.create.CreateImage
import com.github.kerubistan.kerub.planner.steps.vstorage.gvinum.create.CreateGvinumVolume
import com.github.kerubistan.kerub.planner.steps.vstorage.lvm.create.CreateLv
import com.github.kerubistan.kerub.planner.steps.vstorage.share.iscsi.AbstractIscsiShare
import com.github.kerubistan.kerub.stories.config.ControllerConfigDefs
import com.github.kerubistan.kerub.testVm
import com.github.kerubistan.kerub.utils.now
import com.github.kerubistan.kerub.utils.silent
import com.github.kerubistan.kerub.utils.skip
import com.github.kerubistan.kerub.utils.toSize
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import cucumber.api.DataTable
import cucumber.api.PendingException
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import org.junit.Assert
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.ForkJoinPool
import kotlin.test.assertTrue

class PlannerDefs {

	var vms = listOf<VirtualMachine>()
	var hosts = listOf<Host>()
	var vdisks = listOf<VirtualStorageDevice>()

	var vmDyns = listOf<VirtualMachineDynamic>()
	var hostDyns = listOf<HostDynamic>()
	var vstorageDyns = listOf<VirtualStorageDeviceDynamic>()

	var hostConfigs = listOf<HostConfiguration>()
	var controllerConfig = ControllerConfig()

	val backtrack: BacktrackService = BacktrackServiceImpl(ForkJoinPool(1))
	val executor: PlanExecutor = mock()
	val builder: OperationalStateBuilder = mock()

	val planner: Planner = PlannerImpl(
			backtrack,
			executor,
			builder,
			PlanViolationDetectorImpl()
	)

	var executedPlans = listOf<Plan>()

	@Before
	fun setup() {
		whenever(builder.buildState()).then {
			OperationalState.fromLists(
					hosts = hosts,
					hostDyns = hostDyns,
					hostCfgs = hostConfigs,
					vms = vms,
					vmDyns = vmDyns,
					vStorage = vdisks,
					vStorageDyns = vstorageDyns,
					config = controllerConfig
			)
		}
		doAnswer({
			executedPlans += (it.arguments[0] as Plan)
			Unit
		}).whenever(executor).execute(any<Plan>(), any<(Plan) -> Unit>())
	}

	@Given("^VMs:$")
	fun setVms(vmsTable: DataTable) {
		val raw = vmsTable.raw()
		for (row in raw.filter { it != raw.first() }) {
			val vm = VirtualMachine(
					name = row[0],
					memory = Range(
							min = (row[1].toSize()),
							max = (row[2].toSize())
					),
					nrOfCpus = row[3].toInt(),
					expectations = listOf(
							CpuArchitectureExpectation(
									cpuArchitecture = row[4]
							)
					)
			)
			this.vms = this.vms + vm
		}
	}

	@Given("^hosts:$")
	fun hosts(hostsTable: DataTable) {
		for (row in hostsTable.raw().filter { it != hostsTable.raw().first() }) {
			val host = Host(
					address = row[0],
					dedicated = true,
					publicKey = "",
					capabilities = HostCapabilities(
							os = OperatingSystem.valueOf(row[5]),
							cpuArchitecture = row[4],
							cpus = listOf(ProcessorInformation(
									manufacturer = "Test",
									coreCount = row[2].toInt(),
									threadCount = row[3].toInt(),
									flags = listOf(),
									l1cache = null,
									l2cache = null,
									l3cache = null,
									maxSpeedMhz = 3200,
									socket = "",
									version = "",
									voltage = null
							)
							),
							chassis = null,
							distribution = SoftwarePackage(
									name = silent { row[6] } ?: row[5],
									version = silent { Version.fromVersionString(row[7]) }
											?: Version.fromVersionString("1.0")
							),
							devices = listOf(),
							installedSoftware = listOf(),
							system = null,
							totalMemory = row[1].toSize()
					)
			)
			hosts += host
		}
	}

	@Given("host (\\S+) is scheduled for recycling")
	fun setHoostRecycle(address: String) {
		hosts = hosts.replace({ it.address == address }, {
			it.copy(recycling = true)
		})
	}

	@Given("host (\\S+) is (not\\s+)?dedicated")
	fun setDedicated(address: String, yesNo: String?) {
		hosts = hosts.replace({ it.address == address }, {
			it.copy(dedicated = yesNo?.trim() != "not")
		})
	}

	@Given("host (\\S+) filesystem is:")
	fun setHostFilesystemCapabilities(hostAddr: String, mounts: DataTable) {
		val fsCapabilities = mounts.raw().skip().map {
			row ->
			FsStorageCapability(
					size = row[1].toSize(),
					mountPoint = row[0],
					fsType = row[3]
			)
		}
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = requireNotNull(host.capabilities).copy(
							storageCapabilities = requireNotNull(host.capabilities).storageCapabilities + fsCapabilities
					)
			)
		})
	}

	@Given("host (\\S+) gvinum disks are:")
	fun setHostGvinumCapabilities(hostAddr: String, disks: DataTable) {
		val diskCapabilities = disks.raw().skip().map {
			row ->
			GvinumStorageCapability(
					name = row[0],
					device = row[1],
					size = row[2].toSize()
			)
		}
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = host.capabilities!!.copy(
							storageCapabilities = requireNotNull(host.capabilities?.storageCapabilities) + diskCapabilities
					)
			)
		})
	}

	@Given("host (\\S+) volume groups are:")
	fun setHostLvmCapabilities(hostAddr: String, vgs: DataTable) {
		val lvmCapabilities = vgs.raw().skip().map {
			row ->
			LvmStorageCapability(
					id = UUID.randomUUID(),
					physicalVolumes = row[2].split(",").map(String::toSize),
					size = row[1].toSize(),
					volumeGroupName = row[0]
			)
		}
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = requireNotNull(host.capabilities).copy(
							storageCapabilities = requireNotNull(requireNotNull(host.capabilities).storageCapabilities) + lvmCapabilities
					)
			)
		})
	}

	@Given("(\\S+) is running on (\\S+)")
	fun setVmRunningOnHost(vmName: String, hostAddr: String) {
		val vm = vms.first { it.name == vmName }
		vms = vms.replace({ it.id == vm.id }, {
			it.copy(
					expectations = vm.expectations + listOf(
							VirtualMachineAvailabilityExpectation(up = true))
			)
		})
		val host = hosts.first { it.address == hostAddr }

		vmDyns += VirtualMachineDynamic(
				id = vm.id,
				hostId = host.id,
				status = VirtualMachineStatus.Up,
				lastUpdated = now(),
				memoryUsed = vm.memory.min
		)

		requireNotNull(hostDyns.firstOrNull { it.id == host.id }, { "host must be up, otherwise no vm!" })

		hostDyns = hostDyns.replace({ it.id == host.id }, {
			it.copy(
					memFree = requireNotNull(it.memFree ?: host.capabilities?.totalMemory) - vm.memory.min,
					memUsed = (it.memUsed ?: BigInteger.ZERO) + vm.memory.min
			)
		})
	}

	@When("^VM (\\S+) is started$")
	fun startVm(vm: String) {
		vms = vms.map {
			if (it.name == vm) {
				it.copy(expectations = (
						it.expectations
								+ VirtualMachineAvailabilityExpectation(
								level = ExpectationLevel.DealBreaker,
								up = true
						)
						)
				)
			} else {
				it
			}
		}
		planner.onEvent(EntityUpdateMessage(
				obj = vms.filter { it.name == vm }.first(),
				date = now()
		))
	}

	@Then("^host (\\S+) will be recycled$")
	fun verifyHostRecycle(hostAddress: String) {
		Assert.assertTrue(executedPlans.any {
			it.steps.any {
				it is RecycleHost &&
						it.host.address == hostAddress
			}
		})
	}

	@Then("^host (\\S+) will (not\\s+)?be powered down")
	fun verifyHostPowerDown(hostAddress: String, yesNo: String?) {
		val shouldPowerDown = yesNo?.trim() != "not"
		Assert.assertEquals(shouldPowerDown, executedPlans.any {
			it.steps.any {
				it is PowerDownHost &&
						it.host.address == hostAddress
			}
		})
	}

	@Then("^VM (\\S+) gets scheduled on host (\\S+) with kvm hypervisor$")
	fun verifyVmScheduledOnHostWithKvm(vmName: String, hostAddress: String) {
		Assert.assertTrue(executedPlans.any {
			it.steps.any {
				it is KvmStartVirtualMachine
						&& it.host.address == hostAddress
						&& it.vm.name == vmName
			}
		})
	}

	@Then("^VM (\\S+) gets scheduled on host (\\S+) with virtualbox hypervisor$")
	fun verifyVmScheduledOnHostWithVirtualBox(vmName: String, hostAddress: String) {
		Assert.assertTrue(executedPlans.any {
			it.steps.any {
				it is VirtualBoxStartVirtualMachine
						&& it.host.address == hostAddress
						&& it.vm.name == vmName
			}
		})
	}

	@Then("(\\S+) must be shared with iscsi on host (\\S+) as step (\\d+)")
	fun verifyDiskIscsiShare(diskName: String, hostName: String, stepNo: Int) {
		val shareStep = executedPlans.first().steps[stepNo - 1]
		Assert.assertTrue("step $stepNo is $shareStep", shareStep is AbstractIscsiShare)
		Assert.assertEquals((shareStep as AbstractIscsiShare).host.address, hostName)
		Assert.assertEquals(shareStep.vstorage.name, diskName)
	}

	@Then("^(\\S+) will be migrated to (\\S+) as step (\\d+)")
	fun verifyVmMigration(vmName: String, targetHostAddr: String, stepNo: Int) {
		val migrationStep = executedPlans.first().steps[stepNo - 1]
		Assert.assertTrue(migrationStep is KvmMigrateVirtualMachine)
		Assert.assertEquals((migrationStep as KvmMigrateVirtualMachine).target.address, targetHostAddr)
		Assert.assertEquals(migrationStep.vm.name, vmName)
	}

	@Then("^VM (\\S+) gets scheduled on host (\\S+) as step (\\d+)$")
	fun verifyVmGetsScheduled(vmName: String, targetHostAddr: String, stepNo: Int) {
		val startStep = executedPlans.first().steps[stepNo - 1]
		Assert.assertTrue(startStep is KvmStartVirtualMachine)
		Assert.assertEquals((startStep as KvmStartVirtualMachine).host.address, targetHostAddr)
		Assert.assertEquals(startStep.vm.name, vmName)
	}

	@When("^optimization is triggered$")
	fun optimization_is_triggered() {
		throw PendingException()
	}

	@Then("^host (\\S+) should be go to power-save$")
	fun host_should_be_go_to_power_save(host: String) {
		throw PendingException()
	}

	@Given("^status:$")
	fun status(vmstatus: DataTable) {
		throw PendingException()
	}

	@Given("software installed on host (\\S+):")
	fun setHostInstalledSoftware(hostAddr: String, software: DataTable) {
		hosts = hosts.replace(
				{ it.address == hostAddr },
				{
					host ->
					val caps = requireNotNull(host.capabilities)
					host.copy(
							capabilities = caps.copy(
									installedSoftware = software.raw().skip().map {
										SoftwarePackage(name = it[0], version = Version.fromVersionString(it[1]))
									}
							)
					)
				}
		)
	}

	@Given("^host (\\S+) is Up$")
	fun setHostDyn(address: String) {
		val host = hosts.firstOrNull { it.address == address }!!
		hostDyns += HostDynamic(
				id = host.id,
				status = HostStatus.Up,
				memFree = host.capabilities?.totalMemory
		)

	}

	@Given("^host (\\S+) is Down$")
	fun setHostDown(hostAddress: String) {
		val host = hosts.first { host ->
			host.address == hostAddress
		}
		hostDyns = hostDyns.filter {
			dyn ->
			dyn.id != host.id
		}
	}

	@Then("^(\\S+) will be started as step (\\d+)$")
	fun verifyHostStartedUp(hostAddr: String, stepNo: Int) {
		val startStep = executedPlans.first().steps[stepNo - 1]
		Assert.assertTrue(startStep is AbstractWakeHost)
		Assert.assertEquals((startStep as AbstractWakeHost).host.address, hostAddr)
	}

	@Given("^virtual storage devices:$")
	fun setVirtualStorageDevices(vstorage: DataTable) {
		vdisks = vstorage.raw().filterNot { it[0] == "name" }.map {
			VirtualStorageDevice(
					name = it[0],
					size = it[1].toSize(),
					readOnly = it[2].toBoolean()
			)
		}
	}

	@Given("^(\\S+) is attached to (\\S+)$")
	fun attachDiskToVm(storageName: String, vmName: String) {
		val storage = vdisks.first { it.name == storageName }
		vms = vms.replace({ it.name == vmName }, {
			it.copy(
					virtualStorageLinks = it.virtualStorageLinks
							+ VirtualStorageLink(
							virtualStorageId = storage.id,
							bus = BusType.sata
					)
			)
		})
	}

	@Given("^(\\S+) is not yet created$")
	fun removeVStorageDyn(storageName: String) {
		val storage = vdisks.first { it.name == storageName }
		vstorageDyns = vstorageDyns.filterNot { it.id == storage.id }
	}

	@Given("^the virtual disk (\\S+) is created on (\\S+)$")
	fun createVStorageDyn(storageName: String, hostAddr: String) {
		val storage = vdisks.first { it.name == storageName }
		val host = hosts.first { it.address == hostAddr }
		vstorageDyns += VirtualStorageDeviceDynamic(
				id = storage.id,
				allocations = listOf(VirtualStorageFsAllocation(
						hostId = host.id,
						actualSize = storage.size,
						mountPoint = "/var",
						type = VirtualDiskFormat.qcow2,
						fileName = "/var/${storage.id}"
				))
		)
	}

	@Then("^the virtual disk (\\S+) must be allocated on (\\S+) under (\\S+)$")
	fun verifyVirtualStorageCreatedOnFs(storageName: String, hostAddr: String, mountPoint: String) {
		val storage = vdisks.first { it.name == storageName }
		val host = hosts.first { it.address == hostAddr }
		Assert.assertTrue(executedPlans.first().steps.any {
			it is CreateImage &&
					it.disk == storage &&
					it.host == host &&
					it.path == mountPoint
		})
	}

	@Then("the virtual disk (\\S+) must be allocated on (\\S+) under on the volume group (\\S+)")
	fun verifyVirtualStorageCreatedOnLvm(storageName: String, hostAddr: String, volumeGroup: String) {
		Assert.assertTrue(executedPlans.first().steps.any {
			it is CreateLv
					&& it.disk.name == storageName
					&& it.host.address == hostAddr
					&& it.volumeGroupName == volumeGroup
		})
	}

	@Then("the virtual disk (\\S+) must be allocated on (\\S+) under on the gvinum disks: (\\S+)")
	fun checkGvinumConcatenated(storageName: String, hostAddr: String, diskNames: List<String>) {
		assertTrue {
			executedPlans.first().steps.any {
				it is CreateGvinumVolume
						&& it.disk.name == storageName
						&& it.host.address == hostAddr
						&& it.config is ConcatenatedGvinumConfiguration
			}
		}
	}


	@Then("the virtual disk (\\S+) must be allocated on (\\S+) under on the gvinum disk (\\S+)")
	fun verifyVirtualStorageCreatedOnGvinum(storageName: String, hostAddr: String, diskName: String) {

		Assert.assertTrue(executedPlans.first().steps.any {
			it is CreateGvinumVolume
					&& it.disk.name == storageName
					&& it.host.address == hostAddr
			// TODO: verify disk name
		})

	}

	@Given("volume group (\\S+) on host (\\S+) has (\\S+) free capacity")
	fun setVolumeGroupFreeCapacity(volumeGroupName: String, hostAddr: String, capacity: String) {
		val host = hosts.first { it.address == hostAddr }
		val volumeGroup = requireNotNull(
				host
						.capabilities
						?.storageCapabilities
						?.first {
							it is LvmStorageCapability
									&& it.volumeGroupName == volumeGroupName
						}
		) as LvmStorageCapability
		setStorageCapabilityFreeCapacity(capacity, volumeGroup.id, host)
	}

	@Given("gvinum disk (\\S+) on host (\\S+) has (\\S+) free capacity")
	fun setGvinumDiskFreeCapacity(diskName: String, hostAddr: String, capacity: String) {
		val host = hosts.first { it.address == hostAddr }
		val disk = requireNotNull(host.capabilities?.storageCapabilities?.first { it is GvinumStorageCapability && it.name == diskName })
		setStorageCapabilityFreeCapacity(capacity, disk.id, host)
	}

	private fun setStorageCapabilityFreeCapacity(capacity: String, capId: UUID, host: Host) {
		hostDyns = hostDyns.replace({ it.id == host.id }, {
			dyn ->
			dyn.copy(
					storageStatus = dyn.storageStatus + StorageDeviceDynamic(
							id = capId,
							freeCapacity = capacity.toSize()
					)
			)
		})
	}

	@Then("^the virtual disk (\\S+) must not be allocated$")
	fun verifyNoStorageCreate(storageName: String) {
		val storage = vdisks.first { it.name == storageName }
		Assert.assertTrue(executedPlans.first().steps.none { it is CreateImage && it.disk == storage })

	}

	@Given("^no virtual machines$")
	fun clearVirtualMachines() {
		vms = listOf()
		vmDyns = listOf()
	}

	@Given("(\\S+) manufaturer has NO L(\\d) cache")
	fun setNoCache(hostAddress: String, cachelevel: Int) {
		hosts = hosts.replace({ it.address == hostAddress }, {
			host ->
			host.copy(
					capabilities = host.capabilities!!.copy(
							cpus = host.capabilities!!.cpus.replace({ true }, {
								cpu ->
								cpu.copy(
										l1cache = if (cachelevel == 1) null else cpu.l1cache,
										l2cache = if (cachelevel == 2) null else cpu.l2cache
								)
							})
					)
			)
		})
	}

	@Given("(\\S+) manufaturer has (\\S+\\s+\\S+) L(\\d) cache")
	fun setCacheSize(hostAddress: String, amount: String, cachelevel: Int) {
		val size = amount.toSize()
		hosts = hosts.replace({ it.address == hostAddress }, {
			host ->
			host.copy(
					capabilities = host.capabilities!!.copy(
							cpus = host.capabilities!!.cpus.replace({ true }, {
								cpu ->
								val cacheInformation = CacheInformation(
										size = size.toInt(),
										errorCorrection = "",
										operation = "",
										socket = "",
										speedNs = null
								)
								cpu.copy(
										l1cache = if (cachelevel == 1) cacheInformation else cpu.l1cache,
										l2cache = if (cachelevel == 2) cacheInformation else cpu.l2cache
								)
							})
					)
			)
		})
	}

	@Given("^VM (\\S+) requires (\\S+\\s+\\S+) L1 cache$")
	fun setVmCacheRequirement(vmName: String, amount: String) {
		vms = vms.replace({ it.name == vmName }, {
			vm ->
			vm.copy(
					expectations = vm.expectations + CacheSizeExpectation(
							minL1 = amount.toSize().toLong(),
							level = ExpectationLevel.DealBreaker
					)
			)
		})
	}

	@Given("^(\\S+) manufaturer is (\\S+)$")
	fun setHostManufacturer(hostAddr: String, manufacturer: String) {
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = host.capabilities?.copy(
							chassis = host.capabilities?.chassis?.copy(manufacturer = manufacturer) ?:
									ChassisInformation(
											manufacturer = manufacturer,
											type = "BLADE",
											height = null,
											nrOfPowerCords = 1
									)
					)
			)
		})
	}

	@Given("^VM (\\S+) requires manufacturer (\\S+)$")
	fun setVmHostManufacturerInformation(vmName: String, manufacturer: String) {
		vms = vms.replace({ it.name == vmName }, {
			vm ->
			vm.copy(
					expectations = vm.expectations + ChassisManufacturerExpectation(
							manufacturer = manufacturer
					)
			)
		})
	}

	@Given("^(\\S+) has notsame host expectation against (\\S+)$")
	fun addNotSameHostExpectation(vmName: String, otherVmName: String) {
		val otherVm = vms.first { it.name == otherVmName }
		vms = vms.replace({ it.name == vmName }, {
			vm ->
			vm.copy(
					expectations = vm.expectations + NotSameHostExpectation(
							otherVmId = otherVm.id
					)
			)
		})

	}

	@Given("^(\\S+) has cpu clock speed expectation (\\S+) Mhz$")
	fun addCpuClockSpeedExpectation(vmName: String, freq: Int) {
		vms = vms.replace({ it.name == vmName }, {
			vm ->
			vm.copy(
					expectations = vm.expectations + ClockFrequencyExpectation(
							minimalClockFrequency = freq,
							level = ExpectationLevel.Want
					)
			)
		})
	}

	@Given("^(\\S+) cpu clockspeed is (\\S+) Mhz$")
	fun setHostCpuSpeed(hostAddr: String, freq: Int) {
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = host.capabilities?.copy(
							cpus = host.capabilities?.cpus?.map {
								it.copy(
										maxSpeedMhz = freq
								)
							} ?: listOf()
					)
			)
		})
	}

	@Given("^(\\S+) has ECC memory$")
	fun setHostEccMemory(hostAddr: String) {
		throw PendingException()
	}

	@Given("^(\\S+) does not have ECC memory$")
	fun setHostNonEccMemory(hostAddr: String) {
		throw PendingException()
	}

	@Given("(\\S+) has memory clock speed expectation (\\d+) Mhz")
	fun addVmMemoryClockSpeedExpectation(vmName: String, speedMhz: Int) {
		vms = vms.replace({ it.name == vmName }, {
			vm ->
			vm.copy(
					expectations = vm.expectations
							+ MemoryClockFrequencyExpectation(level = ExpectationLevel.DealBreaker, min = speedMhz)
			)
		})
	}

	@Given("(\\S+) memory information is not known")
	fun clearHostMemoryInformation(hostAddr: String) {
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = host.capabilities?.copy(
							memoryDevices = listOf()
					)
			)
		})
	}

	@Given("(\\S+) memory clockspeed is (\\d+) Mhz")
	fun setHostMemoryClockSpeed(hostAddr: String, speedMhz: Int) {
		hosts = hosts.replace({ it.address == hostAddr }, {
			host ->
			host.copy(
					capabilities = host.capabilities!!.copy(
							memoryDevices = listOf(
									host.capabilities!!.memoryDevices.firstOrNull()
											?: MemoryInformation(
											size = "8 GB".toSize(),
											type = "",
											formFactor = "SODIMM",
											locator = "BANK-A",
											speedMhz = speedMhz,
											manufacturer = "DUCT TAPE INC",
											partNumber = "",
											configuredSpeedMhz = speedMhz,
											serialNumber = "",
											bankLocator = ""
									)
							)
					)
			)
		})
	}

	@Given("virtual disk (\\S+) has not-same-storage expectation against (\\S+)")
	fun addNotSameStorageExpectation(diskName: String, againstDiskName: String) {
		val againstDisk = vdisks.first { it.name == againstDiskName }
		vdisks = vdisks.replace({ it.name == diskName }, {
			vdisk ->
			vdisk.copy(
					expectations = vdisk.expectations + NotSameStorageExpectation(
							level = ExpectationLevel.DealBreaker,
							otherDiskId = againstDisk.id
					)
			)
		})
	}

	@Given("(\\S+) requires dedicated cores")
	fun addCoreDedicationExpectation(vmName: String) {
		vms = vms.replace({ it.name == vmName }, {
			it.copy(
					expectations = it.expectations + CoreDedicationExpectation()
			)
		})
	}

	@Given("(\\S+) has no-migrate expectation")
	fun addNoMigrateExpectation(vmName: String) {
		vms = vms.replace({ it.name == vmName }, {
			vm ->
			vm.copy(
					expectations = vm.expectations + NoMigrationExpectation(userTimeoutMinutes = 60)
			)
		})
	}

	@Given("host (\\S+) has (\\S+) power management")
	fun setPowerManagement(hostAddr: String, powerManagementType: String) {
		val host = hosts.first { it.address == hostAddr }
		val pm = when (powerManagementType) {
			"wake-on-lan" -> WakeOnLanInfo()
			"ipmi" -> IpmiInfo(
					address = "",
					password = "",
					username = ""
			)
			else -> throw IllegalArgumentException("typo? " + powerManagementType)
		}
		hosts = hosts.replace({ it.address == host.address }, {
			host.copy(
					capabilities = host.capabilities?.copy(
							powerManagment = listOf(
									pm
							)
					)
			)
		})
	}

	@When("virtual disk (\\S+) gets an availability expectation")
	fun createVirtualStorageAvailabilityExpectation(diskName: String) {

		vdisks = vdisks.replace({ it.name == diskName }, {
			it.copy(
					expectations = it.expectations + StorageAvailabilityExpectation(format = VirtualDiskFormat.raw)
			)
		})
		val virtualStorage = vdisks.first { it.name == diskName }

		planner.onEvent(EntityUpdateMessage(
				obj = virtualStorage,
				date = now()
		))
	}

	@When("planner starts")
	fun kickPlanner() {
		planner.onEvent(EntityUpdateMessage(
				obj = testVm,
				date = now()
		))
	}

	@Given("(\\S+) is allocated on host (\\S+) volume group (\\S+)")
	fun createLvmAllocation(diskName: String, hostName: String, volumeGroup: String) {

		val host = hosts.first { it.address == hostName }
		val disk = vdisks.first { it.name == diskName }
		val diskDyn = vstorageDyns.firstOrNull { it.id == disk.id }
		if (diskDyn == null) {
			vstorageDyns += VirtualStorageDeviceDynamic(
					id = disk.id,
					allocations = listOf(VirtualStorageLvmAllocation(
							hostId = host.id,
							actualSize = disk.size,
							path = "/dev/test/" + disk.id
					)),
					lastUpdated = now()
			)
		}
	}

	@Given("(\\S+) is shared with iscsi on host (\\S+)")
	fun createIscsiShare(diskName: String, hostName: String) {
		val host = hosts.first { it.address == hostName }
		val disk = vdisks.first { it.name == diskName }
		val service = IscsiService(
				vstorageId = disk.id
		)
		if (hostConfigs.any { it.id == host.id }) {
			hostConfigs = hostConfigs.replace({ it.id == host.id }, {
				it.copy(
						services = it.services + service
				)
			})
		} else {
			hostConfigs += HostConfiguration(
					id = host.id,
					services = listOf(service)
			)
		}

	}

	@Given("host (\\S+) CPUs are (\\S+):")
	fun setHostCpus(addr: String, nrOfCpus: Int, cpuData: DataTable) {
		val cpus = (1..nrOfCpus).map {
			ProcessorInformation(
					manufacturer = cpuData.raw()[1][0],
					maxSpeedMhz = cpuData.raw()[1][1].toInt(),
					flags = cpuData.raw()[1][3].split(","),
					coreCount = cpuData.raw()[1].getOrNull(4)?.toInt(),
					threadCount = cpuData.raw()[1].getOrNull(5)?.toInt(),
					socket = "",
					version = ""
			)
		}
		hosts = hosts.replace(
				{ it.address == addr },
				{
					it.copy(
							capabilities = it.capabilities!!.copy(
									cpus = cpus
							)
					)
				}
		)
	}

	@Given("Controller config enabled storage mounts are")
	fun setConfigurationFsType(data : DataTable) {
		this.controllerConfig = this.controllerConfig.copy(
				storageTechnologies = this.controllerConfig.storageTechnologies.copy(
						fsPathEnabled = data.raw().skip().map { it[0] }
				)
		)
	}

	@Given("Controller config filesystem type '(.*)' is (enabled|disabled)")
	fun setConfigurationFsType(fstype: String, enabled: String) {
		this.controllerConfig = this.controllerConfig.copy(
				storageTechnologies = this.controllerConfig.storageTechnologies.copy(
						fsTypeEnabled = if (enabled == "enabled") {
							this.controllerConfig.storageTechnologies.fsTypeEnabled + fstype
						} else {
							this.controllerConfig.storageTechnologies.fsTypeEnabled.filter { it != fstype }
						}
				)
		)
	}

	@Given("Controller configuration '(.*)' is (enabled|disabled)")
	fun setConfigurationBoolean(configName: String, enabled: String) {
		this.controllerConfig = requireNotNull(ControllerConfigDefs.configs[configName])
				.invoke("enabled" == enabled, this.controllerConfig)
	}

}

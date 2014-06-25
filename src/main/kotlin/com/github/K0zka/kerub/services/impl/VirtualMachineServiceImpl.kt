package com.github.K0zka.kerub.services.impl

import com.github.K0zka.kerub.services.VirtualMachineService
import com.github.K0zka.kerub.model.VirtualMachine
import java.util.UUID
import com.github.K0zka.kerub.data.VirtualMachineDao

public class VirtualMachineServiceImpl(dao: VirtualMachineDao) : BaseServiceImpl<VirtualMachine, UUID>(dao),
		VirtualMachineService {
	override fun listAll(): List<VirtualMachine> {
		throw UnsupportedOperationException()
	}
}
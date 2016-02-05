package com.github.K0zka.kerub.utils.junix.storagemanager.lvm

import java.math.BigInteger

data class PhysicalVolume(
		val device: String,
		val size: BigInteger,
		val freeSize: BigInteger,
		val volumeGroupId: String
)
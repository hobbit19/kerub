package com.github.kerubistan.kerub.utils.junix.storagemanager.gvinum

import java.io.Serializable
import java.math.BigInteger

data class GvinumSubDisk(
		val name: String,
		val up: Boolean,
		val drive: String,
		val offset: BigInteger,
		val size: BigInteger,
		val plex: String
) : Serializable
package com.github.kerubistan.kerub.utils.junix.vmstat

import com.github.kerubistan.kerub.utils.KB
import com.github.kerubistan.kerub.utils.toBigInteger
import org.apache.sshd.client.session.ClientSession
import java.math.BigInteger

object BsdVmStat {
	class BsdVmstatOutputStream(val handler: (BsdVmStatEvent) -> Unit) : AbstractVmstatOutputStream() {
		override fun handleInput(split: List<String>) {
			handler(
					BsdVmStatEvent(
							userCpu = split[16].toByte(),
							systemCpu = split[17].toByte(),
							idleCpu = split[18].toByte(),
							cacheMem = BigInteger.ZERO,
							ioBuffMem = BigInteger.ZERO,
							swapMem = BigInteger.ZERO,
							freeMem = split[4].toBigInteger() * KB.toBigInteger()
					)
			)
		}

	}

	fun vmstat(session: ClientSession, handler: (BsdVmStatEvent) -> Unit, interval: Int = 1): Unit {
		commonVmStat(
				session = session,
				interval = interval,
				out = BsdVmstatOutputStream(handler)
		)
	}

}
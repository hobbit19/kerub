package com.github.kerubistan.kerub.utils.junix.sysctl

import com.github.kerubistan.kerub.host.executeOrDie
import org.apache.sshd.client.session.ClientSession

object BsdSysCtl {

	private val splitter = Regex.fromLiteral("\$kern\\.")

	fun getCpuFlags(session: ClientSession): List<String> {

		val output = session.executeOrDie("sysctl -a")
		val props = output.split(splitter)


		return listOf()
	}
}
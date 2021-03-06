package com.github.kerubistan.kerub.utils.junix.mount

import com.github.kerubistan.kerub.host.executeOrDie
import com.github.kerubistan.kerub.model.HostCapabilities
import com.github.kerubistan.kerub.model.OperatingSystem
import com.github.kerubistan.kerub.utils.junix.common.OsCommand
import com.github.kerubistan.kerub.utils.substringBetween
import org.apache.sshd.client.session.ClientSession

object Mount : OsCommand {
	//no known distribution without a mount command
	override fun available(hostCapabilities: HostCapabilities?) =
			hostCapabilities?.os == OperatingSystem.Linux
					|| (
					hostCapabilities?.os == OperatingSystem.BSD
							&& hostCapabilities.distribution?.name == "NetBSD"
					)

	fun listMounts(session: ClientSession): List<FsMount> =
			session.executeOrDie("mount").lines().map {
				FsMount(
						device = it.substringBefore(" on ").trim(),
						mountPoint = it.substringBetween(" on ", " type ").trim(),
						options = it.substringBetween("(", ")").split(','),
						type = it.substringBetween(" type ", "(").trim()
				)
			}

}
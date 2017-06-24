package com.github.K0zka.kerub.host.fw

import com.github.K0zka.kerub.host.FireWall
import com.github.K0zka.kerub.utils.junix.fw.ipfw.Ipfw
import org.apache.sshd.client.session.ClientSession

class IpfwFireWall(private val session: ClientSession) : FireWall {
	override fun open(port: Int, proto: String) {
		Ipfw.open(session, port, proto)
	}

	override fun close(port: Int, proto: String) {
		Ipfw.close(session, port, proto)
	}

}
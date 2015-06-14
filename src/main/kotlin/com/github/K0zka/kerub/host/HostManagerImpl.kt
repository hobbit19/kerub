package com.github.K0zka.kerub.host

import org.apache.sshd.SshClient
import org.slf4j.LoggerFactory
import org.apache.sshd.common.NamedFactory
import org.apache.sshd.common.KeyExchange
import org.apache.sshd.ClientSession
import java.net.SocketAddress
import java.security.PublicKey
import org.apache.sshd.client.ServerKeyVerifier
import org.apache.sshd.server.PublickeyAuthenticator
import org.apache.sshd.server.session.ServerSession
import java.security.KeyPair
import org.apache.sshd.common.SessionListener
import org.apache.sshd.common.Session
import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.utils.getLogger
import com.github.K0zka.kerub.data.HostDao
import com.github.K0zka.kerub.data.dynamic.HostDynamicDao
import java.util.*

public class HostManagerImpl (
		val keyPair : KeyPair,
		val hostDao : HostDao,
		val hostDynamicDao : HostDynamicDao,
		val sshClientService : SshClientService) : HostManager {

	companion object {
		val logger = getLogger(HostManagerImpl::class)
		val defaultSshServerPort = 22
		val defaultSshUserName = "root"
	}

	public var sshServerPort : Int = defaultSshServerPort
	public var sshUserName : String = defaultSshUserName
	private val connections = Collections.synchronizedMap(hashMapOf<UUID, ClientSession>())

	override fun connectHost(host: Host) {
		connections.put(host.id, sshClientService.loginWithPublicKey(host.address))
	}

	fun start() {
		logger.info("starting host manager")
		//TODO: connect each assigned hosts
	}

	fun stop() {
		logger.info("stopping host manager")
		//TODO: disconnect hosts quick but nice
	}

	class ServerKeyReader : ServerKeyVerifier {
		override fun verifyServerKey(sshClientSession: ClientSession?, remoteAddress: SocketAddress?, serverKey: PublicKey?): Boolean {
			logger.debug("server key : {}", serverKey)
			this.serverKey = serverKey
			return true
		}
		companion object val logger = getLogger(ServerKeyReader::class)

		var serverKey : PublicKey? = null
	}

	override fun getHostPublicKey(address: String): PublicKey {
		val pubKeySshClient = SshClient.setUpDefaultClient()!!
		val serverKeyReader = ServerKeyReader()
		pubKeySshClient.setServerKeyVerifier( serverKeyReader )
		pubKeySshClient.start()
		try {
			val connect = pubKeySshClient.connect(sshUserName, address, sshServerPort)!!

			connect.await()
			val session = connect.getSession()!!
			session.auth()!!.await()
			logger.info(session.getServerVersion())
			return serverKeyReader.serverKey!!
		} finally {
			pubKeySshClient.stop()
		}

	}
	override fun registerHost() {
		throw UnsupportedOperationException()
	}
}
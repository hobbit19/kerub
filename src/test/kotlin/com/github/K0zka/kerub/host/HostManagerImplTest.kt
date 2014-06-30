package com.github.K0zka.kerub.host

import org.junit.Test
import java.security.KeyPair
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.Mock
import com.github.K0zka.kerub.data.HostDao
import java.security.PublicKey
import java.security.PrivateKey
import com.github.K0zka.kerub.getTestKey
import com.github.K0zka.kerub.model.Host
import org.junit.Before
import org.junit.After
import org.apache.sshd.SshServer
import org.apache.sshd.server.auth.UserAuthPublicKey
import kotlin.test.assertEquals
import org.apache.sshd.server.Command
import java.io.InputStream
import java.io.OutputStream
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.Environment
import com.github.K0zka.kerub.data.dynamic.HostDynamicDao

RunWith(javaClass<MockitoJUnitRunner>())
public class HostManagerImplTest {

	Mock
	var hostDao: HostDao? = null

	Mock
	var pubKey: PublicKey? = null

	Mock
	var privKey: PrivateKey? = null

	Mock
	var hostDynamicDao : HostDynamicDao? = null

	var hostManager : HostManagerImpl? = null

	var sshServer: SshServer? = null
	var shell : TestShellCommand? = null

	class TestShellCommand : Command {

		var input : InputStream? = null
		var output : OutputStream? = null
		var error : OutputStream? = null
		var env : Environment? = null
		var destroyed : Boolean = false

		override fun setInputStream(`in`: InputStream?) {
			input = `in`
		}

		override fun setOutputStream(out: OutputStream?) {
			output = out
		}

		override fun setErrorStream(err: OutputStream?) {
			error = err
		}

		override fun setExitCallback(callback: ExitCallback?) {
		}

		override fun start(env: Environment?) {
			this.env = env
		}

		override fun destroy() {
			this.destroyed = true
		}

	}

	Before
	fun setup() {
		val key = getTestKey()
		hostManager = HostManagerImpl(getTestKey(), hostDao!!, hostDynamicDao!!)
		hostManager!!.sshServerPort = 2022
		hostManager!!.start()
		shell = TestShellCommand()
		sshServer = SshServer.setUpDefaultServer()
		sshServer!!.setPort(2022)
		sshServer!!.setUserAuthFactories(listOf(UserAuthPublicKey.Factory()))
		sshServer!!.setKeyPairProvider( SingleKeyPairProvider(key) )
		sshServer!!.setShellFactory { shell }
		sshServer!!.start()
	}

	After
	fun cleanup() {
		hostManager!!.stop()
		sshServer!!.stop()
	}

	Test
	fun getHostPubkey() {
		val publicKey = hostManager!!.getHostPublicKey("localhost")
		assertEquals( getTestKey().getPublic(), publicKey )
	}

	Test
	fun connectHost() {
		val host = Host()
		host.address = "127.0.0.1"
		hostManager!!.connectHost(host)
		Thread.sleep(1000)
	}
}
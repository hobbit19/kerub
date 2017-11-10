package com.github.kerubistan.kerub.socket

import com.github.kerubistan.kerub.createClient
import com.github.kerubistan.kerub.createSocketClient
import com.github.kerubistan.kerub.login
import com.github.kerubistan.kerub.logout
import com.github.kerubistan.kerub.model.messages.PingMessage
import com.github.kerubistan.kerub.testWsUrl
import com.github.kerubistan.kerub.utils.createObjectMapper
import com.github.kerubistan.kerub.utils.getLogger
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.UpgradeException
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class WebSocketSecurityIT {

	companion object {
		val logger = getLogger(WebSocketSecurityIT::class)
	}

	var socketClient: WebSocketClient? = null

	@Before
	fun setup() {
		socketClient = WebSocketClient()
		socketClient!!.start()
	}

	@After
	fun cleanup() {
		socketClient?.stop()
	}


	@WebSocket
	class Listener(private val queue: BlockingQueue<String>) {

		@OnWebSocketConnect
		fun connect(session: Session) {
			logger.info("connected: ${session.isOpen}")
			session.remote.sendString(
					createObjectMapper().writeValueAsString(PingMessage(sent = System.currentTimeMillis()))
			)
			queue.put("connected")
		}

		@OnWebSocketClose
		fun close(code: Int, msg: String?) {
			logger.info("connection closed {} {}", code, msg)
			queue.put("closed")
		}

		@OnWebSocketMessage
		fun message(session: Session, input: String) {
			logger.info("message: {}", input)
			queue.put("message")
		}

		@OnWebSocketError
		fun error(error: Throwable) {
			logger.info("socket error", error)
			queue.put("error")
		}
	}

	@Test
	fun authenticatedUser() {
		val client = createClient()
		val rep = client.login("admin", "password")
		val socketClient = createSocketClient(rep)
		val queue: BlockingQueue<String> = ArrayBlockingQueue<String>(1024)

		val session = socketClient.connect(Listener(queue), URI(testWsUrl)).get()

		session.remote.sendString(
				createObjectMapper().writeValueAsString(PingMessage(sent = System.currentTimeMillis()))
		)

		var messages = listOf<String>()
		var msg = queue.poll(1, TimeUnit.SECONDS)
		while (msg != null) {
			messages += msg
			msg = queue.poll(1, TimeUnit.SECONDS)
		}

		assertTrue(messages.first() == "connected")
		assertTrue(messages.contains("message"))

		client.logout()

		msg = queue.poll(1, TimeUnit.SECONDS)
		while (msg != null) {
			messages += msg
			msg = queue.poll(1, TimeUnit.SECONDS)
		}

		assertTrue(messages.last() == "closed")

	}

	@Test
	fun unauthenticatedUser() {

		val queue: BlockingQueue<String> = ArrayBlockingQueue<String>(1024)

		com.github.kerubistan.kerub.expect(ExecutionException::class, {
			socketClient!!.connect(Listener(queue), URI(testWsUrl)).get()
		}, { it.cause is UpgradeException })

	}
}
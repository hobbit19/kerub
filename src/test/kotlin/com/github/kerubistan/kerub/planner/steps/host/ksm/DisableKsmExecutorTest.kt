package com.github.kerubistan.kerub.planner.steps.host.ksm

import com.github.kerubistan.kerub.data.dynamic.HostDynamicDao
import com.github.kerubistan.kerub.host.HostCommandExecutor
import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.dynamic.HostDynamic
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import org.apache.sshd.client.session.ClientSession
import org.junit.Test
import org.mockito.Mockito
import java.util.UUID

class DisableKsmExecutorTest {

	val exec: HostCommandExecutor = mock()
	val hostDynDao: HostDynamicDao = mock()

	val host = Host(
			id = UUID.randomUUID(),
			address = "host-1.example.com",
			dedicated = true,
			publicKey = ""
	)

	@Test
	fun execute() {
		DisableKsmExecutor(exec, hostDynDao).execute(DisableKsm(
				host = host
		))

		Mockito.verify(exec).execute(Mockito.eq(host) ?: host, any<(ClientSession) -> Unit>())
		Mockito.verify(hostDynDao).update(
				eq(host.id),
				any<(UUID) -> HostDynamic>(),
				any<(HostDynamic) -> HostDynamic>()
		)

	}
}
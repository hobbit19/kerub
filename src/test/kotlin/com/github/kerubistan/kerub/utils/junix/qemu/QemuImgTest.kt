package com.github.kerubistan.kerub.utils.junix.qemu

import com.github.kerubistan.kerub.model.SoftwarePackage
import com.github.kerubistan.kerub.model.Version
import com.github.kerubistan.kerub.model.io.VirtualDiskFormat
import com.github.kerubistan.kerub.testHostCapabilities
import com.github.kerubistan.kerub.utils.toSize
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.apache.commons.io.input.NullInputStream
import org.apache.sshd.client.channel.ChannelExec
import org.apache.sshd.client.future.OpenFuture
import org.apache.sshd.client.session.ClientSession
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import kotlin.test.assertTrue

class QemuImgTest {
	val session: ClientSession = mock()
	val execChannel: ChannelExec = mock()
	val channelOpenFuture: OpenFuture = mock()

	@Before
	fun setup() {
		whenever(session.createExecChannel(any<String>())).thenReturn(execChannel)
		whenever(execChannel.open()).thenReturn(channelOpenFuture)
	}

	@Test
	fun available() {
		assertTrue(QemuImg.available(testHostCapabilities.copy(
				distribution = SoftwarePackage("CentOS", Version.fromVersionString("7.0")),
				installedSoftware = listOf(
						SoftwarePackage("qemu-img", version = Version.fromVersionString("2.1.5"))
				)
		)))
		assertTrue(QemuImg.available(testHostCapabilities.copy(
				distribution = SoftwarePackage("Fedora", Version.fromVersionString("25")),
				installedSoftware = listOf(
						SoftwarePackage("qemu-img", version = Version.fromVersionString("2.1.5"))
				)
		)))
		assertTrue(QemuImg.available(testHostCapabilities.copy(
				distribution = SoftwarePackage("Ubuntu", Version.fromVersionString("16.04")),
				installedSoftware = listOf(
						SoftwarePackage("qemu-util", version = Version.fromVersionString("2.1.5"))
				)
		)))
		assertTrue(QemuImg.available(testHostCapabilities.copy(
				distribution = SoftwarePackage("Debian", Version.fromVersionString("16.04")),
				installedSoftware = listOf(
						SoftwarePackage("qemu-util", version = Version.fromVersionString("2.1.5"))
				)
		)))
	}

	@Test
	fun create() {
		whenever(execChannel.invertedOut).thenReturn(NullInputStream(0))
		whenever(execChannel.invertedErr).thenReturn(NullInputStream(0))

		QemuImg.create(session, VirtualDiskFormat.raw, "100 MB".toSize(), "/tmp/test.raw")

		Mockito.verify(session).createExecChannel("qemu-img create -f raw /tmp/test.raw ${"100 MB".toSize()}")
	}

	@Test
	fun info() {
		whenever(session.createExecChannel(any<String>())).thenReturn(execChannel)
		whenever(execChannel.open()).thenReturn(channelOpenFuture)
		val testOutput = """
{
    "virtual-size": 1073741824,
    "filename": "tmp/test.raw",
    "format": "raw",
    "actual-size": 0,
    "dirty-flag": false
}
"""
		whenever(execChannel.invertedOut).thenReturn(ByteArrayInputStream(testOutput.toByteArray(charset("UTF-8"))))
		whenever(execChannel.invertedErr).thenReturn(NullInputStream(0))

		val info = QemuImg.info(session, "/tmp/test.raw")

		Mockito.verify(session).createExecChannel("qemu-img info --output=json /tmp/test.raw")
		Assert.assertThat(info.virtualSize, CoreMatchers.equalTo(1073741824.toLong()))
		Assert.assertThat(info.diskSize, CoreMatchers.equalTo(0.toLong()))
		Assert.assertThat(info.fileName, CoreMatchers.`is`("tmp/test.raw"))
		Assert.assertThat(info.format, CoreMatchers.`is`(VirtualDiskFormat.raw))

	}

	@Test
	fun resize() {
		whenever(execChannel.invertedOut).thenReturn(NullInputStream(0))
		whenever(execChannel.invertedErr).thenReturn(NullInputStream(0))

		QemuImg.resize(session, "/tmp/test.raw", "100 MB".toSize())

		Mockito.verify(session).createExecChannel("qemu-img resize /tmp/test.raw ${"100 MB".toSize()}")
	}

	@Test
	fun convert() {
		whenever(execChannel.invertedOut).thenReturn(NullInputStream(0))
		whenever(execChannel.invertedErr).thenReturn(NullInputStream(0))

		QemuImg.convert(session, "/tmp/test.raw", "/tmp/test.qcow2", VirtualDiskFormat.qcow2)

		Mockito.verify(session).createExecChannel("qemu-img convert -O qcow2 /tmp/test.raw /tmp/test.qcow2")
	}

}
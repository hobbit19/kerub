package com.github.kerubistan.kerub.utils

import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringUtilsTest {

	@Test
	fun toBigInteger() {
		assertEquals(BigInteger.ONE, "1".toBigInteger())
		assertEquals(BigInteger.TEN, "10".toBigInteger())
	}

	@Test
	fun remove() {
		assertEquals("foo bar baz", "foo, bar, baz".remove(",".toRegex()))
		assertEquals("abcdefgh", "abc12def45gh6".remove("\\d+".toRegex()))
		assertEquals("123456", "12   3  45\t6".remove("\\s+".toRegex()))
	}

	@Test
	fun substringsBetween() {
		assertEquals("world", "hello world!".substringBetween("hello ", "!"))
	}

	@Test
	fun substringAfterOrNull() {
		assertEquals("C", "ABC".substringAfterOrNull("B"))
		assertEquals("C", "ABC".substringAfterOrNull("AB"))
		assertEquals("", "ABC".substringAfterOrNull("C"))
		assertNull("ABC".substringAfterOrNull("D"))
	}

	@Test
	fun substringBetweenOrNull() {
		assertEquals("B", "ABC".substringBetweenOrNull("A", "C"))
		assertEquals("BC", "ABCD".substringBetweenOrNull("A", "D"))
		assertEquals("C", "ABCD".substringBetweenOrNull("AB", "D"))
		assertNull("ABC".substringBetweenOrNull("A", "D"))
	}

	@Test
	fun substringBeforeOrNull() {
		assertEquals("A", "ABC".substringBeforeOrNull("B"))
		assertEquals("A", "ABC".substringBeforeOrNull("BC"))
		assertNull("ABC".substringBeforeOrNull("D"))
	}
}
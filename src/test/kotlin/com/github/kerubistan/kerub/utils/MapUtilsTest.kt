package com.github.kerubistan.kerub.utils

import com.github.kerubistan.kerub.expect
import org.junit.Test
import kotlin.test.assertEquals

class MapUtilsTest {

	@Test
	fun want() {
		assertEquals("value", mapOf("key" to "value").want("key"))
		expect(IllegalArgumentException::class) {
			mapOf("key-1" to "value-1").want("key-2")
		}
	}

	@Test
	fun toPairList() {
		assertEquals(listOf(1 to 2), listOf(1 to 2))
	}

	@Test
	fun mapInverse() {
		assertEquals(
				mapOf(1 to "one", 2 to "two", 3 to "three"),
				mapOf("one" to 1, "two" to 2, "three" to 3).inverse()
		)
		assertEquals(
				mapOf<String, String>(),
				mapOf<String, String>().inverse()
		)

	}

	@Test
	fun mapUpdate() {
		assertEquals(
				mapOf(
						1 to "one",
						2 to "two",
						3 to "three"
				),
				mapOf(
						1 to "one",
						2 to "two",
						3 to "drei"
				).update(3, { "three" }, { "" })
		)
		assertEquals(
				mapOf(
						1 to "one",
						2 to "two",
						3 to "three",
						4 to "four"
				),
				mapOf(
						1 to "one",
						2 to "two",
						3 to "three"
				).update(4, { "" }, { "four" })
		)

	}

}
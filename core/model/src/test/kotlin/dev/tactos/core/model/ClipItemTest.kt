package dev.tactos.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ClipItemTest {

    @Test
    fun `content hash is stable for equal text`() {
        assertEquals(contentHashOf("hello tactos"), contentHashOf("hello tactos"))
    }

    @Test
    fun `content hash is the expected sha-256 hex`() {
        // Known vector: sha256("abc")
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            contentHashOf("abc"),
        )
    }

    @Test
    fun `content hash differs for different text, including unicode`() {
        assertNotEquals(contentHashOf("a"), contentHashOf("b"))
        assertNotEquals(contentHashOf("straße"), contentHashOf("strasse"))
        assertNotEquals(contentHashOf(""), contentHashOf(" "))
    }

    @Test
    fun `default constructor computes the hash from text`() {
        val item = ClipItem(text = "192.168.0.1", type = ClipType.IP_ADDRESS, createdAt = 1L)
        assertEquals(contentHashOf("192.168.0.1"), item.contentHash)
    }

    @Test
    fun `updatedAt defaults to createdAt`() {
        val item = ClipItem(text = "x", type = ClipType.TEXT, createdAt = 42L)
        assertEquals(42L, item.updatedAt)
    }
}

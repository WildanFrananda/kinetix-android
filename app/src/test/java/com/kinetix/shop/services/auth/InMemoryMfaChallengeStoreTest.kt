package com.kinetix.shop.services.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryMfaChallengeStoreTest {
    @Test
    fun storesPeeksAndClearsChallengeTokenInMemoryOnly() {
        val store = InMemoryMfaChallengeStore()
        assertNull(store.peek())

        store.store("chal-secret-1")
        assertEquals("chal-secret-1", store.peek())

        store.store("   ")
        assertNull(store.peek())

        store.store("chal-secret-2")
        store.clear()
        assertNull(store.peek())
    }
}

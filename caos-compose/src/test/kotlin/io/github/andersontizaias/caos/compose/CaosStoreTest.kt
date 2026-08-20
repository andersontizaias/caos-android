package io.github.andersontizaias.caos.compose

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaosStoreTest {
    private lateinit var store: CaosStore

    @Before
    fun setUp() {
        store = CaosStore()
    }

    // MARK: - Data registration / synchronous resolution

    @Test
    fun `registers and resolves sync provider`() {
        store.register(key = "username") { "Anderson" }
        assertEquals("Anderson", store.resolve<String>("username"))
    }

    @Test
    fun `resolve for unregistered key returns null`() {
        assertNull(store.resolve<String>("nonexistent"))
    }

    @Test
    fun `resolve with wrong type returns null`() {
        store.register(key = "count") { 42 }
        assertNull(store.resolve<String>("count"))
    }

    @Test
    fun `registering a provider again updates the resolved value`() {
        store.register(key = "balance") { 100.0 }
        val first = store.resolve<Double>("balance")
        store.register(key = "balance") { 200.0 }
        val second = store.resolve<Double>("balance")
        assertEquals(100.0, first)
        assertEquals(200.0, second)
    }

    // MARK: - Data registration / StateFlow

    @Test
    fun `resolve reads the current value of a registered flow`() {
        val flow = MutableStateFlow<Any?>(42)
        store.register(key = "score", flow = flow)
        assertEquals(42, store.resolve<Int>("score"))
    }

    @Test
    fun `flowFor returns the registered flow`() {
        val flow = MutableStateFlow<Any?>(99)
        store.register(key = "score", flow = flow)
        assertEquals(99, store.flowFor<Int>("score")?.value)
    }

    @Test
    fun `flowFor for unregistered key returns null`() {
        assertNull(store.flowFor<String>("ghost"))
    }

    @Test
    fun `provider takes priority over flow for the same key`() {
        val flow = MutableStateFlow<Any?>("from-flow")
        store.register(key = "value", flow = flow)
        store.register(key = "value") { "from-provider" }
        assertEquals("from-provider", store.resolve<String>("value"))
    }

    @Test
    fun `flow updates are reflected on resolve`() {
        val flow = MutableStateFlow<Any?>(1)
        store.register(key = "counter", flow = flow)
        assertEquals(1, store.resolve<Int>("counter"))
        flow.value = 2
        assertEquals(2, store.resolve<Int>("counter"))
    }
}

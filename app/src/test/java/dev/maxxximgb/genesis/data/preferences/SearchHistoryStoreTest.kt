package dev.maxxximgb.genesis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SearchHistoryStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: SearchHistoryStore

    @Before
    fun setUp() {
        val file = File(tempFolder.newFolder(), "search_history.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(produceFile = { file })
        store = SearchHistoryStore(ds)
    }

    @Test
    fun emptyByDefault() = runTest {
        assertTrue(store.observeRecent().first().isEmpty())
    }

    @Test
    fun addPushesToFront() = runTest {
        store.add("foo")
        store.add("bar")
        store.add("baz")
        assertEquals(listOf("baz", "bar", "foo"), store.observeRecent().first())
    }

    @Test
    fun addDeduplicatesCaseInsensitive() = runTest {
        store.add("foo")
        store.add("bar")
        store.add("FOO")
        assertEquals(listOf("FOO", "bar"), store.observeRecent().first())
    }

    @Test
    fun addCapsAt10() = runTest {
        repeat(15) { store.add("query$it") }
        val recent = store.observeRecent().first()
        assertEquals(10, recent.size)
        assertEquals("query14", recent.first())
        assertEquals("query5", recent.last())
    }

    @Test
    fun blankIsIgnored() = runTest {
        store.add("   ")
        store.add("")
        assertTrue(store.observeRecent().first().isEmpty())
    }

    @Test
    fun trimWhitespace() = runTest {
        store.add("  hello  ")
        assertEquals(listOf("hello"), store.observeRecent().first())
    }

    @Test
    fun removeRemovesEntry() = runTest {
        store.add("foo")
        store.add("bar")
        store.remove("foo")
        assertEquals(listOf("bar"), store.observeRecent().first())
    }

    @Test
    fun removeIsCaseInsensitive() = runTest {
        store.add("Foo")
        store.add("bar")
        store.remove("FOO")
        assertEquals(listOf("bar"), store.observeRecent().first())
    }

    @Test
    fun clearRemovesAll() = runTest {
        store.add("foo")
        store.add("bar")
        store.clear()
        assertTrue(store.observeRecent().first().isEmpty())
    }
}

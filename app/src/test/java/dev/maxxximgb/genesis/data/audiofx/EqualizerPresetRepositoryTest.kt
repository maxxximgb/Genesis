package dev.maxxximgb.genesis.data.audiofx

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.maxxximgb.genesis.domain.audiofx.EqualizerPreset
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
class EqualizerPresetRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repo: EqualizerPresetRepository

    @Before
    fun setUp() {
        val ds = PreferenceDataStoreFactory.create(produceFile = {
            File(tempFolder.newFolder(), "equalizer_presets.preferences_pb")
        })
        repo = EqualizerPresetRepository(ds)
    }

    @Test
    fun savedPresetsRoundTripWithSpecialCharsAndEmoji() = runTest {
        val a = preset("a", "Bass+ 🎵", listOf(300, 200, 0, -100, 500))
        val b = preset("b", "Vocal", listOf(0, -100, 200, 300, 400, 500))

        repo.savePreset(a)
        repo.savePreset(b)

        // observePresets always prepends a synthesized default preset; filter to user records.
        val loaded = repo.observePresets().first().filterNot { it.isDefault }
        assertEquals(listOf(a, b), loaded)
    }

    @Test
    fun renamePreservesIdAndOtherFields() = runTest {
        val original = preset("x", "Old", listOf(1, 2, 3))
        repo.savePreset(original)
        repo.renamePreset("x", "New")

        val loaded = repo.observePresets().first().single { it.id == "x" }
        assertEquals("x", loaded.id)
        assertEquals("New", loaded.name)
        assertEquals(original.bandLevelsMillibels, loaded.bandLevelsMillibels)
    }

    @Test
    fun deletePresetRemovesById() = runTest {
        repo.savePreset(preset("a", "A", listOf(0)))
        repo.savePreset(preset("b", "B", listOf(0)))

        repo.deletePreset("a")

        val loaded = repo.observePresets().first().filterNot { it.isDefault }
        assertEquals(listOf("b"), loaded.map { it.id })
    }

    @Test
    fun deleteDefaultPresetIsNoOp() = runTest {
        repo.savePreset(preset(EqualizerPreset.DEFAULT_ID, "", listOf(100, 200, 300)))
        repo.deletePreset(EqualizerPreset.DEFAULT_ID)

        // Default preset is permanent — deletion request is ignored, edits stay.
        val loaded = repo.observePresets().first().single { it.isDefault }
        assertEquals(listOf(100, 200, 300), loaded.bandLevelsMillibels)
    }

    @Test
    fun nameTruncatedToMaxLength() = runTest {
        val longName = "x".repeat(100)
        repo.savePreset(preset("a", longName, listOf(0)))

        val loaded = repo.observePresets().first().single { it.id == "a" }
        assertEquals(EqualizerPreset.MAX_NAME_LENGTH, loaded.name.length)
    }

    @Test
    fun loadFromFileMergesWithDeduplication() = runTest {
        repo.savePreset(preset("existing-id", "MyMix", listOf(0, 0, 0)))

        val incoming = listOf(
            preset("new-id-1", "MyMix", listOf(100, 200, 300)),
            preset("new-id-2", "Fresh", listOf(0, 0, 0)),
            preset("existing-id", "Skipped", listOf(999, 999, 999)),
        )
        val tempFile = File(tempFolder.newFolder(), "import.json").apply {
            writeText(EqualizerPresetRepository.serializePresets(incoming))
        }

        val result = repo.loadFromFile(tempFile)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
        val loaded = repo.observePresets().first().filterNot { it.isDefault }
        val names = loaded.map { it.name }.sorted()
        assertEquals(listOf("Fresh", "MyMix", "MyMix (2)"), names)
    }

    @Test
    fun deserializingMalformedJsonReturnsEmpty() {
        val parsed = EqualizerPresetRepository.deserializePresets("not really json")
        assertEquals(emptyList<EqualizerPreset>(), parsed)
    }

    @Test
    fun deserializingNullOrEmptyReturnsEmpty() {
        assertEquals(emptyList<EqualizerPreset>(), EqualizerPresetRepository.deserializePresets(null))
        assertEquals(emptyList<EqualizerPreset>(), EqualizerPresetRepository.deserializePresets(""))
    }

    @Test
    fun deserializingLegacyJsonWithBassVirtSpatialKeysIsTolerated() {
        // Older saves carried bass/virt/spatial keys. After their feature removal we silently
        // drop them — the rest of the record still loads.
        val legacyJson = """[{"id":"x","name":"OldMix","bands":[100,200],"bass":500,"virt":300,"spatial":true}]"""
        val parsed = EqualizerPresetRepository.deserializePresets(legacyJson).single()
        assertEquals("x", parsed.id)
        assertEquals("OldMix", parsed.name)
        assertEquals(listOf(100, 200), parsed.bandLevelsMillibels)
    }

    private fun preset(id: String, name: String, bands: List<Int>) = EqualizerPreset(
        id = id,
        name = name,
        bandLevelsMillibels = bands,
    )
}

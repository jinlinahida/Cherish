package com.cherish.app.settings.repository

import com.cherish.app.home.model.HomeViewMode
import com.cherish.app.settings.model.AppContentScale
import com.cherish.app.settings.model.AppSettings
import com.cherish.app.settings.storage.SettingsStorage
import com.cherish.app.settings.storage.SettingsStorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SettingsRepositoryTest {

    private class FakeSettingsStorage(
        var initialSettings: AppSettings = AppSettings(),
        var failOnSave: Boolean = false,
    ) : SettingsStorage {
        var current: AppSettings = initialSettings
        var saveCallCount = 0

        override fun load(): AppSettings = current

        override fun save(settings: AppSettings) {
            if (failOnSave) {
                throw SettingsStorageException.StorageWriteException("Simulated storage write error")
            }
            saveCallCount++
            current = settings
        }
    }

    @Test
    fun `initializes with loaded settings from storage`() {
        val initial = AppSettings(
            homeViewMode = HomeViewMode.GRID,
            hapticsEnabled = false,
            contentScale = AppContentScale.LARGE,
        )
        val storage = FakeSettingsStorage(initial)
        val repo = DefaultSettingsRepository(storage)

        assertEquals(initial, repo.getSettings())
        assertEquals(initial, repo.settings.value)
    }

    @Test
    fun `update modifies settings and persists to storage`() {
        val storage = FakeSettingsStorage()
        val repo = DefaultSettingsRepository(storage)

        repo.update { it.copy(homeViewMode = HomeViewMode.GRID, hapticsEnabled = false) }

        assertEquals(HomeViewMode.GRID, repo.getSettings().homeViewMode)
        assertFalse(repo.getSettings().hapticsEnabled)
        assertEquals(1, storage.saveCallCount)
        assertEquals(HomeViewMode.GRID, storage.current.homeViewMode)
    }

    @Test
    fun `update failure preserves in-memory state without corruption`() {
        val storage = FakeSettingsStorage(failOnSave = true)
        val repo = DefaultSettingsRepository(storage)

        val before = repo.getSettings()

        try {
            repo.update { it.copy(homeViewMode = HomeViewMode.GRID) }
            fail("Expected exception")
        } catch (e: SettingsStorageException.StorageWriteException) {
            // Expected
        }

        assertEquals(before, repo.getSettings())
        assertEquals(before, repo.settings.value)
    }

    @Test
    fun `reload re-reads settings from storage`() {
        val storage = FakeSettingsStorage()
        val repo = DefaultSettingsRepository(storage)

        assertEquals(HomeViewMode.LIST, repo.getSettings().homeViewMode)

        storage.current = AppSettings(homeViewMode = HomeViewMode.GRID)
        repo.reload()

        assertEquals(HomeViewMode.GRID, repo.getSettings().homeViewMode)
    }
}

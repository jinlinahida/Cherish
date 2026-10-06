package com.cherish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cherish.app.event.repository.DefaultEventRepository
import com.cherish.app.home.HomeScreen
import com.cherish.app.home.HomeViewModel
import com.cherish.app.settings.repository.DefaultSettingsRepository
import com.cherish.app.settings.storage.AtomicFileSettingsStorage
import com.cherish.app.settings.ui.toShirokoWear
import com.cherish.app.storage.AtomicFileEventStorage
import com.cherish.app.storage.FileEventImageStorage
import com.cherish.app.ui.rememberScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storageFile = File(filesDir, "events.json")
        val storage = AtomicFileEventStorage(storageFile)
        val repository = DefaultEventRepository(storage)

        val settingsFile = File(filesDir, "settings.json")
        val settingsStorage = com.cherish.app.settings.storage.AtomicFileSettingsStorage(settingsFile)
        val settingsRepository = com.cherish.app.settings.repository.DefaultSettingsRepository(settingsStorage)

        val imageStorageDir = File(filesDir, "backgrounds")
        val imageStorage = FileEventImageStorage(imageStorageDir)

        viewModel = HomeViewModel(
            repository = repository,
            settingsRepository = settingsRepository,
        )

        setContent {
            val settings by settingsRepository.settings.collectAsState()
            val screenShape = rememberScreenShape()
            ShirokoWearTheme(
                contentScale = settings.contentScale.toShirokoWear(),
                screenShape = screenShape,
                hapticFeedbackEnabled = settings.hapticsEnabled,
            ) {
                CherishApp(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    homeViewModel = viewModel,
                    imageStorage = imageStorage,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshToday()
        }
    }
}

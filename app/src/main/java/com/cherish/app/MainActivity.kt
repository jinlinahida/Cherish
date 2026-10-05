package com.cherish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cherish.app.event.repository.DefaultEventRepository
import com.cherish.app.home.HomeScreen
import com.cherish.app.home.HomeViewModel
import com.cherish.app.storage.AtomicFileEventStorage
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

        viewModel = HomeViewModel(
            repository = repository,
            settingsRepository = settingsRepository,
        )

        setContent {
            ShirokoWearTheme {
                CherishApp(
                    repository = repository,
                    homeViewModel = viewModel,
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

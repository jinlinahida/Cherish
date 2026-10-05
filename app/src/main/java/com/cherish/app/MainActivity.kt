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
        viewModel = HomeViewModel(repository = repository)

        setContent {
            ShirokoWearTheme {
                HomeScreen(viewModel = viewModel)
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

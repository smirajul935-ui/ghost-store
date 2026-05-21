package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.download.ApkDownloader
import com.example.ui.GhostStoreApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup full-bleed immersive edge to edge behavior
        enableEdgeToEdge()
        
        // Initialize the download engine
        val downloader = ApkDownloader(this)

        setContent {
            MyApplicationTheme {
                // Obtain ViewModel through Jetpack Compose state holders
                val storeViewModel: StoreViewModel = viewModel()
                
                // Mount the whole application composition
                GhostStoreApp(viewModel = storeViewModel, downloader = downloader)
            }
        }
    }
}

package com.example.mtgstatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.mtgstatapp.composables.LoginScreen
import com.example.mtgstatapp.composables.MainContentScreen
import com.example.mtgstatapp.ui.theme.MtgstatappTheme
import com.example.mtgstatapp.viewmodel.ViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
         val viewModel : ViewModel by viewModels()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MtgstatappTheme {
                //Currently only calling LoginScreen to demonstrate gRPC calls
                LoginScreen(viewModel)
                }
            }
        }
    }

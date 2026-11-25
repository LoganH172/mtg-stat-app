package com.example.mtgstatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mtgstatapp.composables.LoginScreen
import com.example.mtgstatapp.composables.MainContentScreen
import com.example.mtgstatapp.composables.SCRYFALL_TEST_SCREEN
import com.example.mtgstatapp.composables.TEST_SCREEN
import com.example.mtgstatapp.ui.theme.MtgstatappTheme
import com.example.mtgstatapp.viewmodel.ViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
         val viewModel : ViewModel by viewModels()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MtgstatappTheme {
                val navHostController : NavHostController = rememberNavController()

                //Navigate to the correct screen
                NavHost(
                    navController = navHostController,
                    startDestination = NavigationRoutes.LOGIN.name,
                    builder = {
                        composable(NavigationRoutes.LOGIN.name) {
                            LoginScreen(viewModel, navHostController)
                        }
                        composable(NavigationRoutes.MAIN.name){
                            MainContentScreen(viewModel, navHostController)
                        }
                        composable (NavigationRoutes.TEST.name ){
                            TEST_SCREEN(viewModel, navHostController)
                        }
                        composable (NavigationRoutes.SCRYFALL.name ){
                            SCRYFALL_TEST_SCREEN(viewModel, navHostController)
                        }

                    }
                )
                }
            }
        }
    }

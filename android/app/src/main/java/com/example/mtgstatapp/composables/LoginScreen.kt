package com.example.mtgstatapp.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mtgstatapp.viewmodel.ViewModel

/*Eventually will be parsed for the user's information. Currently just used to demonstrate gRPC connection.*/
@Composable
fun LoginScreen(viewModel: ViewModel) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    )
    {
        Button(onClick = {viewModel.greet("Android")}) { }
    }


}
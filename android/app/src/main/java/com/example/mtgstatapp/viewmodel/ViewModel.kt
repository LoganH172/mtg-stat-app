package com.example.mtgstatapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mtgstatapp.client.ClientService
import io.grpc.StatusException
import kotlinx.coroutines.launch

class ViewModel(application: Application): AndroidViewModel(application) {
    /*Had to do some debugging and figuring, but my own local network somehow claimed the 10.0.0.0/24 host
    * which caused some issues. Feel free to change to 10.0.2.2. Alternatively you can port forward to fix
    * some issues and change host to localhost. I included some commands to help port forward in the updated README.
    * Otherwise this is the layout fot the viewModel, which will handle data from gRPC calls and pass into UI elements
    * for updating. */
    private val client = ClientService(application, "10.0.0.193", 50051);

    fun greet(name: String) {
        viewModelScope.launch {
            try {
                val reply = client.sayHello(name)
                Log.d("CommanderGRPC", "Server response: $reply")
            } catch (e: StatusException) {
                Log.e("CommanderGRPC", "RPC failed: ${e.status}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}

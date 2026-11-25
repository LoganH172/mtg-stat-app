package com.example.mtgstatapp.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mtgstatapp.viewmodel.ViewModel

/*Serves to allow for simple searching and getting cards from Scryfall*/

@Composable
fun SCRYFALL_TEST_SCREEN(viewModel: ViewModel, navHostController: NavHostController){
    val cardSearchResults by viewModel.cardSearchReturn.collectAsState()

    //Simple textfield and checkbox to set query and isCommander field
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        TextField(
            value = viewModel.cardSearchQuery,
            onValueChange = {viewModel.cardSearchQuery = it}
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center ) {
            Text("Check to search only for commanders")
            Checkbox(
                checked = viewModel.isCommander,
                onCheckedChange = {viewModel.isCommander = !viewModel.isCommander}
            )
        }

        //Search call button
        Button(onClick =
            {
            if (viewModel.cardSearchQuery.isNotEmpty())
                viewModel.cardSearch(viewModel.cardSearchQuery, viewModel.isCommander, 10)
            }
        ) { Text("Search")}

        //Displays cards from search in lazycolumn. Since flow, should populate as cards come in.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            items(
                items = cardSearchResults,
                key = {card -> card.scryfallId}
            ) { card ->
                Text("Name: ${card.name} ")
                }
            }
        }

    }
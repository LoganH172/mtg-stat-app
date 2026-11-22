package com.example.mtgstatapp.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mtgstatapp.NavBarScreens
import com.example.mtgstatapp.viewmodel.ViewModel

/*Simple UI that swaps between screens for views.*/
@Composable
fun MainContentScreen(viewModel: ViewModel, navHostController: NavHostController)
{
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar{
                NavBarScreens.entries.forEachIndexed { index, screens ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {selectedIndex = index},
                        icon = {
                            Icon(
                                modifier = Modifier.size(36.dp),
                                painter = painterResource(screens.icon),
                                contentDescription = screens.contentDescription)
                        },
                        //Label may be removed once Icons are more set to save space
                        label = { Text(screens.label, fontSize = 12.sp)}

                    )
                }
            }
        }
    ){innerPadding -> Content(modifier = Modifier.padding(innerPadding), selectedIndex)}

}

@Composable
fun Content(modifier: Modifier = Modifier, selectedIndex : Int){
    when(selectedIndex){
        0->HomeScreen()
        1->MyCommanderScreen()
        2->GameScreen()
        3->PlaygroupScreen()
    }
}

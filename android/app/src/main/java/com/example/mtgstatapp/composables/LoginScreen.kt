package com.example.mtgstatapp.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mtgstatapp.NavigationRoutes
import com.example.mtgstatapp.R
import com.example.mtgstatapp.viewmodel.ViewModel

/*Eventually will be parsed for the user's information. Currently just used to demonstrate gRPC connection.*/
@Composable
fun LoginScreen(viewModel: ViewModel, navHostController: NavHostController) {


        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                label = {
                    Text("Username")
                })

            Spacer(modifier = Modifier.padding(8.dp))

            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                visualTransformation =
                    if (viewModel.passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.passwordVisible = !viewModel.passwordVisible
                    }) {
                        val icon: Painter
                        if (viewModel.passwordVisible)
                            icon = painterResource(R.drawable.rounded_visibility_24)
                        else
                            icon = painterResource(R.drawable.rounded_visibility_off_24)
                        Icon(
                            painter = icon,
                            contentDescription = "Change Password Visibility"
                        )
                    }
                },
                label = {
                    Text("Password")
                }
            )

            //UPDATED TO MOVE TO TEST SCREEN
            Button(onClick = {
                if (viewModel.loginScreenState.equals("Login"))
                {
                    viewModel.playerLogin(viewModel.username, viewModel.password)
                    viewModel.username = ""
                    viewModel.password = ""
                    navHostController.navigate(NavigationRoutes.TEST.name)
                }
                else
                {
                    viewModel.playerRegister(viewModel.username, viewModel.password)
                    viewModel.username = ""
                    viewModel.password = ""
                    navHostController.navigate(NavigationRoutes.TEST.name)
                }
                })
            {
                Text(viewModel.loginScreenState)
            }

            Spacer(modifier = Modifier.padding(20.dp))

            if (viewModel.newUserRegister) {
                Text(
                    text = "New User? Click to Register",
                    modifier = Modifier
                        .clickable(
                            enabled = viewModel.newUserRegister,
                            onClick = {
                                viewModel.newUserRegister = false
                                viewModel.loginScreenState = "Register"
                            })
                )
            }
        }
    }


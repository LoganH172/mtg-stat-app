package com.example.mtgstatapp.composables


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mtgstatapp.R

@Preview
@Composable
fun HomeScreen(){
    Box(
        modifier = Modifier
            .background(Color(0xFF1C4DA4))
            .fillMaxSize()
    )
    {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.size(0.dp, 50.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Image(
                    painter = painterResource(R.drawable.test_image),
                    contentDescription = "LAST PLAYED COMMANDER"
                )
                Canvas(
                    modifier = Modifier.size(200.dp)
                )
                {
                    inset(30F, { drawCircle(color = Color(0xFFFFFFFF)) })

                }
            }
            Spacer(modifier = Modifier.size(0.dp, 100.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                Canvas(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                {
                    inset(
                        30F,
                        {
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                size = Size(width = size.width, height = size.height / 3F)
                            )
                        })
                    inset(
                        30F,
                        {
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                size = Size(width = size.width, height = size.height / 3F),
                                topLeft = Offset(0F, 250F)
                            )
                        })
                    inset(
                        30F,
                        {
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                size = Size(width = size.width, height = size.height / 3F),
                                topLeft = Offset(0F, 500F)
                            )
                        })

                }
            }
        }
    }
}
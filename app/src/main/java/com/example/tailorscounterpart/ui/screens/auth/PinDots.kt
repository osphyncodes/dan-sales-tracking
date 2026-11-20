package com.example.tailorscounterpart.ui.screens.auth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PinDots(count: Int) {

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

        repeat(4) { i ->

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (i < count) Color.Black else Color.Transparent,
                        CircleShape
                    )
                    .border(1.dp, Color.Gray, CircleShape)
            )
        }
    }
}
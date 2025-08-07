package com.example.cyclink.misc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.sign_in.LoginActivity
import com.example.cyclink.R

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WelcomeScreen()
        }
    }
}

@Composable
fun WelcomeScreen() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.honeydew)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.cyclink_logo),
                contentDescription = "Cyclink Logo",
                modifier = Modifier.size(150.dp)
            )

            Text(
                text = "Welcome to Cyclink",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5A1807)
            )

            Text(
                text = "Your companion in cycling — we look after you on every ride.",
                fontSize = 16.sp,
                color = Color(0xFF55251D),
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 22.sp
            )

            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.red_pantone)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}
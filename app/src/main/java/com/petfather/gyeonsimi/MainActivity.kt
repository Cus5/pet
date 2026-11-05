package com.petfather.gyeonsimi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.petfather.gyeonsimi.ui.GyeonsimiApp
import com.petfather.gyeonsimi.ui.theme.GyeonsimiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GyeonsimiTheme {
                GyeonsimiApp()
            }
        }
    }
}

package com.petfather.gyeonsimi2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.petfather.gyeonsimi2.data.remote.UsageEventName
import com.petfather.gyeonsimi2.ui.GyeonsimiApp
import com.petfather.gyeonsimi2.ui.theme.GyeonsimiTheme

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

    override fun onStart() {
        super.onStart()
        // 하루에 한 번만 기록되고, 사용자가 익명 사용 통계를 켠 경우에만 전송됩니다.
        (application as GyeonsimiApplication).container.telemetry.track(UsageEventName.APP_OPEN)
    }
}

package com.skybots.kiko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.skybots.kiko.ui.KikoHomeScreen
import com.skybots.kiko.ui.theme.KikoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KikoTheme {
                KikoHomeScreen()
            }
        }
    }
}

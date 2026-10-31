package org.lunaria.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import org.lunaria.app.ui.LunariaTheme
import org.lunaria.app.ui.RootScreen

class MainActivity : ComponentActivity() {
    private val vm: CycleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunariaTheme {
                CompositionLocalProvider(LocalCycle provides vm.state) {
                    RootScreen()
                }
            }
        }
    }
}
